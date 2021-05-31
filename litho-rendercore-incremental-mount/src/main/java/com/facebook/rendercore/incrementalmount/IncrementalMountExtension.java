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

import static com.facebook.rendercore.incrementalmount.IncrementalMountUtils.log;
import static com.facebook.rendercore.utils.ThreadUtils.assertMainThread;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.ErrorReporter;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.HostNotMountedException;
import com.facebook.rendercore.LogLevel;
import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Extension for performing incremental mount. */
public class IncrementalMountExtension
    extends MountExtension<IncrementalMountExtensionInput, IncrementalMountExtensionState> {

  private static final IncrementalMountExtension sInstance = new IncrementalMountExtension(false);
  private static final IncrementalMountExtension sInstanceDisableForceAcquireMount =
      new IncrementalMountExtension(true);
  private final boolean mDisableForceAcquireHostMount;

  private IncrementalMountExtension(boolean disableForceAcquireHostMount) {
    mDisableForceAcquireHostMount = disableForceAcquireHostMount;
  }

  public static IncrementalMountExtension getInstance(boolean disableForceAcquireMount) {
    return disableForceAcquireMount ? sInstanceDisableForceAcquireMount : sInstance;
  }

  @Override
  public IncrementalMountExtensionState createState() {
    return new IncrementalMountExtensionState(mDisableForceAcquireHostMount);
  }

  @Override
  public void beforeMount(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final IncrementalMountExtensionInput input,
      final Rect localVisibleRect) {

    log("beforeMount");
    RenderCoreSystrace.beginSection("IncrementalMountExtension.beforeMount");

    final IncrementalMountExtensionState state = extensionState.getState();

    releaseAcquiredReferencesForRemovedItems(extensionState, input);
    state.mInput = input;
    state.mPreviousLocalVisibleRect.setEmpty();

    setVisibleRect(state, localVisibleRect);

    RenderCoreSystrace.endSection();
  }

  @Override
  public void afterMount(final ExtensionState<IncrementalMountExtensionState> extensionState) {

    log("afterMount");
    RenderCoreSystrace.beginSection("IncrementalMountExtension.afterMount");

    final IncrementalMountExtensionState state = extensionState.getState();

    setupPreviousMountableOutputData(state, state.mPreviousLocalVisibleRect);

    RenderCoreSystrace.endSection();
  }

  @Override
  public void beforeMountItem(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final RenderTreeNode renderTreeNode,
      final int index) {
    log("beforeMountItem [id=" + renderTreeNode.getRenderUnit().getId() + "]");
    RenderCoreSystrace.beginSection("IncrementalMountExtension.beforeMountItem");

    final long id = renderTreeNode.getRenderUnit().getId();
    final IncrementalMountExtensionState state = extensionState.getState();
    final IncrementalMountOutput output = state.mInput.getIncrementalMountOutputForId(id);
    if (output == null) {
      throw new IllegalArgumentException("Output with id=" + id + " not found.");
    }

    maybeAcquireReference(extensionState, state.mPreviousLocalVisibleRect, output, false);

    RenderCoreSystrace.endSection();
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

    log("onVisibleBoundsChanged [visibleBounds=" + localVisibleRect + "]");
    RenderCoreSystrace.beginSection("IncrementalMountExtension.onVisibleBoundsChanged");

    final IncrementalMountExtensionState state = extensionState.getState();
    if (state.mInput == null) {
      // Something notified the host that the visible bounds changed, but nothing was mounted yet.
      // Nothing to do.
      log("Skipping: Input is empty.");
      RenderCoreSystrace.endSection();
      return;
    }

    if (localVisibleRect.isEmpty() && state.mPreviousLocalVisibleRect.isEmpty()) {
      log("Skipping: Visible area is 0");
      RenderCoreSystrace.endSection();
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

    RenderCoreSystrace.endSection();
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

  private static void acquireMountReferenceEnsureHostIsMounted(
      final Rect localVisibleRect,
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final IncrementalMountOutput output,
      final boolean isMounting) {
    if (extensionState.getState().mDisableForceAcquireHostMount) {
      extensionState.acquireMountReference(output.getId(), isMounting);
    } else {
      acquireMountReferenceEnsureHostIsMounted(
          localVisibleRect, extensionState, output, isMounting, false, null);
    }
  }

  private static void acquireMountReferenceEnsureHostIsMounted(
      final Rect localVisibleRect,
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final IncrementalMountOutput output,
      final boolean isMounting,
      final boolean forceAcquiredHostToMount,
      @Nullable List<String> recursionSource) {

    final IncrementalMountOutput host = output.getHostOutput();

    final boolean ownsHostRef = host == null ? true : extensionState.ownsReference(host.getId());
    final boolean needsHostMount = !ownsHostRef || !isHostMounted(extensionState, host);

    // If id is ROOT_HOST_ID then already at root host.
    if (needsHostMount) {

      if (recursionSource == null) {
        recursionSource = new ArrayList<>();
      }

      final String errorMessage =
          getHostNotMountedErrorMessage(localVisibleRect, isMounting, output, host, ownsHostRef);

      // This should not happen, but we won't crash until we understand what's going on here.
      // We'll force mounting on the host by bypassing ref count instead.
      if (ownsHostRef) {
        ErrorReporter.getInstance().report(LogLevel.ERROR, "IncrementalMount", errorMessage);
      }

      recursionSource.add(errorMessage);

      // If not root host or if no reference was acquired, acquire it.
      acquireMountReferenceEnsureHostIsMounted(
          localVisibleRect, extensionState, host, true, ownsHostRef, recursionSource);
      extensionState.getState().mComponentIdsMountedInThisFrame.add(host.getId());
    }

    try {
      if (forceAcquiredHostToMount) {
        extensionState.getMountDelegate().getMountDelegateTarget().notifyMount(output.getId());
      } else {
        extensionState.acquireMountReference(output.getId(), isMounting);
      }
    } catch (HostNotMountedException e) {
      String errorMessage = getHostNotMountedExceptionMessage(e, recursionSource, output, host);

      throw new RuntimeException(errorMessage);
    }
  }

  private static boolean isHostMounted(
      final ExtensionState<IncrementalMountExtensionState> extensionState,
      final IncrementalMountOutput hostOutput) {
    return hostOutput == null
        ? true
        : extensionState.getMountDelegate().getContentById(hostOutput.getId()) != null;
  }

  private static String getHostNotMountedExceptionMessage(
      HostNotMountedException e,
      List<String> additionalErrorMessages,
      IncrementalMountOutput output,
      IncrementalMountOutput host) {
    String errorMessage = "Failed to mount item with id " + output.getId() + ".";
    errorMessage += host == null ? " Host is null." : " Host id is " + host.getId() + "\n";

    if (additionalErrorMessages != null) {
      for (String source : additionalErrorMessages) {
        errorMessage += source + "\n";
      }
    }

    errorMessage += "MountDelegateTarget mounting info: ";
    errorMessage +=
        " renderUnit id "
            + e.renderUnit.getId()
            + ", host "
            + (e.parentRenderUnit == null ? " null " : " id " + e.parentRenderUnit.getId())
            + "\n";
    errorMessage += e.getMessage();

    return errorMessage;
  }

  private static String getHostNotMountedErrorMessage(
      Rect localVisibleRect,
      boolean isMounting,
      IncrementalMountOutput output,
      IncrementalMountOutput host,
      boolean ownsHostRef) {
    return ownsHostRef
        ? "Forcing acquired host mount in "
            + localVisibleRect
            + ", mounting "
            + isMounting
            + ". Item:  id = "
            + output.getId()
            + ", bounds = "
            + output.getBounds()
            + ". Host: id = "
            + host.getId()
            + ", originalBounds = "
            + host.getOriginalBounds()
            + ", bounds = "
            + host.getBounds()
        : "Forcing unaacquired host mount in "
            + localVisibleRect
            + ", mounting "
            + isMounting
            + " . Item:  id = "
            + output.getId()
            + ", bounds = "
            + output.getBounds()
            + ". Host: id = "
            + host.getId()
            + ", originalBounds = "
            + host.getOriginalBounds()
            + ", bounds = "
            + host.getBounds();
  }

  static void recursivelyNotifyVisibleBoundsChanged(
      final IncrementalMountExtensionInput input, final long id, final Object content) {
    assertMainThread();
    if (input != null && input.renderUnitWithIdHostsRenderTrees(id)) {
      log("RecursivelyNotify [RenderUnit=" + id + "]");
      RenderCoreSystrace.beginSection("IncrementalMountExtension.recursivelyNotify");
      RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged(content);
      RenderCoreSystrace.endSection();
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
      acquireMountReferenceEnsureHostIsMounted(
          localVisibleRect, extensionState, incrementalMountOutput, isMounting);
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

        if (extensionState.ownsReference(id)) {
          extensionState.releaseMountReference(id, true);
          if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
            itemsUnmounted++;
          }
        }

        state.mPreviousBottomsIndex++;
      }

      while (state.mPreviousBottomsIndex > 0
          && localVisibleRect.top
              <= byBottomBounds.get(state.mPreviousBottomsIndex - 1).getBounds().bottom) {

        final IncrementalMountOutput node = byBottomBounds.get(state.mPreviousBottomsIndex - 1);
        final long id = node.getId();

        // Item should still be in the view port.
        if (localVisibleRect.bottom
            >= byBottomBounds.get(state.mPreviousBottomsIndex - 1).getBounds().top) {
          if (!extensionState.ownsReference(id)) {
            acquireMountReferenceEnsureHostIsMounted(localVisibleRect, extensionState, node, true);
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
    final int height = root != null ? root.getHeight() : 0;
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
            acquireMountReferenceEnsureHostIsMounted(localVisibleRect, extensionState, node, true);
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

        if (extensionState.ownsReference(id)) {
          extensionState.releaseMountReference(id, true);
          if (IncrementalMountExtensionConfigs.isDebugLoggingEnabled) {
            itemsUnmounted++;
          }
        }

        state.mPreviousTopsIndex--;
      }
    }

    log("Updates: [Items Mounted=" + itemsMounted + ", Items Unmounted=" + itemsUnmounted + "]");

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
    private final Rect mPreviousLocalVisibleRect = new Rect();
    private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();
    private final Set<Long> mItemsShouldNotNotifyVisibleBoundsChangedOnChildren = new HashSet<>();
    private final boolean mDisableForceAcquireHostMount;

    private IncrementalMountExtensionInput mInput;
    private int mPreviousTopsIndex;
    private int mPreviousBottomsIndex;

    private IncrementalMountExtensionState(boolean disableForceAcquireHostMount) {
      mDisableForceAcquireHostMount = disableForceAcquireHostMount;
    }
  }
}
