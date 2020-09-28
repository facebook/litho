/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.graphics.Rect;
import android.util.LongSparseArray;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.RenderCoreExtensionHost;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionInput;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Extension for performing incremental mount. */
public class IncrementalMountExtension extends MountExtension<IncrementalMountExtensionInput> {

  private final RenderCoreExtensionHost mHost;
  private final Rect mPreviousLocalVisibleRect = new Rect();
  private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();
  private final IncrementalMountBinder mAttachDetachBinder = new IncrementalMountBinder();
  private final LongSparseArray<RenderTreeNode> mPendingImmediateRemoval = new LongSparseArray<>();
  private final boolean mAcquireReferencesDuringMount;

  private IncrementalMountExtensionInput mInput;
  private int mPreviousTopsIndex;
  private int mPreviousBottomsIndex;

  public IncrementalMountExtension(final RenderCoreExtensionHost host) {
    this(host, false);
  }

  public IncrementalMountExtension(
      final RenderCoreExtensionHost host, final boolean acquireReferencesDuringMount) {
    mHost = host;
    mAcquireReferencesDuringMount = acquireReferencesDuringMount;
  }

  @Override
  public void beforeMount(IncrementalMountExtensionInput input, Rect localVisibleRect) {
    releaseAcquiredReferencesForRemovedItems(input);
    mInput = input;
    mPreviousLocalVisibleRect.setEmpty();

    if (!mAcquireReferencesDuringMount) {
      initIncrementalMount(localVisibleRect, false);
    }

    setVisibleRect(localVisibleRect);
  }

  @Override
  public void beforeMountItem(RenderTreeNode renderTreeNode, int index) {
    if (!mAcquireReferencesDuringMount) {
      return;
    }
    maybeAcquireReference(mPreviousLocalVisibleRect, renderTreeNode, index, false);
  }

  @Override
  public void afterMount() {
    if (mAcquireReferencesDuringMount) {
      setupPreviousMountableOutputData(mPreviousLocalVisibleRect);
    }

    // remove everything that was marked as needing to be removed.
    // At this point we know that all items have been moved to the appropriate hosts.
    for (int i = 0, size = mPendingImmediateRemoval.size(); i < size; i++) {
      final long position = mPendingImmediateRemoval.keyAt(i);
      final RenderTreeNode node = mPendingImmediateRemoval.get(position);
      releaseMountReference(node, (int) position, true);
    }
    mPendingImmediateRemoval.clear();
  }

  @Override
  public void onUnmount() {
    resetAcquiredReferences();
    mPreviousLocalVisibleRect.setEmpty();
  }

  /**
   * Called when LithoView visible bounds change to perform incremental mount. This is always called
   * on a non-dirty mount with a non-null localVisibleRect.
   *
   * @param localVisibleRect
   */
  @Override
  public void onVisibleBoundsChanged(Rect localVisibleRect) {
    assertMainThread();

    // Horizontally scrolling or no visible rect. Can't incrementally mount.
    if (mPreviousLocalVisibleRect.isEmpty()
        || localVisibleRect.isEmpty()
        || localVisibleRect.left != mPreviousLocalVisibleRect.left
        || localVisibleRect.right != mPreviousLocalVisibleRect.right) {
      initIncrementalMount(localVisibleRect, true);
    } else {
      performIncrementalMount(localVisibleRect);
    }

    setVisibleRect(localVisibleRect);

    LithoStats.incrementComponentMountCount();
  }

  @Override
  public void onUnbind() {}

  @Override
  protected void acquireMountReference(
      final RenderTreeNode renderTreeNode,
      final int position,
      final MountDelegate.MountDelegateInput input,
      final boolean isMounting) {
    // Make sure the host is mounted before the child.
    final RenderTreeNode hostTreeNode = renderTreeNode.getParent();
    if (hostTreeNode != null) {
      final long hostId = hostTreeNode.getRenderUnit().getId();
      if (!ownsReference(hostId)) {
        final int hostIndex = mInput.getLayoutOutputPositionForId(hostId);
        acquireMountReference(
            hostTreeNode, hostIndex, input, isMounting || mAcquireReferencesDuringMount);
      }
    }

    super.acquireMountReference(renderTreeNode, position, input, isMounting);
  }

  @Override
  public boolean canPreventMount() {
    return true;
  }

  public RenderUnit.Binder getAttachDetachBinder() {
    return mAttachDetachBinder;
  }

