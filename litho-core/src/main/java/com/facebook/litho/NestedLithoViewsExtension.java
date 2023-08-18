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

package com.facebook.litho;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.OnItemCallbacks;
import java.util.ArrayList;

/**
 * MountExtension to ensure that content with nested LithoViews is properly clearing those
 * LithoViews when the item is unmounted. Since this should only happen when unmounting an item and
 * not when it's being updated, shouldUpdateItem is not overridden (defaulting to super
 * implementation which returns false).
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NestedLithoViewsExtension extends MountExtension<Void, Void>
    implements OnItemCallbacks<Void> {

  @Override
  protected Void createState() {
    return null;
  }

  @Override
  public void onUnmountItem(
      ExtensionState<Void> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {

    if (content instanceof HasLithoViewChildren) {
      final ArrayList<BaseMountingView> baseMountingViews = new ArrayList<>();
      ((HasLithoViewChildren) content).obtainLithoViewChildren(baseMountingViews);

      for (int i = baseMountingViews.size() - 1; i >= 0; i--) {
        final BaseMountingView baseMountingView = baseMountingViews.get(i);
        baseMountingView.unmountAllItems();
      }
    }
  }

  @Override
  public void beforeMountItem(
      ExtensionState<Void> extensionState, RenderTreeNode renderTreeNode, int index) {}

  @Override
  public void onMountItem(
      ExtensionState<Void> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  @Override
  public boolean shouldUpdateItem(
      ExtensionState<Void> extensionState,
      RenderUnit<?> previousRenderUnit,
      @Nullable Object previousLayoutData,
      RenderUnit<?> nextRenderUnit,
      @Nullable Object nextLayoutData) {
    return false;
  }

  @Override
  public void onBindItem(
      ExtensionState<Void> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  @Override
  public void onUnbindItem(
      ExtensionState<Void> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  @Override
  public void onBoundsAppliedToItem(
      ExtensionState<Void> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}
}
