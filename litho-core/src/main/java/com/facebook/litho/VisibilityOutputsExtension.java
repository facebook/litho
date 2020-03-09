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

import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.config.ComponentsConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class VisibilityOutputsExtension
    implements HostListenerExtension<VisibilityOutputsExtension.VisibilityOutputsExtensionInput> {

  private final Host mHost;
  // Holds a list with information about the components linked to the VisibilityOutputs that are
  // stored in LayoutState. An item is inserted in this map if its corresponding component is
  // visible. When the component exits the viewport, the item associated with it is removed from the
  // map.
  private final Map<String, VisibilityItem> mVisibilityIdToItemMap;
  private @Nullable VisibilityModule mVisibilityModule;
  private final Rect mPreviousLocalVisibleRect = new Rect();
  private static final Rect sTempRect = new Rect();

  private boolean mIncrementalVisibilityEnabled;
  private List<VisibilityOutput> mVisibilityOutputs;
  private VisibilityModuleInput mVisibilityModuleInput;

  VisibilityOutputsExtension(Host host) {
    mHost = host;
    mVisibilityIdToItemMap = new HashMap<>();
  }

  public interface VisibilityOutputsExtensionInput {
    List<VisibilityOutput> getVisibilityOutputs();

    boolean isIncrementalVisibilityEnabled();

    VisibilityModuleInput getVisibilityModuleInput();
  }

  private void processVisibilityOutputs(
      @Nullable Rect localVisibleRect, @Nullable PerfEvent mountPerfEvent, boolean isDirty) {
    final boolean isTracing = ComponentsSystrace.isTracing();

    try {
      if (mountPerfEvent != null) {
        mountPerfEvent.markerPoint("VISIBILITY_HANDLERS_START");
      }

      if (isTracing) {
        ComponentsSystrace.beginSection("processVisibilityOutputs");
      }

      if (mIncrementalVisibilityEnabled) {
        if (mVisibilityModule == null) {
          if (mHost == null) {
            return;
          }

          mVisibilityModule = new VisibilityModule(mHost);
        }

        mVisibilityModule.processVisibilityOutputs(
            isDirty, mVisibilityModuleInput, localVisibleRect, mPreviousLocalVisibleRect);
      } else {
        processVisibilityOutputsNonInc(localVisibleRect, isDirty);
      }

    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      if (mountPerfEvent != null) {
        mountPerfEvent.markerPoint("VISIBILITY_HANDLERS_END");
      }
    }

    if (localVisibleRect != null) {
      mPreviousLocalVisibleRect.set(localVisibleRect);
    }
  }

  private void processVisibilityOutputsNonInc(@Nullable Rect localVisibleRect, boolean isDirty) {
    assertMainThread();

    if (localVisibleRect == null) {
      return;
    }

    final boolean isTracing = ComponentsSystrace.isTracing();

    for (int j = 0, size = mVisibilityOutputs.size(); j < size; j++) {
      final VisibilityOutput visibilityOutput = mVisibilityOutputs.get(j);
      if (isTracing) {
        final String componentName =
            visibilityOutput.getComponent() != null
                ? visibilityOutput.getComponent().getSimpleName()
                : "Unknown";
        ComponentsSystrace.beginSection("visibilityHandlers:" + componentName);
      }

      final Rect visibilityOutputBounds = visibilityOutput.getBounds();
      final boolean boundsIntersect =
          sTempRect.setIntersect(visibilityOutputBounds, localVisibleRect);
      final boolean isFullyVisible = boundsIntersect && sTempRect.equals(visibilityOutputBounds);
      final String visibilityOutputId = visibilityOutput.getId();
      VisibilityItem visibilityItem = mVisibilityIdToItemMap.get(visibilityOutputId);

      final boolean wasFullyVisible;
      if (visibilityItem != null) {
        wasFullyVisible = visibilityItem.wasFullyVisible();
        visibilityItem.setWasFullyVisible(isFullyVisible);
      } else {
        wasFullyVisible = false;
      }

      if (isFullyVisible
          && wasFullyVisible
          && ComponentsConfiguration.skipVisChecksForFullyVisible) {
        // VisibilityOutput is still fully visible, no new events to dispatch, skip to next
        if (isTracing) {
          ComponentsSystrace.endSection();
        }

        visibilityItem.setDoNotClearInThisPass(isDirty);
        continue;
      }

      final EventHandler<VisibleEvent> visibleHandler = visibilityOutput.getVisibleEventHandler();
      final EventHandler<FocusedVisibleEvent> focusedHandler =
          visibilityOutput.getFocusedEventHandler();
      final EventHandler<UnfocusedVisibleEvent> unfocusedHandler =
          visibilityOutput.getUnfocusedEventHandler();
      final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler =
          visibilityOutput.getFullImpressionEventHandler();
      final EventHandler<InvisibleEvent> invisibleHandler =
          visibilityOutput.getInvisibleEventHandler();
      final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
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
            EventDispatcherUtils.dispatchOnInvisible(visibilityItem.getInvisibleHandler());
          }

          if (visibilityChangedHandler != null) {
            EventDispatcherUtils.dispatchOnVisibilityChanged(
                visibilityChangedHandler, 0, 0, 0f, 0f);
          }

          if (visibilityItem.isInFocusedRange()) {
            visibilityItem.setFocusedRange(false);
            if (visibilityItem.getUnfocusedHandler() != null) {
              EventDispatcherUtils.dispatchOnUnfocused(visibilityItem.getUnfocusedHandler());
            }
          }

          mVisibilityIdToItemMap.remove(visibilityOutputId);
          visibilityItem = null;
        } else {
          // Processed, do not clear.
          visibilityItem.setDoNotClearInThisPass(isDirty);
        }
      }

      if (isCurrentlyVisible) {
        // The component is visible now, but used to be outside the viewport.
        if (visibilityItem == null) {
          final String globalKey =
              visibilityOutput.getComponent() != null
                  ? visibilityOutput.getComponent().getGlobalKey()
                  : null;
          visibilityItem =
              new VisibilityItem(
                  globalKey, invisibleHandler, unfocusedHandler, visibilityChangedHandler);
          visibilityItem.setDoNotClearInThisPass(isDirty);
          visibilityItem.setWasFullyVisible(isFullyVisible);
          mVisibilityIdToItemMap.put(visibilityOutputId, visibilityItem);

          if (visibleHandler != null) {
            EventDispatcherUtils.dispatchOnVisible(visibleHandler);
          }
        }

        // Check if the component has entered or exited the focused range.
        if (focusedHandler != null || unfocusedHandler != null) {
          if (isInFocusedRange(visibilityOutputBounds, sTempRect)) {
            if (!visibilityItem.isInFocusedRange()) {
              visibilityItem.setFocusedRange(true);
              if (focusedHandler != null) {
                EventDispatcherUtils.dispatchOnFocused(focusedHandler);
              }
            }
          } else {
            if (visibilityItem.isInFocusedRange()) {
              visibilityItem.setFocusedRange(false);
              if (unfocusedHandler != null) {
                EventDispatcherUtils.dispatchOnUnfocused(unfocusedHandler);
              }
            }
          }
        }
        // If the component has not entered the full impression range yet, make sure to update the
        // information about the visible edges.
        if (fullImpressionHandler != null && !visibilityItem.isInFullImpressionRange()) {
          visibilityItem.setVisibleEdges(visibilityOutputBounds, sTempRect);

          if (visibilityItem.isInFullImpressionRange()) {
            EventDispatcherUtils.dispatchOnFullImpression(fullImpressionHandler);
          }
        }

        if (visibilityChangedHandler != null) {
          final int visibleWidth = sTempRect.right - sTempRect.left;
          final int visibleHeight = sTempRect.bottom - sTempRect.top;
          EventDispatcherUtils.dispatchOnVisibilityChanged(
              visibilityChangedHandler,
              visibleWidth,
              visibleHeight,
              100f * visibleWidth / visibilityOutputBounds.width(),
              100f * visibleHeight / visibilityOutputBounds.height());
        }
      }

      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    if (isDirty) {
      clearVisibilityItems();
    }
  }

  private boolean isInVisibleRange(
      VisibilityOutput visibilityOutput, Rect bounds, Rect visibleBounds) {
    float heightRatio = visibilityOutput.getVisibleHeightRatio();
    float widthRatio = visibilityOutput.getVisibleWidthRatio();

    if (heightRatio == 0 && widthRatio == 0) {
      return true;
    }

    return isInRatioRange(heightRatio, bounds.height(), visibleBounds.height())
        && isInRatioRange(widthRatio, bounds.width(), visibleBounds.width());
  }

  /** Returns true if the component is in the focused visible range. */
  private boolean isInFocusedRange(Rect componentBounds, Rect componentVisibleBounds) {
    final View parent = (View) mHost.getParent();
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

  void clearVisibilityItems() {
    if (mVisibilityModule != null) {
      clearVisibilityItemsIncremental();
    } else {
      clearVisibilityItemsNonincremental();
    }
  }

  private void clearVisibilityItemsIncremental() {
    assertMainThread();

    boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("MountState.clearIncrementalItems");
    }

    if (mVisibilityModule != null) {
      mVisibilityModule.clearIncrementalItems();
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  private void clearVisibilityItemsNonincremental() {
    assertMainThread();
    boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("MountState.clearIncrementalItems");
    }

    List<String> toClear = new ArrayList<>();

    for (String key : mVisibilityIdToItemMap.keySet()) {
      final VisibilityItem visibilityItem = mVisibilityIdToItemMap.get(key);
      if (visibilityItem.doNotClearInThisPass()) {
        // This visibility item has already been accounted for in this pass, so ignore it.
        visibilityItem.setDoNotClearInThisPass(false);
      } else {
        toClear.add(key);
      }
    }

    for (int i = 0, size = toClear.size(); i < size; i++) {
      final String key = toClear.get(i);
      final VisibilityItem visibilityItem = mVisibilityIdToItemMap.get(key);

      final EventHandler<InvisibleEvent> invisibleHandler = visibilityItem.getInvisibleHandler();
      final EventHandler<UnfocusedVisibleEvent> unfocusedHandler =
          visibilityItem.getUnfocusedHandler();
      final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
          visibilityItem.getVisibilityChangedHandler();

      if (invisibleHandler != null) {
        EventDispatcherUtils.dispatchOnInvisible(invisibleHandler);
      }

      if (visibilityItem.isInFocusedRange()) {
        visibilityItem.setFocusedRange(false);
        if (unfocusedHandler != null) {
          EventDispatcherUtils.dispatchOnUnfocused(unfocusedHandler);
        }
      }

      if (visibilityChangedHandler != null) {
        EventDispatcherUtils.dispatchOnVisibilityChanged(visibilityChangedHandler, 0, 0, 0f, 0f);
      }

      visibilityItem.setWasFullyVisible(false);

      mVisibilityIdToItemMap.remove(key);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  public void unmountAllItems() {
    mPreviousLocalVisibleRect.setEmpty();
  }

  @Override
  public void onUnmount() {
    mPreviousLocalVisibleRect.setEmpty();
  }

  @Override
  public void beforeMount(VisibilityOutputsExtensionInput input) {
    boolean processVisibilityOutputs = !mHost.isInTransientState();
    if (!processVisibilityOutputs) {
      return;
    }

    mVisibilityOutputs = input.getVisibilityOutputs();
    mIncrementalVisibilityEnabled = input.isIncrementalVisibilityEnabled();
    mVisibilityModuleInput = input.getVisibilityModuleInput();
    mPreviousLocalVisibleRect.setEmpty();
    final Rect localVisibleRect = mHost.getVisibleRect();
    processVisibilityOutputs(localVisibleRect, null, true);
  }

  @Override
  public void onViewOffset() {
    boolean processVisibilityOutputs = !mHost.isInTransientState();
    if (processVisibilityOutputs) {
      final Rect localVisibleRect = mHost.getVisibleRect();
      processVisibilityOutputs(localVisibleRect, null, false);
    }
  }

  @Override
  public void onUnbind() {
    clearVisibilityItems();
  }

  @Override
  public void onHostVisibilityChanged(boolean isVisible) {
    boolean processVisOutputs = !mHost.isInTransientState();
    if (!processVisOutputs) {
      return;
    }

    if (isVisible) {
      final Rect localVisibleRect = mHost.getVisibleRect();
      processVisibilityOutputs(localVisibleRect, null, false);
    } else {
      clearVisibilityItems();
    }
  }

  public void notifyOnUnbind() {
    clearVisibilityItems();
  }

  private static boolean isInRatioRange(float ratio, int length, int visiblelength) {
    return visiblelength >= ratio * length;
  }

  private static int computeRectArea(Rect rect) {
    return rect.isEmpty() ? 0 : (rect.width() * rect.height());
  }

  @VisibleForTesting
  Map<String, VisibilityItem> getVisibilityIdToItemMap() {
    return mVisibilityIdToItemMap;
  }
}
