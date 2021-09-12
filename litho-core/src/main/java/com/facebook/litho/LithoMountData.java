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

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.yoga.YogaDirection;

/** This class hosts any extra mount data related to MountItem. */
public class LithoMountData {

  private static final int FLAG_VIEW_CLICKABLE = 1 << 0;
  private static final int FLAG_VIEW_LONG_CLICKABLE = 1 << 1;
  private static final int FLAG_VIEW_FOCUSABLE = 1 << 2;
  private static final int FLAG_VIEW_ENABLED = 1 << 3;
  private static final int FLAG_VIEW_SELECTED = 1 << 4;
  private static final int FLAG_VIEW_LAYER_TYPE_0 = 1 << 5;
  private static final int FLAG_VIEW_LAYER_TYPE_1 = 1 << 6;

  // Flags that track view-related behaviour of mounted view content.
  final int mDefaultAttributeValuesFlags;

  boolean mIsReleased;
  String mReleaseCause;

  public LithoMountData(Object content) {
    mDefaultAttributeValuesFlags = getViewAttributeFlags(content);
  }

  public int getDefaultAttributeValuesFlags() {
    return mDefaultAttributeValuesFlags;
  }

  /** @return Whether the view associated with this MountItem is clickable. */
  static boolean isViewClickable(int flags) {
    return (flags & FLAG_VIEW_CLICKABLE) == FLAG_VIEW_CLICKABLE;
  }

  /** @return Whether the view associated with this MountItem is long clickable. */
  static boolean isViewLongClickable(int flags) {
    return (flags & FLAG_VIEW_LONG_CLICKABLE) == FLAG_VIEW_LONG_CLICKABLE;
  }

  /** @return Whether the view associated with this MountItem is setFocusable. */
  static boolean isViewFocusable(int flags) {
    return (flags & FLAG_VIEW_FOCUSABLE) == FLAG_VIEW_FOCUSABLE;
  }

  /** @return Whether the view associated with this MountItem is setEnabled. */
  static boolean isViewEnabled(int flags) {
    return (flags & FLAG_VIEW_ENABLED) == FLAG_VIEW_ENABLED;
  }

  /** @return Whether the view associated with this MountItem is setSelected. */
  static boolean isViewSelected(int flags) {
    return (flags & FLAG_VIEW_SELECTED) == FLAG_VIEW_SELECTED;
  }

  static @LayerType int getOriginalLayerType(final int flags) {
    if ((flags & FLAG_VIEW_LAYER_TYPE_0) == 0) {
      return LayerType.LAYER_TYPE_NOT_SET;
    } else if ((flags & FLAG_VIEW_LAYER_TYPE_1) == FLAG_VIEW_LAYER_TYPE_1) {
      return LayerType.LAYER_TYPE_HARDWARE;
    } else {
      return LayerType.LAYER_TYPE_SOFTWARE;
    }
  }

  void releaseMountContent(
      final Context context,
      final MountItem item,
      final String releaseCause,
      final int recyclingMode) {
    final RenderTreeNode node = item.getRenderTreeNode();
    final LayoutOutput output = getLayoutOutput(node);
    final Component mComponent = output.getComponent();
    if (mIsReleased) {
      final String componentName = mComponent != null ? mComponent.getSimpleName() : "<null>";
      throw new ReleasingReleasedMountContentException(
          "Releasing released mount content! component: "
              + componentName
              + ", transitionId: "
              + output.getTransitionId()
              + ", previousReleaseCause: "
              + mReleaseCause);
    }
    ComponentsPools.release(context, mComponent, item.getContent(), recyclingMode);
    mIsReleased = true;
    mReleaseCause = releaseCause;
  }

  /** This mountItem represents the top-level root host (LithoView) which is always mounted. */
  static MountItem createRootHostMountItem(LithoView lithoView) {
    final ViewNodeInfo viewNodeInfo = new ViewNodeInfo();
    viewNodeInfo.setLayoutDirection(YogaDirection.INHERIT);
    final LithoRenderUnit unit =
        LithoRenderUnit.create(
            ROOT_HOST_ID,
            HostComponent.create(),
            null,
            null,
            viewNodeInfo,
            lithoView.getPreviousMountBounds(),
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            LayoutOutput.STATE_DIRTY,
            null);

    final Rect bounds = lithoView.getPreviousMountBounds();

    MountItem item =
        new MountItem(LithoRenderUnit.create(unit, bounds, null), lithoView, lithoView);
    item.setMountData(new LithoMountData(lithoView));
    return item;
  }

  static LithoMountData getMountData(MountItem item) {
    Object data = item.getMountData();
    if (!(data instanceof LithoMountData)) {
      throw new RuntimeException("MountData should not be null when using Litho's MountState.");
    }
    return (LithoMountData) item.getMountData();
  }

  static int getViewAttributeFlags(Object content) {
    int flags = 0;

    if (content instanceof View) {
      final View view = (View) content;

      if (view.isClickable()) {
        flags |= FLAG_VIEW_CLICKABLE;
      }

      if (view.isLongClickable()) {
        flags |= FLAG_VIEW_LONG_CLICKABLE;
      }

      if (view.isFocusable()) {
        flags |= FLAG_VIEW_FOCUSABLE;
      }

      if (view.isEnabled()) {
        flags |= FLAG_VIEW_ENABLED;
      }

      if (view.isSelected()) {
        flags |= FLAG_VIEW_SELECTED;
      }

      final int layerType = view.getLayerType();
      switch (layerType) {
        case View.LAYER_TYPE_NONE:
          break;
        case View.LAYER_TYPE_SOFTWARE:
          flags |= FLAG_VIEW_LAYER_TYPE_0;
          break;
        case View.LAYER_TYPE_HARDWARE:
          flags |= FLAG_VIEW_LAYER_TYPE_1;
          break;
        default:
          throw new IllegalArgumentException("Unhandled layer type encountered.");
      }
    }

    return flags;
  }

  public static class ReleasingReleasedMountContentException extends RuntimeException {

    public ReleasingReleasedMountContentException(String message) {
      super(message);
    }
  }
}