  private void releaseAcquiredReferencesForRemovedItems(IncrementalMountExtensionInput input) {
    if (mInput == null) {
      return;
    }

    for (int i = 0, size = mInput.getMountableOutputCount(); i < size; i++) {
      final RenderTreeNode node = mInput.getMountableOutputAt(i);
      final long id = node.getRenderUnit().getId();
      if (input.getLayoutOutputPositionForId(id) < 0 && ownsReference(id)) {
        releaseMountReference(node, i, false);
      }
    }
  }

  private void initIncrementalMount(Rect localVisibleRect, boolean isMounting) {
    for (int i = 0, size = mInput.getMountableOutputCount(); i < size; i++) {
      final RenderTreeNode renderTreeNode = mInput.getMountableOutputAt(i);
      maybeAcquireReference(localVisibleRect, renderTreeNode, i, isMounting);
    }

    setupPreviousMountableOutputData(localVisibleRect);
  }

  private void maybeAcquireReference(
      Rect localVisibleRect, RenderTreeNode renderTreeNode, int position, boolean isMounting) {
    final LayoutOutput layoutOutput = getLayoutOutput(renderTreeNode);
    final Component component = layoutOutput.getComponent();
    final Object content = getContentAt(position);

    // By default, a LayoutOutput passed in to mount will be mountable. Incremental mount can
    // override that if the item is outside the visible bounds.
    // TODO (T64830748): extract animations logic out of this.
    final boolean isMountable =
        isMountedHostWithChildContent(content)
            || Rect.intersects(localVisibleRect, getLayoutOutput(renderTreeNode).getBounds())
            || isRootItem(position);
    final boolean hasAcquiredMountRef = ownsReference(renderTreeNode);
    if (isMountable && !hasAcquiredMountRef) {
      acquireMountReference(renderTreeNode, position, mInput, isMounting);
    } else if (!isMountable && hasAcquiredMountRef) {
      if (!isMounting) {
        mPendingImmediateRemoval.put(position, renderTreeNode);
      } else {
        releaseMountReference(renderTreeNode, position, true);
      }
    } else if (isMountable && hasAcquiredMountRef && isMounting) {
      // If we're in the process of mounting now, we know the item we're updating is already
      // mounted and that MountState.mount will not be called. We have to call the binder
      // ourselves.
      onItemUpdated(content, component);
    }
  }

  private void setVisibleRect(@Nullable Rect localVisibleRect) {
    if (localVisibleRect != null) {
      mPreviousLocalVisibleRect.set(localVisibleRect);
    }
  }

