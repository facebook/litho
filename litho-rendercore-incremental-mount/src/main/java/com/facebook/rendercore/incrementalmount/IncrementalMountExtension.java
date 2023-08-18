/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static com.facebook.rendercore.incrementalmount.IncrementalMountExtensionConfigs.DEBUG_TAG;
import static com.facebook.rendercore.utils.ThreadUtils.assertMainThread;

import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountState;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.GapWorkerCallbacks;
import com.facebook.rendercore.extensions.InformsMountCallback;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.OnItemCallbacks;
import com.facebook.rendercore.extensions.VisibleBoundsCallbacks;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Extension for performing incremental mount. */
public class IncrementalMountExtension
    extends MountExtension<IncrementalMountExtensionInput, IncrementalMountExtensionState>
    implements VisibleBoundsCallbacks<IncrementalMountExtensionState>,
        GapWorkerCallbacks<IncrementalMountExtensionState>,
        OnItemCallbacks<IncrementalMountExtensionState>,
        InformsMountCallback {

  private static final IncrementalMountExtension sInstance = new IncrementalMountExtension(false);
  private static final IncrementalMountExtension sGapWorkerInstance =
      new IncrementalMountExtension(true);

  private final boolean mUseGapWorker;

  public IncrementalMountExtension(boolean useGapWorker) {
    mUseGapWorker = useGapWorker;
  }

  public static IncrementalMountExtension getInstance() {
    return sInstance;
  }

  public static IncrementalMountExtension getInstance(boolean useGapWorker) {
    return useGapWorker ? sGapWorkerInstance : sInstance;
  }

  @Override
  public IncrementalMountExtensionState createState() {
    return new IncrementalMountExtensionState();
  }

  @Override
  public void onRegisterForPremount(
      final ExtensionState<IncrementalMountExtensionState> extension, @Nullable Long frameTimeMs) {
    if (mUseGapWorker) {
      extension.getState().usesGapWorker = true;
      final IncrementalMountGapWorker worker = getGapWorker(extension);
      worker.add(extension.getMountDelegate(), frameTimeMs);
    }
  }

  @Override
  public void onUnregisterForPremount(
      final ExtensionState<IncrementalMountExtensionState> extension) {
    if (mUseGapWorker) {
      extension.getState().usesGapWorker = false;
      final IncrementalMountGapWorker worker = getGapWorker(extension);
      worker.remove(extension.getMountDelegate());
    }
  }

  @Override
  public void beforeMount(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final IncrementalMountExtensionInput input,
      final Rect localVisibleRect) {

    final boolean isTracing = extensionState.getTracer().isTracing();
    if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(DEBUG_TAG, "beforeMount");
    }
    if (isTracing) {
      extensionState.getTracer().beginSection("IncrementalMountExtension.beforeMount");
    }

    final IncrementalMountExtensionState state = extensionState.getState();

    releaseAcquiredReferencesForRemovedItems(extensionState, input);
    state.mInput = input;
    state.mPreviousLocalVisibleRect.setEmpty();

    setVisibleRect(state, localVisibleRect);

    if (isTracing) {
      extensionState.getTracer().endSection();
    }
  }

  @Override
  public void afterMount(final ExtensionState<IncrementalMountExtensionState> extensionState) {
    final boolean isTracing = extensionState.getTracer().isTracing();
    if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(DEBUG_TAG, "afterMount");
    }
    if (isTracing) {
      extensionState.getTracer().beginSection("IncrementalMountExtension.afterMount");
    }

    final IncrementalMountExtensionState state = extensionState.getState();

    setupPreviousMountableOutputData(state, state.mPreviousLocalVisibleRect);

    if (isTracing) {
      extensionState.getTracer().endSection();
    }
  }

  @Override
  public void beforeMountItem(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final RenderTreeNode renderTreeNode,
      final int index) {
    final boolean isTracing = extensionState.getTracer().isTracing();
    if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(DEBUG_TAG, "beforeMountItem [id=" + renderTreeNode.getRenderUnit().getId() + "]");
    }
    if (isTracing) {
      extensionState.getTracer().beginSection("IncrementalMountExtension.beforeMountItem");
    }

    final long id = renderTreeNode.getRenderUnit().getId();
    final IncrementalMountExtensionState state = extensionState.getState();

    if (state.mInput == null) {
      return;
    }

    final IncrementalMountOutput output = state.mInput.getIncrementalMountOutputForId(id);
    if (output == null) {
      throw new IllegalArgumentException("Output with id=" + id + " not found.");
    }

    maybeAcquireReference(extensionState, state.mPreviousLocalVisibleRect, output, false);

    if (isTracing) {
      extensionState.getTracer().endSection();
    }
  }

  @Override
  public boolean hasItemToMount(ExtensionState<IncrementalMountExtensionState> extensionState) {
    final IncrementalMountExtensionState state = extensionState.getState();
    if (state.mInput == null) {
      return false;
    }

    final int count = state.mInput.getIncrementalMountOutputCount();
    return state.mPreviousBottomsIndex > 0 || state.mPreviousTopsIndex < count;
  }

  @Override
  public void premountNext(ExtensionState<IncrementalMountExtensionState> extensionState) {
    final IncrementalMountExtensionState state = extensionState.getState();
    if (state.mInput == null) {
      return;
    }

    final List<IncrementalMountOutput> atTheEnd = state.mInput.getOutputsOrderedByTopBounds();
    final List<IncrementalMountOutput> atTheStart = state.mInput.getOutputsOrderedByBottomBounds();
    final int count = state.mInput.getIncrementalMountOutputCount();

    if (state.mPreviousTopsIndex < count && state.mPreviousTopsIndex >= 0) {
      final IncrementalMountOutput node = atTheEnd.get(state.mPreviousTopsIndex);
      final long id = node.getId();
      if (!extensionState.ownsReference(id)) {
        extensionState.acquireMountReference(node.getId(), true);
      }
      state.mPreviousTopsIndex++;
    } else if (state.mPreviousBottomsIndex > 0 && state.mPreviousBottomsIndex <= count) {
      final IncrementalMountOutput node = atTheStart.get(state.mPreviousBottomsIndex - 1);
      final long id = node.getId();
      if (!extensionState.ownsReference(id)) {
        extensionState.acquireMountReference(node.getId(), true);
      }
      state.mPreviousBottomsIndex--;
    }
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

    final boolean isTracing = extensionState.getTracer().isTracing();
    if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(DEBUG_TAG, "onVisibleBoundsChanged [visibleBounds=" + localVisibleRect + "]");
    }
    if (isTracing) {
      extensionState.getTracer().beginSection("IncrementalMountExtension.onVisibleBoundsChanged");
    }

    final IncrementalMountExtensionState state = extensionState.getState();
    if (state.mInput == null) {
      // Something notified the host that the visible bounds changed, but nothing was mounted yet.
      // Nothing to do.
      if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
        Log.d(DEBUG_TAG, "Skipping: Input is empty.");
      }
      if (isTracing) {
        extensionState.getTracer().endSection();
      }
      return;
    }

    if (localVisibleRect.isEmpty() && state.mPreviousLocalVisibleRect.isEmpty()) {
      if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
        Log.d(DEBUG_TAG, "Skipping: Visible area is 0");
      }
      if (isTracing) {
        extensionState.getTracer().endSection();
      }
      return;
    }

    // Horizontally scrolling or no visible rect. Can't incrementally mount.
    if (state.mPreviousLocalVisibleRect.isEmpty()
        || localVisibleRect.isEmpty()
        || localVisibleRect.left != state.mPreviousLocalVisibleRect.left
        || localVisibleRect.right != state.mPreviousLocalVisibleRect.right) {
      initIncrementalMount(extensionState, localVisibleRect);
    } else {
      performIncrementalMount(extensionState, localVisibleRect);
    }

    setVisibleRect(state, localVisibleRect);

    if (isTracing) {
      extensionState.getTracer().endSection();
    }
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

    if (!renderUnit.doesMountRenderTreeHosts()) {
      return;
    }
    recursivelyNotifyVisibleBoundsChanged(extensionState, id, content);
  }

  @Override
  public void onMountItem(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final long id = renderUnit.getId();

    if (id == MountState.ROOT_HOST_ID && !extensionState.ownsReference(id)) {
      extensionState.acquireMountReference(id, false);
    }

    final IncrementalMountExtensionState state = extensionState.getState();
    if (state.mInput != null && state.mInput.renderUnitWithIdHostsRenderTrees(id)) {
      state.mItemsShouldNotNotifyVisibleBoundsChangedOnChildren.add(id);
      state.mMountedOutputIdsWithNestedContent.put(id, content);
    }
  }

  @Override
  public boolean shouldUpdateItem(
      ExtensionState<IncrementalMountExtensionState> extensionState,
      RenderUnit<?> previousRenderUnit,
      @Nullable Object previousLayoutData,
      RenderUnit<?> nextRenderUnit,
      @Nullable Object nextLayoutData) {
    return false;
  }

  @Override
  public void onUnmountItem(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final IncrementalMountExtensionState state = extensionState.getState();
    final long id = renderUnit.getId();

    if (id == MountState.ROOT_HOST_ID && extensionState.ownsReference(id)) {
      extensionState.releaseMountReference(id, false);
    }

    state.mMountedOutputIdsWithNestedContent.remove(id);
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
  public void onBoundsAppliedToItem(
      ExtensionState<IncrementalMountExtensionState> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

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
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final long id,
      final Object content) {
    assertMainThread();
    final boolean isTracing = extensionState.getTracer().isTracing();
    if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(DEBUG_TAG, "RecursivelyNotify [RenderUnit=" + id + "]");
    }
    if (isTracing) {
      extensionState.getTracer().beginSection("IncrementalMountExtension.recursivelyNotify");
    }
    extensionState.getMountDelegate().notifyVisibleBoundsChangedForItem(content);
    if (isTracing) {
      extensionState.getTracer().endSection();
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
      final Rect localVisibleRect) {
    final IncrementalMountExtensionState state = extensionState.getState();

    if (state.mInput == null) {
      return;
    }

    final Collection<IncrementalMountOutput> outputs = state.mInput.getIncrementalMountOutputs();
    for (IncrementalMountOutput output : outputs) {
      maybeAcquireReference(extensionState, localVisibleRect, output, true);
    }

    setupPreviousMountableOutputData(state, localVisibleRect);
  }

  private static void maybeAcquireReference(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final Rect localVisibleRect,
      final IncrementalMountOutput incrementalMountOutput,
      final boolean isMounting) {
    final long id = incrementalMountOutput.getId();
    final Object content = getContentById(extensionState, id);
    // By default, a LayoutOutput passed in to mount will be mountable. Incremental mount can
    // override that if the item is outside the visible bounds.
    // TODO (T64830748): extract animations logic out of this.

    final boolean isMountable =
        isMountedHostWithChildContent(content)
            || Rect.intersects(localVisibleRect, incrementalMountOutput.getBounds())
            || isRootItem(id)
            || incrementalMountOutput.excludeFromIncrementalMount();
    final boolean hasAcquiredMountRef = extensionState.ownsReference(id);
    if (isMountable && !hasAcquiredMountRef) {
      extensionState.acquireMountReference(incrementalMountOutput.getId(), isMounting);
    } else if (!isMountable && hasAcquiredMountRef && !extensionState.getState().usesGapWorker) {
      extensionState.releaseMountReference(id, isMounting);
    }
  }

  private static void setVisibleRect(
      final IncrementalMountExtensionState state, @Nullable Rect localVisibleRect) {
    if (localVisibleRect != null) {
      state.mPreviousLocalVisibleRect.set(localVisibleRect);
    }
  }

  private static void performIncrementalMount(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final Rect localVisibleRect) {
    final IncrementalMountExtensionState state = extensionState.getState();
    if (state.mInput == null) {
      return;
    }

    final boolean isTracing = extensionState.getTracer().isTracing();
    if (isTracing) {
      extensionState.getTracer().beginSection("performIncrementalMount");
    }

    final List<IncrementalMountOutput> byTopBounds = state.mInput.getOutputsOrderedByTopBounds();
    final List<IncrementalMountOutput> byBottomBounds =
        state.mInput.getOutputsOrderedByBottomBounds();
    final int count = state.mInput.getIncrementalMountOutputCount();

    int itemsMounted = 0;
    int itemsUnmounted = 0;

    if (localVisibleRect.top >= 0 || state.mPreviousLocalVisibleRect.top >= 0) {
      // View is going on/off the top of the screen. Check the bottoms to see if there is anything
      // that has moved on/off the top of the screen.
      while (state.mPreviousBottomsIndex < count
          && localVisibleRect.top
              >= byBottomBounds.get(state.mPreviousBottomsIndex).getBounds().bottom) {

        final IncrementalMountOutput node = byBottomBounds.get(state.mPreviousBottomsIndex);
        final long id = node.getId();

        if (extensionState.ownsReference(id)
            && !node.excludeFromIncrementalMount()
            && !state.usesGapWorker) {
          extensionState.releaseMountReference(id, true);
          if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
            itemsUnmounted++;
          }
        }

        state.mPreviousBottomsIndex++;
      }

      while (state.mPreviousBottomsIndex > 0
          && localVisibleRect.top
              < byBottomBounds.get(state.mPreviousBottomsIndex - 1).getBounds().bottom) {

        final IncrementalMountOutput node = byBottomBounds.get(state.mPreviousBottomsIndex - 1);
        final long id = node.getId();

        // Item should still be in the view port.
        if (localVisibleRect.bottom
            >= byBottomBounds.get(state.mPreviousBottomsIndex - 1).getBounds().top) {
          if (!extensionState.ownsReference(id)) {
            extensionState.acquireMountReference(node.getId(), true);
            state.mComponentIdsMountedInThisFrame.add(id);
            if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
              itemsMounted++;
            }
          }
        }

        state.mPreviousBottomsIndex--;
      }
    }

    Host root = extensionState.getRootHost();
    final int height = root.getHeight();
    if (localVisibleRect.bottom < height || state.mPreviousLocalVisibleRect.bottom < height) {
      // View is going on/off the bottom of the screen. Check the tops to see if there is anything
      // that has changed.
      while (state.mPreviousTopsIndex < count
          && localVisibleRect.bottom >= byTopBounds.get(state.mPreviousTopsIndex).getBounds().top) {

        final IncrementalMountOutput node = byTopBounds.get(state.mPreviousTopsIndex);
        final long id = node.getId();

        // Item should still be in the view port.
        if (localVisibleRect.top <= byTopBounds.get(state.mPreviousTopsIndex).getBounds().bottom) {
          if (!extensionState.ownsReference(id)) {
            extensionState.acquireMountReference(node.getId(), true);
            state.mComponentIdsMountedInThisFrame.add(id);
            if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
              itemsMounted++;
            }
          }
        }

        state.mPreviousTopsIndex++;
      }

      while (state.mPreviousTopsIndex > 0
          && localVisibleRect.bottom
              < byTopBounds.get(state.mPreviousTopsIndex - 1).getBounds().top) {

        final IncrementalMountOutput node = byTopBounds.get(state.mPreviousTopsIndex - 1);
        final long id = node.getId();

        if (extensionState.ownsReference(id)
            && !node.excludeFromIncrementalMount()
            && !state.usesGapWorker) {
          extensionState.releaseMountReference(id, true);
          if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
            itemsUnmounted++;
          }
        }

        state.mPreviousTopsIndex--;
      }
    }

    if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          DEBUG_TAG,
          "Updates: [Items Mounted=" + itemsMounted + ", Items Unmounted=" + itemsUnmounted + "]");
    }

    for (long id : state.mMountedOutputIdsWithNestedContent.keySet()) {
      if (state.mComponentIdsMountedInThisFrame.contains(id)) {
        continue;
      }

      final Object content = state.mMountedOutputIdsWithNestedContent.get(id);
      if (content != null) {
        recursivelyNotifyVisibleBoundsChanged(extensionState, id, content);
      }
    }

    state.mComponentIdsMountedInThisFrame.clear();

    if (isTracing) {
      extensionState.getTracer().endSection();
    }
  }

  private static void setupPreviousMountableOutputData(
      final IncrementalMountExtensionState state, final Rect localVisibleRect) {
    if (localVisibleRect.isEmpty() || state.mInput == null) {
      return;
    }

    final List<IncrementalMountOutput> byTopBounds = state.mInput.getOutputsOrderedByTopBounds();
    final List<IncrementalMountOutput> byBottomBounds =
        state.mInput.getOutputsOrderedByBottomBounds();
    final int mountableOutputCount = state.mInput.getIncrementalMountOutputCount();

    state.mPreviousTopsIndex =
        binarySearchTopBoundary(localVisibleRect.bottom, byTopBounds, mountableOutputCount);
    state.mPreviousBottomsIndex =
        binarySearchBottomBoundary(localVisibleRect.top, byBottomBounds, mountableOutputCount);
  }

  private static IncrementalMountGapWorker getGapWorker(
      ExtensionState<IncrementalMountExtensionState> state) {
    return IncrementalMountGapWorker.get(
        ViewCompat.getDisplay(state.getRootHost()), state.getTracer());
  }

  private static int binarySearchTopBoundary(
      final int rectBottom, final List<IncrementalMountOutput> byTopBounds, final int totalCount) {
    int low = 0;
    int high = totalCount - 1;

    while (low <= high) {
      int mid = low + ((high - low) / 2);

      if (rectBottom > byTopBounds.get(mid).getBounds().top) {
        low = mid + 1;
      } else if (mid > 0 && rectBottom <= byTopBounds.get(mid - 1).getBounds().top) {
        high = mid - 1;
      } else if (mid == 0 || rectBottom > byTopBounds.get(mid - 1).getBounds().top) {
        return mid;
      }
    }

    return totalCount;
  }

  private static int binarySearchBottomBoundary(
      final int rectTop, final List<IncrementalMountOutput> byBottomBounds, final int totalCount) {
    int low = 0;
    int high = totalCount - 1;

    while (low <= high) {
      int mid = low + ((high - low) / 2);

      if (rectTop >= byBottomBounds.get(mid).getBounds().bottom) {
        low = mid + 1;
      } else if (mid > 0 && rectTop < byBottomBounds.get(mid - 1).getBounds().bottom) {
        high = mid - 1;
      } else if (mid == 0 || rectTop >= byBottomBounds.get(mid - 1).getBounds().bottom) {
        return mid;
      }
    }

    return totalCount;
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
    private final Rect mPreviousLocalVisibleRect = new Rect();
    private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();
    private final Set<Long> mItemsShouldNotNotifyVisibleBoundsChangedOnChildren = new HashSet<>();
    private final HashMap<Long, Object> mMountedOutputIdsWithNestedContent = new HashMap<>(8);

    private @Nullable IncrementalMountExtensionInput mInput;
    private int mPreviousTopsIndex;
    private int mPreviousBottomsIndex;

    private boolean usesGapWorker = false;
  }
}
