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

import static com.facebook.rendercore.utils.ThreadUtils.assertMainThread;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Extension for performing incremental mount. */
public class IncrementalMountExtension
    extends MountExtension<IncrementalMountExtensionInput, IncrementalMountExtensionState> {

  private static final IncrementalMountExtension sInstance = new IncrementalMountExtension(false);
  private static final IncrementalMountExtension sInstanceAcquireDuringMount =
      new IncrementalMountExtension(true);

  private final boolean mAcquireReferencesDuringMount;

  private IncrementalMountExtension(final boolean acquireReferencesDuringMount) {
    mAcquireReferencesDuringMount = acquireReferencesDuringMount;
  }

  public static IncrementalMountExtension getInstance() {
    return sInstance;
  }

  public static IncrementalMountExtension getInstance(final boolean acquireReferencesDuringMount) {
    return acquireReferencesDuringMount ? sInstanceAcquireDuringMount : sInstance;
  }

  @Override
  public IncrementalMountExtensionState createState() {
    return new IncrementalMountExtensionState(mAcquireReferencesDuringMount);
  }

  @Override
  public void beforeMount(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final IncrementalMountExtensionInput input,
      final Rect localVisibleRect) {
    final IncrementalMountExtensionState state = extensionState.getState();

    releaseAcquiredReferencesForRemovedItems(extensionState, input);
    state.mInput = input;
    state.mPreviousLocalVisibleRect.setEmpty();

    if (!mAcquireReferencesDuringMount) {
      initIncrementalMount(extensionState, localVisibleRect, false);
    }

    setVisibleRect(state, localVisibleRect);
  }

  @Override
  public void afterMount(final ExtensionState<IncrementalMountExtensionState> extensionState) {
    final IncrementalMountExtensionState state = extensionState.getState();

    if (mAcquireReferencesDuringMount) {
      setupPreviousMountableOutputData(state, state.mPreviousLocalVisibleRect);
    }
  }

  @Override
  public void beforeMountItem(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final RenderTreeNode renderTreeNode,
      final int index) {
    if (!mAcquireReferencesDuringMount) {
      return;
    }

    final long id = renderTreeNode.getRenderUnit().getId();
    final IncrementalMountExtensionState state = extensionState.getState();
    final IncrementalMountOutput output = state.mInput.getIncrementalMountOutputForId(id);
    if (output == null) {
      throw new IllegalArgumentException("Output with id=" + id + " not found.");
    }

    maybeAcquireReference(extensionState, state.mPreviousLocalVisibleRect, output, false);
  }

  /**
   * Called when the visible bounds change to perform incremental mount. This is always called on a
   * non-dirty mount with a non-null localVisibleRect.
   *
   * @param localVisibleRect the current local visible rect of the root host.
   */
  @Override
  public void onVisibleBoundsChanged(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final Rect localVisibleRect) {
    assertMainThread();

    final IncrementalMountExtensionState state = extensionState.getState();
    if (state.mInput == null) {
      // Something notified the host that the visible bounds changed, but nothing was mounted yet.
      // Nothing to do.
      return;
    }

    if (localVisibleRect.isEmpty() && state.mPreviousLocalVisibleRect.isEmpty()) {
      return;
    }

    // Horizontally scrolling or no visible rect. Can't incrementally mount.
    if (state.mPreviousLocalVisibleRect.isEmpty()
        || localVisibleRect.isEmpty()
        || localVisibleRect.left != state.mPreviousLocalVisibleRect.left
        || localVisibleRect.right != state.mPreviousLocalVisibleRect.right) {
      initIncrementalMount(extensionState, localVisibleRect, true);
    } else {
      performIncrementalMount(extensionState, localVisibleRect);
    }

    setVisibleRect(state, localVisibleRect);
  }

  @Override
  public void onUnbind(final ExtensionState<IncrementalMountExtensionState> extensionState) {}

  @Override
  public void onBindItem(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final IncrementalMountExtensionState state = extensionState.getState();
    final long id = renderUnit.getId();

    if (state.mItemsShouldNotNotifyVisibleBoundsChangedOnChildren.remove(id)) {
      return;
    }

    recursivelyNotifyVisibleBoundsChanged(state.mInput, id, content);
  }

  @Override
  public void onMountItem(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final IncrementalMountExtensionState state = extensionState.getState();
    state.mItemsShouldNotNotifyVisibleBoundsChangedOnChildren.add(renderUnit.getId());
  }

  @Override
  public void onUnbindItem(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final IncrementalMountExtensionState state = extensionState.getState();
    final long id = renderUnit.getId();
    state.mItemsShouldNotNotifyVisibleBoundsChangedOnChildren.remove(id);
  }

  @Override
  public void onUnmount(final ExtensionState<IncrementalMountExtensionState> extensionState) {
    extensionState.releaseAllAcquiredReferences();

    final IncrementalMountExtensionState state = extensionState.getState();
    state.mPreviousLocalVisibleRect.setEmpty();
    state.mComponentIdsMountedInThisFrame.clear();
  }

  @Override
  public boolean canPreventMount() {
    return true;
  }

  static void recursivelyNotifyVisibleBoundsChanged(
      final IncrementalMountExtensionInput input, final long id, final Object content) {
    assertMainThread();
    if (input != null && input.renderUnitWithIdHostsRenderTrees(id)) {
      RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged(content);
    }
  }

  private static void releaseAcquiredReferencesForRemovedItems(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final IncrementalMountExtensionInput input) {
    final IncrementalMountExtensionState state = extensionState.getState();
    if (state.mInput == null) {
      return;
    }

    final Collection<IncrementalMountOutput> outputs = state.mInput.getIncrementalMountOutputs();
    for (IncrementalMountOutput output : outputs) {
      final long id = output.getId();
      if (input.getIncrementalMountOutputForId(id) == null && extensionState.ownsReference(id)) {
        extensionState.releaseMountReference(id, false);
      }
    }
  }

  private static void initIncrementalMount(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final Rect localVisibleRect,
      final boolean isMounting) {
    final IncrementalMountExtensionState state = extensionState.getState();
    final Collection<IncrementalMountOutput> outputs = state.mInput.getIncrementalMountOutputs();
    for (IncrementalMountOutput output : outputs) {
      maybeAcquireReference(extensionState, localVisibleRect, output, isMounting);
    }

    setupPreviousMountableOutputData(state, localVisibleRect);
  }

  private static void maybeAcquireReference(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final Rect localVisibleRect,
      final IncrementalMountOutput incrementalMountOutput,
      final boolean isMounting) {
    final IncrementalMountExtensionState state = extensionState.getState();
    final long id = incrementalMountOutput.getId();
    final Object content = getContentById(extensionState, id);
    // By default, a LayoutOutput passed in to mount will be mountable. Incremental mount can
    // override that if the item is outside the visible bounds.
    // TODO (T64830748): extract animations logic out of this.
    final boolean isMountable =
        isMountedHostWithChildContent(content)
            || Rect.intersects(localVisibleRect, incrementalMountOutput.getBounds())
            || isRootItem(id);
    final boolean hasAcquiredMountRef = extensionState.ownsReference(id);
    if (isMountable && !hasAcquiredMountRef) {
      extensionState.acquireMountReference(id, isMounting);
    } else if (!isMountable && hasAcquiredMountRef) {
      extensionState.releaseMountReference(id, isMounting);
    } else if (isMountable && hasAcquiredMountRef && isMounting) {
      // If we're in the process of mounting now, we know the item we're updating is already
      // mounted and that MountState.mount will not be called. We have to call the binder
      // ourselves.
      recursivelyNotifyVisibleBoundsChanged(state.mInput, id, content);
    }
  }

  private static void setVisibleRect(
      final IncrementalMountExtensionState state, @Nullable Rect localVisibleRect) {
    if (localVisibleRect != null) {
      state.mPreviousLocalVisibleRect.set(localVisibleRect);
    }
  }

  /**
   * @return true if this method did all the work that was necessary and there is no other content
   *     that needs mounting/unmounting in this mount step. If false then a full mount step should
   *     take place.
   */
  private static boolean performIncrementalMount(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final Rect localVisibleRect) {
    final IncrementalMountExtensionState state = extensionState.getState();
    final List<IncrementalMountOutput> byTopBounds = state.mInput.getOutputsOrderedByTopBounds();
    final List<IncrementalMountOutput> byBottomBounds =
        state.mInput.getOutputsOrderedByBottomBounds();
    final int count = state.mInput.getIncrementalMountOutputCount();

    if (localVisibleRect.top >= 0 || state.mPreviousLocalVisibleRect.top >= 0) {
      // View is going on/off the top of the screen. Check the bottoms to see if there is anything
      // that has moved on/off the top of the screen.
      while (state.mPreviousBottomsIndex < count
          && localVisibleRect.top
              >= byBottomBounds.get(state.mPreviousBottomsIndex).getBounds().bottom) {
        final IncrementalMountOutput node = byBottomBounds.get(state.mPreviousBottomsIndex);
        final long id = node.getId();
        if (extensionState.ownsReference(id)) {
          extensionState.releaseMountReference(id, true);
        }
        state.mPreviousBottomsIndex++;
      }

      while (state.mPreviousBottomsIndex > 0
          && localVisibleRect.top
              <= byBottomBounds.get(state.mPreviousBottomsIndex - 1).getBounds().bottom
          && localVisibleRect.bottom
              > byBottomBounds.get(state.mPreviousBottomsIndex - 1).getBounds().top) {
        state.mPreviousBottomsIndex--;
        final IncrementalMountOutput node = byBottomBounds.get(state.mPreviousBottomsIndex);
        final long id = node.getId();
        if (!extensionState.ownsReference(id)) {
          extensionState.acquireMountReference(id, true);
          state.mComponentIdsMountedInThisFrame.add(id);
        }
      }
    }

    Host root = extensionState.getRootHost();
    final int height = root != null ? root.getHeight() : 0;
    if (localVisibleRect.bottom < height || state.mPreviousLocalVisibleRect.bottom < height) {
      // View is going on/off the bottom of the screen. Check the tops to see if there is anything
      // that has changed.
      while (state.mPreviousTopsIndex < count
          && localVisibleRect.bottom >= byTopBounds.get(state.mPreviousTopsIndex).getBounds().top
          && localVisibleRect.top <= byTopBounds.get(state.mPreviousTopsIndex).getBounds().bottom) {
        final IncrementalMountOutput node = byTopBounds.get(state.mPreviousTopsIndex);
        final long id = node.getId();
        if (!extensionState.ownsReference(id)) {
          extensionState.acquireMountReference(id, true);
          state.mComponentIdsMountedInThisFrame.add(id);
        }
        state.mPreviousTopsIndex++;
      }

      while (state.mPreviousTopsIndex > 0
          && localVisibleRect.bottom
              < byTopBounds.get(state.mPreviousTopsIndex - 1).getBounds().top) {
        state.mPreviousTopsIndex--;
        final IncrementalMountOutput node = byTopBounds.get(state.mPreviousTopsIndex);
        final long id = node.getId();
        if (extensionState.ownsReference(id)) {
          extensionState.releaseMountReference(id, true);
        }
      }
    }

    final Collection<IncrementalMountOutput> outputs = state.mInput.getIncrementalMountOutputs();
    for (IncrementalMountOutput output : outputs) {
      final long id = output.getId();
      if (!state.mComponentIdsMountedInThisFrame.contains(id)) {
        if (isLockedForMount(extensionState, id)) {
          final Object content = getContentWithId(extensionState, id);
          if (content != null) {
            recursivelyNotifyVisibleBoundsChanged(state.mInput, id, content);
          }
        }
      }
    }

    state.mComponentIdsMountedInThisFrame.clear();

    return true;
  }

  private static void setupPreviousMountableOutputData(
      final IncrementalMountExtensionState state, final Rect localVisibleRect) {
    if (localVisibleRect.isEmpty()) {
      return;
    }

    final List<IncrementalMountOutput> byTopBounds = state.mInput.getOutputsOrderedByTopBounds();
    final List<IncrementalMountOutput> byBottomBounds =
        state.mInput.getOutputsOrderedByBottomBounds();
    final int mountableOutputCount = state.mInput.getIncrementalMountOutputCount();

    state.mPreviousTopsIndex = mountableOutputCount;
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.bottom <= byTopBounds.get(i).getBounds().top) {
        state.mPreviousTopsIndex = i;
        break;
      }
    }

    state.mPreviousBottomsIndex = mountableOutputCount;
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.top < byBottomBounds.get(i).getBounds().bottom) {
        state.mPreviousBottomsIndex = i;
        break;
      }
    }
  }

  private static @Nullable Object getContentWithId(
      final ExtensionState<IncrementalMountExtensionState> extensionState, final long id) {
    return getMountTarget(extensionState).getContentById(id);
  }

  @VisibleForTesting
  public static int getPreviousTopsIndex(final IncrementalMountExtensionState state) {
    return state.mPreviousTopsIndex;
  }

  @VisibleForTesting
  public static int getPreviousBottomsIndex(final IncrementalMountExtensionState state) {
    return state.mPreviousBottomsIndex;
  }

  private static boolean isMountedHostWithChildContent(final @Nullable Object content) {
    return content instanceof Host && ((Host) content).getMountItemCount() > 0;
  }

  @VisibleForTesting
  public static class IncrementalMountExtensionState {
    private final boolean mAcquireReferencesDuringMount;
    private final Rect mPreviousLocalVisibleRect = new Rect();
    private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();
    private final Set<Long> mItemsShouldNotNotifyVisibleBoundsChangedOnChildren = new HashSet<>();

    private IncrementalMountExtensionInput mInput;
    private int mPreviousTopsIndex;
    private int mPreviousBottomsIndex;

    private IncrementalMountExtensionState(boolean acquireReferencesDuringMount) {
      mAcquireReferencesDuringMount = acquireReferencesDuringMount;
    }
  }
}
