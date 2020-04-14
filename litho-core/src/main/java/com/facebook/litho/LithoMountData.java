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

import android.content.Context;
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
  boolean isViewSelected() {
    return (mDefaultAttributeValuesFlags & FLAG_VIEW_SELECTED) == FLAG_VIEW_SELECTED;
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
      final String globalKey = mComponent != null ? mComponent.getGlobalKey() : "<null>";
      throw new ReleasingReleasedMountContentException(
          "Releasing released mount content! component: "
              + componentName
              + ", globalKey: "
              + globalKey
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
    LayoutOutput output =
        new LayoutOutput(
            null,
            viewNodeInfo,
            HostComponent.create(lithoView.getComponentContext()),
            lithoView.getPreviousMountBounds(),
            0,
            0,
            0,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            lithoView.getContext().getResources().getConfiguration().orientation,
            null);
    MountItem item = new MountItem(LayoutOutput.create(output, null), lithoView, lithoView);
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
    }

    return flags;
  }

  public static class ReleasingReleasedMountContentException extends RuntimeException {

    public ReleasingReleasedMountContentException(String message) {
      super(message);
    }
  }
}