  /**
   * @return true if this method did all the work that was necessary and there is no other content
   *     that needs mounting/unmounting in this mount step. If false then a full mount step should
   *     take place.
   */
  private boolean performIncrementalMount(Rect localVisibleRect) {
    final List<RenderTreeNode> layoutOutputTops = mInput.getMountableOutputTops();
    final List<RenderTreeNode> layoutOutputBottoms = mInput.getMountableOutputBottoms();
    final int count = mInput.getMountableOutputCount();

    if (localVisibleRect.top > 0 || mPreviousLocalVisibleRect.top > 0) {
      // View is going on/off the top of the screen. Check the bottoms to see if there is anything
      // that has moved on/off the top of the screen.
      while (mPreviousBottomsIndex < count
          && localVisibleRect.top
              >= getLayoutOutput(layoutOutputBottoms.get(mPreviousBottomsIndex))
                  .getBounds()
                  .bottom) {
        final RenderTreeNode node = layoutOutputBottoms.get(mPreviousBottomsIndex);
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        final long id = layoutOutput.getId();
        final int layoutOutputIndex = mInput.getLayoutOutputPositionForId(id);
        if (ownsReference(node)) {
          releaseMountReference(node, layoutOutputIndex, true);
        }
        mPreviousBottomsIndex++;
      }

      while (mPreviousBottomsIndex > 0
          && localVisibleRect.top
              < getLayoutOutput(layoutOutputBottoms.get(mPreviousBottomsIndex - 1))
                  .getBounds()
                  .bottom) {
        mPreviousBottomsIndex--;
        final RenderTreeNode node = layoutOutputBottoms.get(mPreviousBottomsIndex);
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        if (!ownsReference(node)) {
          acquireMountReference(
              node, mInput.getLayoutOutputPositionForId(layoutOutput.getId()), mInput, true);
          mComponentIdsMountedInThisFrame.add(layoutOutput.getId());
        }
      }
    }

    final int height = ((Host) mHost).getHeight();
    if (localVisibleRect.bottom < height || mPreviousLocalVisibleRect.bottom < height) {
      // View is going on/off the bottom of the screen. Check the tops to see if there is anything
      // that has changed.
      while (mPreviousTopsIndex < count
          && localVisibleRect.bottom
              > getLayoutOutput(layoutOutputTops.get(mPreviousTopsIndex)).getBounds().top) {
        final RenderTreeNode node = layoutOutputTops.get(mPreviousTopsIndex);
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        if (!ownsReference(node)) {
          acquireMountReference(
              node, mInput.getLayoutOutputPositionForId(layoutOutput.getId()), mInput, true);
          mComponentIdsMountedInThisFrame.add(layoutOutput.getId());
        }
        mPreviousTopsIndex++;
      }

      while (mPreviousTopsIndex > 0
          && localVisibleRect.bottom
              <= getLayoutOutput(layoutOutputTops.get(mPreviousTopsIndex - 1)).getBounds().top) {
        mPreviousTopsIndex--;
        final RenderTreeNode node = layoutOutputTops.get(mPreviousTopsIndex);
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        final long id = layoutOutput.getId();
        final int layoutOutputIndex = mInput.getLayoutOutputPositionForId(id);
        if (ownsReference(node)) {
          releaseMountReference(node, layoutOutputIndex, true);
        }
      }
    }

    for (int i = 0, size = mInput.getMountableOutputCount(); i < size; i++) {
      final RenderTreeNode node = mInput.getMountableOutputAt(i);
      final LayoutOutput layoutOutput = getLayoutOutput(node);
      final long layoutOutputId = layoutOutput.getId();

      if (!mComponentIdsMountedInThisFrame.contains(layoutOutputId)) {
        final Component component = layoutOutput.getComponent();
        if (component.hasChildLithoViews() && isLockedForMount(node)) {
          final int layoutOutputPosition = mInput.getLayoutOutputPositionForId(layoutOutputId);
          if (layoutOutputPosition != -1) {
            recursivelyNotifyVisibleBoundsChanged(getContentAt(i));
          }
        }
      }
    }

    mComponentIdsMountedInThisFrame.clear();

    return true;
  }

  private void setupPreviousMountableOutputData(Rect localVisibleRect) {
    if (localVisibleRect.isEmpty()) {
      return;
    }

    final List<RenderTreeNode> layoutOutputTops = mInput.getMountableOutputTops();
    final List<RenderTreeNode> layoutOutputBottoms = mInput.getMountableOutputBottoms();
    final int mountableOutputCount = mInput.getMountableOutputCount();

    mPreviousTopsIndex = mInput.getMountableOutputCount();
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.bottom <= getLayoutOutput(layoutOutputTops.get(i)).getBounds().top) {
        mPreviousTopsIndex = i;
        break;
      }
    }

    mPreviousBottomsIndex = mInput.getMountableOutputCount();
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.top < getLayoutOutput(layoutOutputBottoms.get(i)).getBounds().bottom) {
        mPreviousBottomsIndex = i;
        break;
      }
    }
  }

  @VisibleForTesting
  int getPreviousTopsIndex() {
    return mPreviousTopsIndex;
  }

  @VisibleForTesting
  int getPreviousBottomsIndex() {
    return mPreviousBottomsIndex;
  }

  static void onItemUpdated(Object content, Component component) {
    if (component.hasChildLithoViews()) {
      recursivelyNotifyVisibleBoundsChanged(content);
    }
  }

  private static boolean isMountedHostWithChildContent(@Nullable Object content) {
    return content instanceof Host && ((Host) content).getMountItemCount() > 0;
  }

  private static void recursivelyNotifyVisibleBoundsChanged(Object content) {
    assertMainThread();
    if (content instanceof RenderCoreExtensionHost) {
      final RenderCoreExtensionHost host = (RenderCoreExtensionHost) content;
      host.notifyVisibleBoundsChanged();
    } else if (content instanceof ViewGroup) {
      final ViewGroup parent = (ViewGroup) content;
      for (int i = 0; i < parent.getChildCount(); i++) {
        final View child = parent.getChildAt(i);
        recursivelyNotifyVisibleBoundsChanged(child);
      }
    }
  }
}
