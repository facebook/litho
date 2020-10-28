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

package com.facebook.rendercore.incrementalmount;

import static com.facebook.rendercore.utils.ThreadUtils.isMainThread;

import android.graphics.Rect;
import android.util.LongSparseArray;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.RenderCoreExtensionHost;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Extension for performing incremental mount. */
public class IncrementalMountExtension
    extends MountExtension<IncrementalMountExtensionInput, Void> {

  private final Rect mPreviousLocalVisibleRect = new Rect();
  private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();
  private final IncrementalMountBinder mAttachDetachBinder;
  private final LongSparseArray<IncrementalMountOutput> mPendingImmediateRemoval =
      new LongSparseArray<>();
  private final boolean mAcquireReferencesDuringMount;

  private IncrementalMountExtensionInput mInput;
  private int mPreviousTopsIndex;
  private int mPreviousBottomsIndex;
  private ExtensionState<Void> mExtensionState;

  public IncrementalMountExtension() {
    this(false);
  }

  public IncrementalMountExtension(final boolean acquireReferencesDuringMount) {
    mAcquireReferencesDuringMount = acquireReferencesDuringMount;
    mAttachDetachBinder = new IncrementalMountBinder(this);
  }

  @Override
  public void beforeMount(
      ExtensionState<Void> extensionState,
      IncrementalMountExtensionInput input,
      Rect localVisibleRect) {
    releaseAcquiredReferencesForRemovedItems(input);
    mInput = input;
    mExtensionState = extensionState;
    mPreviousLocalVisibleRect.setEmpty();

    if (!mAcquireReferencesDuringMount) {
      initIncrementalMount(localVisibleRect, false);
    }

    setVisibleRect(localVisibleRect);
  }

  @Override
  public void beforeMountItem(
      ExtensionState<Void> extensionState, RenderTreeNode renderTreeNode, int index) {
    if (!mAcquireReferencesDuringMount) {
      return;
    }

    final IncrementalMountOutput output = mInput.getIncrementalMountOutputAt(index);

    maybeAcquireReference(mPreviousLocalVisibleRect, output, index, false);
  }

  @Override
  public void afterMount(ExtensionState<Void> extensionState) {
    if (mAcquireReferencesDuringMount) {
      setupPreviousMountableOutputData(mPreviousLocalVisibleRect);
    }

    // remove everything that was marked as needing to be removed.
    // At this point we know that all items have been moved to the appropriate hosts.
    for (int i = 0, size = mPendingImmediateRemoval.size(); i < size; i++) {
      final long position = mPendingImmediateRemoval.keyAt(i);
      final IncrementalMountOutput incrementalMountOutput = mPendingImmediateRemoval.get(position);
      final long id = incrementalMountOutput.getId();
      if (extensionState.ownsReference(id)) {
        extensionState.releaseMountReference(id, (int) position, true);
      }
    }
    mPendingImmediateRemoval.clear();
  }

  @Override
  public void onUnmount(ExtensionState<Void> extensionState) {
    extensionState.resetAcquiredReferences();
    mPreviousLocalVisibleRect.setEmpty();
    mPendingImmediateRemoval.clear();
    mComponentIdsMountedInThisFrame.clear();
  }

  /**
   * Called when LithoView visible bounds change to perform incremental mount. This is always called
   * on a non-dirty mount with a non-null localVisibleRect.
   *
   * @param localVisibleRect
   */
  @Override
  public void onVisibleBoundsChanged(ExtensionState<Void> extensionState, Rect localVisibleRect) {
    isMainThread();

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
  }

  @Override
  public void onUnbind(ExtensionState<Void> extensionState) {}

  @Override
  protected Void createState() {
    return null;
  }

  private void acquireMountReferenceEnsureHostIsMounted(
      IncrementalMountOutput incrementalMountOutput, final int position, final boolean isMounting) {
    // Make sure the host is mounted before the child.
    final long hostId = incrementalMountOutput.getHostId();
    if (hostId >= 0) {
      if (!mExtensionState.ownsReference(hostId)) {
        final int hostIndex = mInput.getPositionForId(hostId);
        mExtensionState.acquireMountReference(
            hostId, hostIndex, isMounting || mAcquireReferencesDuringMount);
      }
    }

    mExtensionState.acquireMountReference(incrementalMountOutput.getId(), position, isMounting);
  }

  @Override
  public boolean canPreventMount() {
    return true;
  }

  public RenderUnit.Binder getAttachDetachBinder() {
    return mAttachDetachBinder;
  }

  void recursivelyNotifyVisibleBoundsChanged(final long id, final Object content) {
    if (mInput != null && mInput.renderUnitWithIdHostsRenderTrees(id)) {
      recursivelyNotifyVisibleBoundsChanged(content);
    }
  }

  private void releaseAcquiredReferencesForRemovedItems(IncrementalMountExtensionInput input) {
    if (mInput == null) {
      return;
    }

    for (int i = 0, size = mInput.getIncrementalMountOutputCount(); i < size; i++) {
      final IncrementalMountOutput node = mInput.getIncrementalMountOutputAt(i);
      final long id = node.getId();
      if (input.getPositionForId(id) < 0 && mExtensionState.ownsReference(id)) {
        mExtensionState.releaseMountReference(id, i, false);
      }
    }
  }

  private void initIncrementalMount(Rect localVisibleRect, boolean isMounting) {
    for (int i = 0, size = mInput.getIncrementalMountOutputCount(); i < size; i++) {
      final IncrementalMountOutput node = mInput.getIncrementalMountOutputAt(i);
      maybeAcquireReference(localVisibleRect, node, i, isMounting);
    }

    setupPreviousMountableOutputData(localVisibleRect);
  }

  private void maybeAcquireReference(
      final Rect localVisibleRect,
      final IncrementalMountOutput incrementalMountOutput,
      final int position,
      final boolean isMounting) {
    final Object content = getContentAt(mExtensionState, position);
    final long id = incrementalMountOutput.getId();
    // By default, a LayoutOutput passed in to mount will be mountable. Incremental mount can
    // override that if the item is outside the visible bounds.
    // TODO (T64830748): extract animations logic out of this.
    final boolean isMountable =
        isMountedHostWithChildContent(content)
            || Rect.intersects(localVisibleRect, incrementalMountOutput.getBounds())
            || isRootItem(mExtensionState, position);
    final boolean hasAcquiredMountRef = mExtensionState.ownsReference(id);
    if (isMountable && !hasAcquiredMountRef) {
      acquireMountReferenceEnsureHostIsMounted(incrementalMountOutput, position, isMounting);
    } else if (!isMountable && hasAcquiredMountRef) {
      if (!isMounting) {
        mPendingImmediateRemoval.put(position, incrementalMountOutput);
      } else if (mExtensionState.ownsReference(id)) {
        mExtensionState.releaseMountReference(id, position, true);
      }
    } else if (isMountable && hasAcquiredMountRef && isMounting) {
      // If we're in the process of mounting now, we know the item we're updating is already
      // mounted and that MountState.mount will not be called. We have to call the binder
      // ourselves.
      recursivelyNotifyVisibleBoundsChanged(id, content);
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
    final List<IncrementalMountOutput> byTopBounds = mInput.getOutputsOrderedByTopBounds();
    final List<IncrementalMountOutput> byBottomBounds = mInput.getOutputsOrderedByBottomBounds();
    final int count = mInput.getIncrementalMountOutputCount();

    if (localVisibleRect.top > 0 || mPreviousLocalVisibleRect.top > 0) {
      // View is going on/off the top of the screen. Check the bottoms to see if there is anything
      // that has moved on/off the top of the screen.
      while (mPreviousBottomsIndex < count
          && localVisibleRect.top >= byBottomBounds.get(mPreviousBottomsIndex).getBounds().bottom) {
        final IncrementalMountOutput node = byBottomBounds.get(mPreviousBottomsIndex);
        final long id = node.getId();
        final int layoutOutputIndex = mInput.getPositionForId(id);
        if (mExtensionState.ownsReference(id)) {
          mExtensionState.releaseMountReference(id, layoutOutputIndex, true);
        }
        mPreviousBottomsIndex++;
      }

      while (mPreviousBottomsIndex > 0
          && localVisibleRect.top
              < byBottomBounds.get(mPreviousBottomsIndex - 1).getBounds().bottom) {
        mPreviousBottomsIndex--;
        final IncrementalMountOutput node = byBottomBounds.get(mPreviousBottomsIndex);
        final long id = node.getId();
        if (!mExtensionState.ownsReference(id)) {
          acquireMountReferenceEnsureHostIsMounted(node, mInput.getPositionForId(id), true);
          mComponentIdsMountedInThisFrame.add(id);
        }
      }
    }

    Host root = getRootHost(mExtensionState);
    final int height = root != null ? root.getHeight() : 0;
    if (localVisibleRect.bottom < height || mPreviousLocalVisibleRect.bottom < height) {
      // View is going on/off the bottom of the screen. Check the tops to see if there is anything
      // that has changed.
      while (mPreviousTopsIndex < count
          && localVisibleRect.bottom > byTopBounds.get(mPreviousTopsIndex).getBounds().top) {
        final IncrementalMountOutput node = byTopBounds.get(mPreviousTopsIndex);
        final long id = node.getId();
        if (!mExtensionState.ownsReference(id)) {
          acquireMountReferenceEnsureHostIsMounted(node, mInput.getPositionForId(id), true);
          mComponentIdsMountedInThisFrame.add(id);
        }
        mPreviousTopsIndex++;
      }

      while (mPreviousTopsIndex > 0
          && localVisibleRect.bottom <= byTopBounds.get(mPreviousTopsIndex - 1).getBounds().top) {
        mPreviousTopsIndex--;
        final IncrementalMountOutput node = byTopBounds.get(mPreviousTopsIndex);
        final long id = node.getId();
        final int layoutOutputIndex = mInput.getPositionForId(id);
        if (mExtensionState.ownsReference(id)) {
          mExtensionState.releaseMountReference(id, layoutOutputIndex, true);
        }
      }
    }

    for (int i = 0, size = mInput.getIncrementalMountOutputCount(); i < size; i++) {
      final IncrementalMountOutput node = mInput.getIncrementalMountOutputAt(i);
      final long id = node.getId();

      if (!mComponentIdsMountedInThisFrame.contains(id)) {
        if (isLockedForMount(mExtensionState, id)) {
          final Object content = getContentWithId(id);
          if (content != null) {
            recursivelyNotifyVisibleBoundsChanged(id, content);
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

    final List<IncrementalMountOutput> byTopBounds = mInput.getOutputsOrderedByTopBounds();
    final List<IncrementalMountOutput> byBottomBounds = mInput.getOutputsOrderedByBottomBounds();
    final int mountableOutputCount = mInput.getIncrementalMountOutputCount();

    mPreviousTopsIndex = mountableOutputCount;
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.bottom <= byTopBounds.get(i).getBounds().top) {
        mPreviousTopsIndex = i;
        break;
      }
    }

    mPreviousBottomsIndex = mountableOutputCount;
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.top < byBottomBounds.get(i).getBounds().bottom) {
        mPreviousBottomsIndex = i;
        break;
      }
    }
  }

  private @Nullable Object getContentWithId(long id) {
    return getMountTarget(mExtensionState).getContentById(id);
  }

  @VisibleForTesting
  public int getPreviousTopsIndex() {
    return mPreviousTopsIndex;
  }

  @VisibleForTesting
  public int getPreviousBottomsIndex() {
    return mPreviousBottomsIndex;
  }

  private static boolean isMountedHostWithChildContent(@Nullable Object content) {
    return content instanceof Host && ((Host) content).getMountItemCount() > 0;
  }

  private static void recursivelyNotifyVisibleBoundsChanged(final Object content) {
    isMainThread();
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
