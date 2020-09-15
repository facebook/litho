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

import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.Function;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.extensions.MountExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisibilityOutputsExtension extends MountExtension<VisibilityOutputsExtensionInput> {

  private static final boolean IS_JELLYBEAN_OR_HIGHER =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;

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
  private @Nullable Rect mCurrentLocalVisibleRect;

  public VisibilityOutputsExtension(Host host) {
    mHost = host;
    mVisibilityIdToItemMap = new HashMap<>();
  }

  @UiThread
  private void processVisibilityOutputs(@Nullable Rect localVisibleRect, boolean isDirty) {
    try {
      RenderCoreSystrace.beginSection("processVisibilityOutputs");

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
      RenderCoreSystrace.endSection();
    }

    if (localVisibleRect != null) {
      mPreviousLocalVisibleRect.set(localVisibleRect);
    }
  }

  @UiThread
  private void processVisibilityOutputsNonInc(@Nullable Rect localVisibleRect, boolean isDirty) {
    if (localVisibleRect == null) {
      return;
    }

    for (int j = 0, size = mVisibilityOutputs.size(); j < size; j++) {
      final VisibilityOutput visibilityOutput = mVisibilityOutputs.get(j);
      final String componentName = visibilityOutput.getKey();
      RenderCoreSystrace.beginSection("visibilityHandlers:" + componentName);

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
          final String globalKey = visibilityOutput.getId();
          visibilityItem =
              new VisibilityItem(
                  globalKey, invisibleHandler, unfocusedHandler, visibilityChangedHandler);
          visibilityItem.setDoNotClearInThisPass(isDirty);
          visibilityItem.setWasFullyVisible(isFullyVisible);
          mVisibilityIdToItemMap.put(visibilityOutputId, visibilityItem);

          if (visibleHandler != null) {
            VisibilityUtils.dispatchOnVisible(visibleHandler);
          }
        }

        // Check if the component has entered or exited the focused range.
        if (focusedHandler != null || unfocusedHandler != null) {
          if (isInFocusedRange(visibilityOutputBounds, sTempRect)) {
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

    if (isDirty) {
      clearVisibilityItems();
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

  @UiThread
  public void clearVisibilityItems() {
    if (mVisibilityModule != null) {
      clearVisibilityItemsIncremental();
    } else {
      clearVisibilityItemsNonincremental();
    }
    mPreviousLocalVisibleRect.setEmpty();
  }

  @UiThread
  private void clearVisibilityItemsIncremental() {
    RenderCoreSystrace.beginSection("VisibilityExtension.clearIncrementalItems");

    if (mVisibilityModule != null) {
      mVisibilityModule.clearIncrementalItems();
    }

    RenderCoreSystrace.endSection();
  }

  @UiThread
  private void clearVisibilityItemsNonincremental() {
    RenderCoreSystrace.beginSection("VisibilityExtension.clearIncrementalItems");

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

      mVisibilityIdToItemMap.remove(key);
    }

    RenderCoreSystrace.endSection();
  }

  public void unmountAllItems() {
    mPreviousLocalVisibleRect.setEmpty();
  }

  @Override
  public void onUnmount() {
    mPreviousLocalVisibleRect.setEmpty();
  }

  @Override
  public void beforeMount(VisibilityOutputsExtensionInput input, @Nullable Rect localVisibleRect) {

    mVisibilityOutputs = input.getVisibilityOutputs();

    // Guard against the input being null.
    if (mVisibilityOutputs == null) {
      mVisibilityOutputs = new ArrayList<>();
    }

    mIncrementalVisibilityEnabled = input.isIncrementalVisibilityEnabled();
    mVisibilityModuleInput = input.getVisibilityModuleInput();
    mPreviousLocalVisibleRect.setEmpty();
    mCurrentLocalVisibleRect = localVisibleRect;
  }

  @Override
  public void afterMount() {
    boolean processVisibilityOutputs = !hasTransientState();

    if (processVisibilityOutputs) {
      processVisibilityOutputs(mCurrentLocalVisibleRect, true);
    }
  }

  @Override
  public void onVisibleBoundsChanged(@Nullable Rect localVisibleRect) {
    if (mVisibilityOutputs == null) {
      return;
    }

    boolean processVisibilityOutputs = !hasTransientState();
    if (processVisibilityOutputs) {
      processVisibilityOutputs(localVisibleRect, false);
    }
  }

  private boolean hasTransientState() {
    return IS_JELLYBEAN_OR_HIGHER && mHost.hasTransientState();
  }

  @Override
  public void onUnbind() {
    clearVisibilityItems();
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
  public Map<String, VisibilityItem> getVisibilityIdToItemMap() {
    return mVisibilityIdToItemMap;
  }

  @VisibleForTesting
  int getInputCount() {
    return mVisibilityOutputs.size();
  }
}
