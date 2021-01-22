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

package com.facebook.rendercore.visibility;

import static com.facebook.rendercore.extensions.RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged;
import static com.facebook.rendercore.visibility.VisibilityExtensionConfigs.DEBUG_TAG;
import static com.facebook.rendercore.visibility.VisibilityUtils.log;

import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.Function;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisibilityMountExtension<Input extends VisibilityExtensionInput>
    extends MountExtension<Input, VisibilityMountExtension.VisibilityMountExtensionState> {

  private static final VisibilityMountExtension sInstance = new VisibilityMountExtension();
  private static final boolean IS_JELLYBEAN_OR_HIGHER =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
  private static final Rect sTempRect = new Rect();

  private VisibilityMountExtension() {}

  public static VisibilityMountExtension getInstance() {
    return sInstance;
  }

  @Override
  public VisibilityMountExtensionState createState() {
    return new VisibilityMountExtensionState();
  }

  @Override
  public void beforeMount(
      ExtensionState<VisibilityMountExtensionState> extensionState,
      Input input,
      @Nullable Rect localVisibleRect) {

    log("beforeMount");
    RenderCoreSystrace.beginSection("VisibilityExtension.beforeMount");

    final VisibilityMountExtensionState state = extensionState.getState();

    state.mVisibilityOutputs = input.getVisibilityOutputs();
    state.mRenderUnitIdsWhichHostRenderTrees = input.getRenderUnitIdsWhichHostRenderTrees();
    state.mIncrementalVisibilityEnabled = input.isIncrementalVisibilityEnabled();
    state.mVisibilityModuleInput = input.getVisibilityModuleInput();
    state.mPreviousLocalVisibleRect.setEmpty();
    state.mCurrentLocalVisibleRect = localVisibleRect;

    RenderCoreSystrace.endSection();
  }

  @Override
  public void afterMount(ExtensionState<VisibilityMountExtensionState> extensionState) {

    log("afterMount");
    RenderCoreSystrace.beginSection("VisibilityExtension.afterMount");

    final boolean processVisibilityOutputs = !hasTransientState(extensionState);

    if (processVisibilityOutputs) {
      final VisibilityMountExtensionState state = extensionState.getState();
      processVisibilityOutputs(extensionState, state.mCurrentLocalVisibleRect, true);
    }

    RenderCoreSystrace.endSection();
  }

  @Override
  public void onVisibleBoundsChanged(
      ExtensionState<VisibilityMountExtensionState> extensionState,
      @Nullable Rect localVisibleRect) {
    final boolean processVisibilityOutputs = !hasTransientState(extensionState);

    log("onVisibleBoundsChanged [hasTransientState=" + hasTransientState(extensionState) + "]");
    RenderCoreSystrace.beginSection("VisibilityExtension.onVisibleBoundsChanged");

    if (processVisibilityOutputs) {
      processVisibilityOutputs(extensionState, localVisibleRect, false);
    }

    RenderCoreSystrace.endSection();
  }

  @Override
  public void onUnbind(ExtensionState<VisibilityMountExtensionState> extensionState) {
    clearVisibilityItems(extensionState);
  }

  @Override
  public void onUnmount(ExtensionState<VisibilityMountExtensionState> extensionState) {
    final VisibilityMountExtensionState state = extensionState.getState();
    state.mPreviousLocalVisibleRect.setEmpty();
  }

  @VisibleForTesting
  public static Map<String, VisibilityItem> getVisibilityIdToItemMap(
      ExtensionState<VisibilityMountExtensionState> extensionState) {
    final VisibilityMountExtensionState state = extensionState.getState();
    return state.mVisibilityIdToItemMap;
  }

  @UiThread
  public static void clearVisibilityItems(
      final ExtensionState<VisibilityMountExtensionState> extensionState) {
    final VisibilityMountExtensionState state = extensionState.getState();
    if (state.mVisibilityModule != null) {
      clearVisibilityItemsIncremental(state);
    } else {
      clearVisibilityItemsNonincremental(state);
    }
    state.mPreviousLocalVisibleRect.setEmpty();
  }

  /** @deprecated Only used for Litho's integration. Marked for removal. */
  @Deprecated
  public static void setRootHost(
      ExtensionState<VisibilityMountExtensionState> extensionState, Host root) {
    final VisibilityMountExtensionState state = extensionState.getState();
    state.mRootHost = root;
  }

  public static void notifyOnUnbind(ExtensionState<VisibilityMountExtensionState> extensionState) {
    clearVisibilityItems(extensionState);
  }

  @UiThread
  private static void processVisibilityOutputs(
      ExtensionState<VisibilityMountExtensionState> extensionState,
      @Nullable Rect localVisibleRect,
      boolean isDirty) {
    final VisibilityMountExtensionState state = extensionState.getState();
    try {

      log("processVisibilityOutputs");
      RenderCoreSystrace.beginSection("VisibilityExtension.processVisibilityOutputs");

      if (state.mIncrementalVisibilityEnabled) {
        if (state.mVisibilityModule == null) {
          Host host = getRootHost(extensionState);
          if (host == null) {
            return;
          }

          state.mVisibilityModule = new VisibilityModule(host);
        }

        state.mVisibilityModule.processVisibilityOutputs(
            isDirty,
            state.mVisibilityModuleInput,
            localVisibleRect,
            state.mPreviousLocalVisibleRect);
      } else {
        processVisibilityOutputsNonInc(extensionState, localVisibleRect, isDirty);
      }

    } finally {
      RenderCoreSystrace.endSection();
    }

    if (localVisibleRect != null) {
      state.mPreviousLocalVisibleRect.set(localVisibleRect);
    }
  }

  @UiThread
  private static void processVisibilityOutputsNonInc(
      final ExtensionState<VisibilityMountExtensionState> extensionState,
      @Nullable Rect localVisibleRect,
      boolean isDirty) {
    final Rect previousVisibleRect = extensionState.getState().mPreviousLocalVisibleRect;
    if (localVisibleRect == null || (!isDirty && previousVisibleRect.equals(localVisibleRect))) {
      log(
          "Skip Processing: "
              + "[isDirty="
              + isDirty
              + ", previousVisibleRect="
              + previousVisibleRect
              + "]");

      return;
    }

    final VisibilityMountExtensionState state = extensionState.getState();
    final int size = state.mVisibilityOutputs.size();

    log("Visibility Outputs to process: " + size);

    for (int j = 0; j < size; j++) {
      final VisibilityOutput visibilityOutput = state.mVisibilityOutputs.get(j);
      final String componentName = visibilityOutput.getKey();

      log("Processing Visibility for: " + componentName);
      RenderCoreSystrace.beginSection("visibilityHandlers:" + componentName);

      final Rect visibilityOutputBounds = visibilityOutput.getBounds();
      final boolean boundsIntersect =
          sTempRect.setIntersect(visibilityOutputBounds, localVisibleRect);
      final boolean isFullyVisible = boundsIntersect && sTempRect.equals(visibilityOutputBounds);
      final String visibilityOutputId = visibilityOutput.getId();
      VisibilityItem visibilityItem = state.mVisibilityIdToItemMap.get(visibilityOutputId);

      final boolean wasFullyVisible;
      if (visibilityItem != null) {
        wasFullyVisible = visibilityItem.wasFullyVisible();
        visibilityItem.setWasFullyVisible(isFullyVisible);
      } else {
        wasFullyVisible = false;
      }

      if (isFullyVisible
          && wasFullyVisible
          && VisibilityExtensionConfigs.skipVisChecksForFullyVisible) {
        // VisibilityOutput is still fully visible, no new events to dispatch, skip to next
        RenderCoreSystrace.endSection();

        visibilityItem.setDoNotClearInThisPass(isDirty);
        continue;
      }

      final Function<Void> visibleHandler = visibilityOutput.getVisibleEventHandler();
      final Function<Void> focusedHandler = visibilityOutput.getFocusedEventHandler();
      final Function<Void> unfocusedHandler = visibilityOutput.getUnfocusedEventHandler();
      final Function<Void> fullImpressionHandler = visibilityOutput.getFullImpressionEventHandler();
      final Function<Void> invisibleHandler = visibilityOutput.getInvisibleEventHandler();
      final Function<Void> visibilityChangedHandler =
          visibilityOutput.getVisibilityChangedEventHandler();

      final boolean isCurrentlyVisible =
          boundsIntersect && isInVisibleRange(visibilityOutput, visibilityOutputBounds, sTempRect);

      if (visibilityItem != null) {

        // If we did a relayout due to e.g. a state update then the handlers will have changed,
        // so we should keep them up to date.
        visibilityItem.setUnfocusedHandler(unfocusedHandler);
        visibilityItem.setInvisibleHandler(invisibleHandler);

        if (!isCurrentlyVisible) {
          // Either the component is invisible now, but used to be visible, or the key on the
          // component has changed so we should generate new visibility events for the new
          // component.
          if (visibilityItem.getInvisibleHandler() != null) {
            VisibilityUtils.dispatchOnInvisible(visibilityItem.getInvisibleHandler());
          }

          if (visibilityChangedHandler != null) {
            VisibilityUtils.dispatchOnVisibilityChanged(visibilityChangedHandler, 0, 0, 0f, 0f);
          }

          if (visibilityItem.isInFocusedRange()) {
            visibilityItem.setFocusedRange(false);
            if (visibilityItem.getUnfocusedHandler() != null) {
              VisibilityUtils.dispatchOnUnfocused(visibilityItem.getUnfocusedHandler());
            }
          }

          state.mVisibilityIdToItemMap.remove(visibilityOutputId);
          visibilityItem = null;
        } else {
          // Processed, do not clear.
          visibilityItem.setDoNotClearInThisPass(isDirty);
        }
      }

      if (isCurrentlyVisible) {
        // The component is visible now, but used to be outside the viewport.
        if (visibilityItem == null) {
          final String globalKey = visibilityOutput.getId();
          visibilityItem =
              new VisibilityItem(
                  globalKey, invisibleHandler, unfocusedHandler, visibilityChangedHandler);
          visibilityItem.setDoNotClearInThisPass(isDirty);
          visibilityItem.setWasFullyVisible(isFullyVisible);
          state.mVisibilityIdToItemMap.put(visibilityOutputId, visibilityItem);

          if (visibleHandler != null) {
            VisibilityUtils.dispatchOnVisible(visibleHandler);
          }
        }

        // Check if the component has entered or exited the focused range.
        if (focusedHandler != null || unfocusedHandler != null) {
          if (isInFocusedRange(extensionState, visibilityOutputBounds, sTempRect)) {
            if (!visibilityItem.isInFocusedRange()) {
              visibilityItem.setFocusedRange(true);
              if (focusedHandler != null) {
                VisibilityUtils.dispatchOnFocused(focusedHandler);
              }
            }
          } else {
            if (visibilityItem.isInFocusedRange()) {
              visibilityItem.setFocusedRange(false);
              if (unfocusedHandler != null) {
                VisibilityUtils.dispatchOnUnfocused(unfocusedHandler);
              }
            }
          }
        }
        // If the component has not entered the full impression range yet, make sure to update the
        // information about the visible edges.
        if (fullImpressionHandler != null && !visibilityItem.isInFullImpressionRange()) {
          visibilityItem.setVisibleEdges(visibilityOutputBounds, sTempRect);

          if (visibilityItem.isInFullImpressionRange()) {
            VisibilityUtils.dispatchOnFullImpression(fullImpressionHandler);
          }
        }

        if (visibilityChangedHandler != null) {
          final int visibleWidth = sTempRect.right - sTempRect.left;
          final int visibleHeight = sTempRect.bottom - sTempRect.top;
          VisibilityUtils.dispatchOnVisibilityChanged(
              visibilityChangedHandler,
              visibleWidth,
              visibleHeight,
              100f * visibleWidth / visibilityOutputBounds.width(),
              100f * visibleHeight / visibilityOutputBounds.height());
        }
      }

      RenderCoreSystrace.endSection();
    }

    for (long id : state.mRenderUnitIdsWhichHostRenderTrees) {
      log("RecursivelyNotify:RenderUnit[id=" + id + "]");
      recursivelyNotifyVisibleBoundsChanged(extensionState.getMountDelegate().getContentById(id));
    }

    if (isDirty) {
      clearVisibilityItems(extensionState);
    }
  }

  private static boolean isInVisibleRange(
      final VisibilityOutput visibilityOutput, final Rect bounds, final Rect visibleBounds) {
    float heightRatio = visibilityOutput.getVisibleHeightRatio();
    float widthRatio = visibilityOutput.getVisibleWidthRatio();

    if (heightRatio == 0 && widthRatio == 0) {
      return true;
    }

    return isInRatioRange(heightRatio, bounds.height(), visibleBounds.height())
        && isInRatioRange(widthRatio, bounds.width(), visibleBounds.width());
  }

  /** Returns true if the component is in the focused visible range. */
  private static boolean isInFocusedRange(
      ExtensionState<VisibilityMountExtensionState> extensionState,
      Rect componentBounds,
      Rect componentVisibleBounds) {
    final Host host = getRootHost(extensionState);
    if (host == null) {
      return false;
    }
    final View parent = (View) host.getParent();
    if (parent == null) {
      return false;
    }

    final int halfViewportArea = parent.getWidth() * parent.getHeight() / 2;
    final int totalComponentArea = computeRectArea(componentBounds);
    final int visibleComponentArea = computeRectArea(componentVisibleBounds);

    // The component has entered the focused range either if it is larger than half of the viewport
    // and it occupies at least half of the viewport or if it is smaller than half of the viewport
    // and it is fully visible.
    return (totalComponentArea >= halfViewportArea)
        ? (visibleComponentArea >= halfViewportArea)
        : componentBounds.equals(componentVisibleBounds);
  }

  @UiThread
  private static void clearVisibilityItemsIncremental(final VisibilityMountExtensionState state) {
    RenderCoreSystrace.beginSection("VisibilityExtension.clearIncrementalItems");

    if (state.mVisibilityModule != null) {
      state.mVisibilityModule.clearIncrementalItems();
    }

    RenderCoreSystrace.endSection();
  }

  @UiThread
  private static void clearVisibilityItemsNonincremental(
      final VisibilityMountExtensionState state) {
    RenderCoreSystrace.beginSection("VisibilityExtension.clearIncrementalItems");

    final List<String> toClear = new ArrayList<>();

    for (Map.Entry<String, VisibilityItem> entry : state.mVisibilityIdToItemMap.entrySet()) {
      final VisibilityItem visibilityItem = entry.getValue();
      if (visibilityItem.doNotClearInThisPass()) {
        // This visibility item has already been accounted for in this pass, so ignore it.
        visibilityItem.setDoNotClearInThisPass(false);
      } else {
        toClear.add(entry.getKey());
      }
    }

    for (int i = 0, size = toClear.size(); i < size; i++) {
      final String key = toClear.get(i);
      final VisibilityItem visibilityItem = state.mVisibilityIdToItemMap.get(key);

      if (visibilityItem != null) {
        final Function<Void> invisibleHandler = visibilityItem.getInvisibleHandler();
        final Function<Void> unfocusedHandler = visibilityItem.getUnfocusedHandler();
        final Function<Void> visibilityChangedHandler =
            visibilityItem.getVisibilityChangedHandler();

        if (invisibleHandler != null) {
          VisibilityUtils.dispatchOnInvisible(invisibleHandler);
        }

        if (visibilityItem.isInFocusedRange()) {
          visibilityItem.setFocusedRange(false);
          if (unfocusedHandler != null) {
            VisibilityUtils.dispatchOnUnfocused(unfocusedHandler);
          }
        }

        if (visibilityChangedHandler != null) {
          VisibilityUtils.dispatchOnVisibilityChanged(visibilityChangedHandler, 0, 0, 0f, 0f);
        }

        visibilityItem.setWasFullyVisible(false);
      }

      state.mVisibilityIdToItemMap.remove(key);
    }

    RenderCoreSystrace.endSection();
  }

  private static boolean hasTransientState(ExtensionState<VisibilityMountExtensionState> state) {
    final Host host = getRootHost(state);
    return IS_JELLYBEAN_OR_HIGHER && (host != null && host.hasTransientState());
  }

  private static @Nullable Host getRootHost(
      ExtensionState<VisibilityMountExtensionState> extensionState) {
    final VisibilityMountExtensionState state = extensionState.getState();

    if (state.mRootHost == null) {
      return extensionState.getRootHost();
    } else {
      return state.mRootHost;
    }
  }

  private static boolean isInRatioRange(float ratio, int length, int visibleLength) {
    return visibleLength >= ratio * length;
  }

  private static int computeRectArea(Rect rect) {
    return rect.isEmpty() ? 0 : (rect.width() * rect.height());
  }

  static class VisibilityMountExtensionState {

    // Holds a list with information about the components linked to the VisibilityOutputs that are
    // stored in LayoutState. An item is inserted in this map if its corresponding component is
    // visible. When the component exits the viewport, the item associated with it is removed from
    // the map.
    private final Map<String, VisibilityItem> mVisibilityIdToItemMap = new HashMap<>();
    private final Rect mPreviousLocalVisibleRect = new Rect();

    private boolean mIncrementalVisibilityEnabled;
    private List<VisibilityOutput> mVisibilityOutputs = Collections.emptyList();
    private Set<Long> mRenderUnitIdsWhichHostRenderTrees = Collections.emptySet();
    private @Nullable VisibilityModule mVisibilityModule;
    private @Nullable VisibilityModuleInput mVisibilityModuleInput;
    private @Nullable Rect mCurrentLocalVisibleRect;

    /** @deprecated Only used for Litho's integration. Marked for removal. */
    @Deprecated private @Nullable Host mRootHost;

    private VisibilityMountExtensionState() {}
  }
}
