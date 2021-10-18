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

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import java.util.ArrayList;

/**
 * MountExtension to ensure that content with nested LithoViews is properly clearing those
 * LithoViews when the item is unmounted. Since this should only happen when unmounting an item and
 * not when it's being updated, shouldUpdateItem is not overridden (defaulting to super
 * implementation which returns false).
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NestedLithoViewsExtension extends MountExtension<Void, Void> {

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
      final ArrayList<LithoView> lithoViews = new ArrayList<>();
      ((HasLithoViewChildren) content).obtainLithoViewChildren(lithoViews);

      for (int i = lithoViews.size() - 1; i >= 0; i--) {
        final LithoView lithoView = lithoViews.get(i);
        lithoView.unmountAllItems();
      }
    }
  }
}
