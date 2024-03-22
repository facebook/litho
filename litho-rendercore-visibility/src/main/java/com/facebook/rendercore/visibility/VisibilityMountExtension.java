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

package com.facebook.rendercore.visibility;

import static com.facebook.rendercore.debug.DebugEventAttribute.Bounds;
import static com.facebook.rendercore.debug.DebugEventAttribute.Key;
import static com.facebook.rendercore.debug.DebugEventAttribute.Name;
import static com.facebook.rendercore.debug.DebugEventAttribute.RenderUnitId;
import static com.facebook.rendercore.visibility.VisibilityExtensionConfigs.DEBUG_TAG;

import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.Function;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.debug.DebugEvent;
import com.facebook.rendercore.debug.DebugEventDispatcher;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.VisibleBoundsCallbacks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisibilityMountExtension<Input extends VisibilityExtensionInput>
    extends MountExtension<Input, VisibilityMountExtension.VisibilityMountExtensionState>
    implements VisibleBoundsCallbacks<VisibilityMountExtension.VisibilityMountExtensionState> {

  private static final VisibilityMountExtension sInstance = new VisibilityMountExtension();
  private static final boolean IS_JELLYBEAN_OR_HIGHER =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;

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

    final boolean isTracing = RenderCoreSystrace.isTracing();
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(DEBUG_TAG, "beforeMount");
    }
    if (isTracing) {
      RenderCoreSystrace.beginSection("VisibilityExtension.beforeMount");
    }

    final VisibilityMountExtensionState state = extensionState.getState();

    state.mVisibilityOutputs = input.getVisibilityOutputs();
    state.mRenderUnitIdsWhichHostRenderTrees = input.getRenderUnitIdsWhichHostRenderTrees();
    state.mPreviousLocalVisibleRect.setEmpty();
    state.mCurrentLocalVisibleRect = localVisibleRect;
    state.mVisibilityBoundsTransformer = input.getVisibilityBoundsTransformer();
    state.mInput = input;

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  @Override
  public void afterMount(ExtensionState<VisibilityMountExtensionState> extensionState) {

    final boolean isTracing = RenderCoreSystrace.isTracing();
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(DEBUG_TAG, "afterMount");
    }
    if (isTracing) {
      RenderCoreSystrace.beginSection("VisibilityExtension.afterMount");
    }

    final boolean processVisibilityOutputs = shouldProcessVisibilityOutputs(extensionState);

    if (processVisibilityOutputs) {
      final VisibilityMountExtensionState state = extensionState.getState();
      processVisibilityOutputs(extensionState, state.mCurrentLocalVisibleRect, true);
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  @Override
  public void onVisibleBoundsChanged(
      ExtensionState<VisibilityMountExtensionState> extensionState,
      @Nullable Rect localVisibleRect) {
    final boolean processVisibilityOutputs = shouldProcessVisibilityOutputs(extensionState);
    final boolean isTracing = RenderCoreSystrace.isTracing();

    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          DEBUG_TAG,
          "onVisibleBoundsChanged [processVisibilityOutputs=" + processVisibilityOutputs + "]");
    }
    if (isTracing) {
      RenderCoreSystrace.beginSection("VisibilityExtension.onVisibleBoundsChanged");
    }

    if (processVisibilityOutputs) {
      processVisibilityOutputs(extensionState, localVisibleRect, false);
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  @Override
  public void onUnbind(ExtensionState<VisibilityMountExtensionState> extensionState) {
    clearVisibilityItems(extensionState);
  }

  @Override
  public void onUnmount(ExtensionState<VisibilityMountExtensionState> extensionState) {
    final VisibilityMountExtensionState state = extensionState.getState();
    state.mPreviousLocalVisibleRect.setEmpty();
    state.mInput = null;
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
    clearVisibilityItemsNonincremental(extensionState.getRenderStateId(), state);
    state.mPreviousLocalVisibleRect.setEmpty();
  }

  /**
   * @deprecated Only used for Litho's integration. Marked for removal.
   */
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
  public static void processVisibilityOutputs(
      ExtensionState<VisibilityMountExtensionState> extensionState,
      @Nullable Rect localVisibleRect,
      boolean isDirty) {
    final VisibilityMountExtensionState state = extensionState.getState();
    final boolean isTracing = RenderCoreSystrace.isTracing();
    try {

      if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
        Log.d(DEBUG_TAG, "processVisibilityOutputs");
      }
      if (isTracing) {
        RenderCoreSystrace.beginSection("VisibilityExtension.processVisibilityOutputs");
      }

      processVisibilityOutputsNonInc(extensionState, localVisibleRect, isDirty);

    } finally {
      if (isTracing) {
        RenderCoreSystrace.endSection();
      }
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
      if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
        Log.d(
            DEBUG_TAG,
            "Skip Processing: "
                + "[isDirty="
                + isDirty
                + ", previousVisibleRect="
                + previousVisibleRect
                + "]");
      }

      return;
    }

    final VisibilityMountExtensionState state = extensionState.getState();
    final int size = state.mVisibilityOutputs.size();

    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(DEBUG_TAG, "Visibility Outputs to process: " + size);
    }

    if (!extensionState.getState().mVisibilityOutputs.isEmpty()) {

      final @Nullable VisibilityBoundsTransformer transformer =
          extensionState.getState().mVisibilityBoundsTransformer;
      @Nullable Rect transformedLocalVisibleRect = null;
      if (transformer != null) {
        transformedLocalVisibleRect =
            transformer.getTransformedLocalVisibleRect(extensionState.getRootHost());
      }

      final boolean isTracing = RenderCoreSystrace.isTracing();

      final Rect intersection = new Rect();
      for (int j = 0; j < size; j++) {
        final VisibilityOutput visibilityOutput = state.mVisibilityOutputs.get(j);
        final String componentName = visibilityOutput.getKey();

        if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
          Log.d(DEBUG_TAG, "Processing Visibility for: " + componentName);
        }
        if (isTracing) {
          RenderCoreSystrace.beginSection("visibilityHandlers:" + componentName);
        }

        final Rect visibilityOutputBounds = visibilityOutput.getBounds();

        final boolean shouldUseTransformedVisibleRect =
            transformer != null && transformer.shouldUseTransformedVisibleRect(visibilityOutput);
        final boolean boundsIntersect =
            intersection.setIntersect(
                visibilityOutputBounds,
                shouldUseTransformedVisibleRect && transformedLocalVisibleRect != null
                    ? transformedLocalVisibleRect
                    : localVisibleRect);
        final boolean isFullyVisible =
            boundsIntersect && intersection.equals(visibilityOutputBounds);
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
          if (isTracing) {
            RenderCoreSystrace.endSection();
          }

          visibilityItem.setDoNotClearInThisPass(isDirty);
          continue;
        }

        final Function<Void> visibleHandler = visibilityOutput.getVisibleEventHandler();
        final Function<Void> focusedHandler = visibilityOutput.getFocusedEventHandler();
        final Function<Void> unfocusedHandler = visibilityOutput.getUnfocusedEventHandler();
        final Function<Void> fullImpressionHandler =
            visibilityOutput.getFullImpressionEventHandler();
        final Function<Void> invisibleHandler = visibilityOutput.getInvisibleEventHandler();
        final Function<Void> visibilityChangedHandler =
            visibilityOutput.getVisibilityChangedEventHandler();

        final boolean isCurrentlyVisible =
            boundsIntersect
                && isInVisibleRange(visibilityOutput, visibilityOutputBounds, intersection);

        if (visibilityItem != null) {

          // If we did a relayout due to e.g. a state update then the handlers will have changed,
          // so we should keep them up to date.
          visibilityItem.setUnfocusedHandler(unfocusedHandler);
          visibilityItem.setInvisibleHandler(invisibleHandler);

          if (!isCurrentlyVisible) {
            // Either the component is invisible now, but used to be visible, or the key on the
            // component has changed so we should generate new visibility events for the new
            // component.
            maybeDispatchOnInvisible(extensionState.getRenderStateId(), visibilityItem);

            if (visibilityChangedHandler != null) {
              VisibilityUtils.dispatchOnVisibilityChanged(
                  visibilityChangedHandler, 0, 0, 0, 0, 0, 0, 0f, 0f);
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
                    globalKey,
                    invisibleHandler,
                    unfocusedHandler,
                    visibilityChangedHandler,
                    visibilityOutput.getKey(),
                    visibilityOutput.mRenderUnitId,
                    visibilityOutput.getBounds());

            visibilityItem.setDoNotClearInThisPass(isDirty);
            visibilityItem.setWasFullyVisible(isFullyVisible);
            state.mVisibilityIdToItemMap.put(visibilityOutputId, visibilityItem);

            if (visibleHandler != null) {
              final Object content =
                  visibilityOutput.hasMountableContent
                      ? getContentById(extensionState, visibilityOutput.mRenderUnitId)
                      : null;

              @Nullable
              Integer traceIdentifier =
                  DebugEventDispatcher.generateTraceIdentifier(DebugEvent.RenderUnitOnVisible);
              if (traceIdentifier != null) {
                DebugEventDispatcher.beginTrace(
                    traceIdentifier,
                    DebugEvent.RenderUnitOnVisible,
                    String.valueOf(extensionState.getRenderStateId()),
                    createVisibilityDebugAttributes(visibilityItem));
              }

              VisibilityUtils.dispatchOnVisible(visibleHandler, content);

              if (traceIdentifier != null) {
                DebugEventDispatcher.endTrace(traceIdentifier);
              }
            }
          }

          // Check if the component has entered or exited the focused range.
          if (focusedHandler != null || unfocusedHandler != null) {
            if (isInFocusedRange(
                extensionState,
                visibilityOutputBounds,
                intersection,
                shouldUseTransformedVisibleRect)) {
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
            visibilityItem.setVisibleEdges(visibilityOutputBounds, intersection);

            if (visibilityItem.isInFullImpressionRange()) {
              VisibilityUtils.dispatchOnFullImpression(fullImpressionHandler);
            }
          }

          if (visibilityChangedHandler != null) {
            final int visibleWidth = getVisibleWidth(intersection);
            final int visibleHeight = getVisibleHeight(intersection);
            int rootHostViewWidth = getRootHostViewWidth(extensionState);
            int rootHostViewHeight = getRootHostViewHeight(extensionState);
            if (shouldUseTransformedVisibleRect && transformer != null) {
              final Host host = getRootHost(extensionState);
              if (host != null && (host.getParent() instanceof View)) {
                final View parent = (View) host.getParent();
                rootHostViewWidth = transformer.getViewportWidth(parent);
                rootHostViewHeight = transformer.getViewportHeight(parent);
              }
            }

            VisibilityUtils.dispatchOnVisibilityChanged(
                visibilityChangedHandler,
                getVisibleTop(visibilityOutputBounds, intersection),
                getVisibleLeft(visibilityOutputBounds, intersection),
                visibleWidth,
                visibleHeight,
                rootHostViewWidth,
                rootHostViewHeight,
                100f * visibleWidth / visibilityOutputBounds.width(),
                100f * visibleHeight / visibilityOutputBounds.height());
          }
        }

        if (isTracing) {
          RenderCoreSystrace.endSection();
        }
      }
    }

    final MountDelegate mountDelegate = extensionState.getMountDelegate();
    for (long id : state.mRenderUnitIdsWhichHostRenderTrees) {
      if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
        Log.d(DEBUG_TAG, "RecursivelyNotify:RenderUnit[id=" + id + "]");
      }
      mountDelegate.notifyVisibleBoundsChangedForItem(mountDelegate.getContentById(id));
    }

    if (isDirty) {
      clearVisibilityItems(extensionState);
    }
  }

  @NonNull
  private static Map<String, Object> createVisibilityDebugAttributes(
      VisibilityItem visibilityItem) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(RenderUnitId, visibilityItem.getRenderUnitId());
    attributes.put(Name, visibilityItem.getComponentName());
    attributes.put(Bounds, visibilityItem.getBounds());
    attributes.put(Key, visibilityItem.getKey());
    return attributes;
  }

  /**
   * This method will dispatch an {@code onInvisibleEvent} to the given {@link VisibilityItem} only
   * if it returns a non-null invisible handler from {@link VisibilityItem#getInvisibleHandler()}.
   */
  private static void maybeDispatchOnInvisible(int renderStateId, VisibilityItem visibilityItem) {
    if (visibilityItem.getInvisibleHandler() == null) {
      return;
    }

    @Nullable
    Integer traceIdentifier =
        DebugEventDispatcher.generateTraceIdentifier(DebugEvent.RenderUnitOnInvisible);
    if (traceIdentifier != null) {
      DebugEventDispatcher.beginTrace(
          traceIdentifier,
          DebugEvent.RenderUnitOnInvisible,
          String.valueOf(renderStateId),
          createVisibilityDebugAttributes(visibilityItem));
    }

    VisibilityUtils.dispatchOnInvisible(visibilityItem.getInvisibleHandler());

    if (traceIdentifier != null) {
      DebugEventDispatcher.endTrace(traceIdentifier);
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
      Rect componentVisibleBounds,
      boolean shouldUseTransformedVisibleRect) {
    final Host host = getRootHost(extensionState);
    if (host == null) {
      return false;
    }
    if (!(host.getParent() instanceof View)) {
      return false;
    }
    final View parent = (View) host.getParent();
    if (parent == null) {
      return false;
    }

    final @Nullable VisibilityBoundsTransformer transformer =
        extensionState.getState().mVisibilityBoundsTransformer;
    int halfViewportArea = parent.getWidth() * parent.getHeight() / 2;
    if (shouldUseTransformedVisibleRect && transformer != null) {
      halfViewportArea = transformer.getViewportArea(parent) / 2;
    }

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
  private static void clearVisibilityItemsNonincremental(
      final int renderStateId, final VisibilityMountExtensionState state) {
    final boolean isTracing = RenderCoreSystrace.isTracing();
    if (isTracing) {
      RenderCoreSystrace.beginSection("VisibilityExtension.clearIncrementalItems");
    }

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
        final Function<Void> unfocusedHandler = visibilityItem.getUnfocusedHandler();
        final Function<Void> visibilityChangedHandler =
            visibilityItem.getVisibilityChangedHandler();

        maybeDispatchOnInvisible(renderStateId, visibilityItem);

        if (visibilityItem.isInFocusedRange()) {
          visibilityItem.setFocusedRange(false);
          if (unfocusedHandler != null) {
            VisibilityUtils.dispatchOnUnfocused(unfocusedHandler);
          }
        }

        if (visibilityChangedHandler != null) {
          VisibilityUtils.dispatchOnVisibilityChanged(
              visibilityChangedHandler, 0, 0, 0, 0, 0, 0, 0f, 0f);
        }

        visibilityItem.setWasFullyVisible(false);
      }

      state.mVisibilityIdToItemMap.remove(key);
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  public static boolean shouldProcessVisibilityOutputs(
      ExtensionState<VisibilityMountExtensionState> extensionState) {
    final VisibilityMountExtensionState state = extensionState.getState();

    if (state.mInput != null && !state.mInput.isProcessingVisibilityOutputsEnabled()) {
      return false;
    }

    final Host host = getRootHost(extensionState);
    return !(IS_JELLYBEAN_OR_HIGHER && host != null && host.hasTransientState());
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

  public static class VisibilityMountExtensionState {

    // Holds a list with information about the components linked to the VisibilityOutputs that are
    // stored in LayoutState. An item is inserted in this map if its corresponding component is
    // visible. When the component exits the viewport, the item associated with it is removed from
    // the map.
    private final Map<String, VisibilityItem> mVisibilityIdToItemMap = new HashMap<>();
    private final Rect mPreviousLocalVisibleRect = new Rect();

    private List<VisibilityOutput> mVisibilityOutputs = Collections.emptyList();
    private Set<Long> mRenderUnitIdsWhichHostRenderTrees = Collections.emptySet();
    private @Nullable Rect mCurrentLocalVisibleRect;
    private @Nullable VisibilityBoundsTransformer mVisibilityBoundsTransformer;
    private @Nullable VisibilityExtensionInput mInput;

    /**
     * @deprecated Only used for Litho's integration. Marked for removal.
     */
    @Deprecated private @Nullable Host mRootHost;

    private VisibilityMountExtensionState() {}
  }

  public static boolean isVisible(VisibilityMountExtensionState state, String key) {
    return state.mVisibilityIdToItemMap.containsKey(key);
  }

  public static int getVisibleTop(Rect itemBounds, Rect itemIntersection) {
    return itemIntersection.top - itemBounds.top;
  }

  public static int getVisibleLeft(Rect itemBounds, Rect itemIntersection) {
    return itemIntersection.left - itemBounds.left;
  }

  public static int getVisibleWidth(Rect itemIntersection) {
    return itemIntersection.width();
  }

  public static int getVisibleHeight(Rect itemIntersection) {
    return itemIntersection.height();
  }

  public static int getRootHostViewWidth(
      ExtensionState<VisibilityMountExtensionState> extensionState) {
    final Host host = getRootHost(extensionState);
    if (host == null) {
      return 0;
    }
    if (!(host.getParent() instanceof View)) {
      return 0;
    }
    final View parent = (View) host.getParent();
    if (parent == null) {
      return 0;
    }
    return parent.getWidth();
  }

  public static int getRootHostViewHeight(
      ExtensionState<VisibilityMountExtensionState> extensionState) {
    final Host host = getRootHost(extensionState);
    if (host == null) {
      return 0;
    }
    if (!(host.getParent() instanceof View)) {
      return 0;
    }
    final View parent = (View) host.getParent();
    if (parent == null) {
      return 0;
    }
    return parent.getHeight();
  }
}
