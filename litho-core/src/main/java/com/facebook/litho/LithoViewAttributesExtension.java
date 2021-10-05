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
import java.util.HashMap;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoViewAttributesExtension
    extends MountExtension<Void, LithoViewAttributesExtension.LithoViewAttributesState> {

  private static final LithoViewAttributesExtension sInstance = new LithoViewAttributesExtension();

  private LithoViewAttributesExtension() {}

  static LithoViewAttributesExtension getInstance() {
    return sInstance;
  }

  @Override
  protected LithoViewAttributesState createState() {
    return new LithoViewAttributesState();
  }

  static class LithoViewAttributesState {
    private Map<Long, Integer> mDefaultViewAttributes = new HashMap<>();

    void setDefaultViewAttributes(long renderUnitId, int flags) {
      mDefaultViewAttributes.put(renderUnitId, flags);
    }

    int getDefaultViewAttributes(long renderUnitId) {
      final Integer flags = mDefaultViewAttributes.get(renderUnitId);
      if (flags == null) {
        throw new IllegalStateException(
            "View attributes not found, did you call onUnbindItem without onBindItem?");
      }

      return flags;
    }

    boolean hasDefaultViewAttributes(long renderUnitId) {
      return mDefaultViewAttributes.containsKey(renderUnitId);
    }
  }

  @Override
  public void onBindItem(
      final ExtensionState<LithoViewAttributesState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    if (renderUnit instanceof LithoRenderUnit) {
      final LithoRenderUnit lithoRenderUnit = (LithoRenderUnit) renderUnit;
      final LayoutOutput output = lithoRenderUnit.output;
      final LithoViewAttributesState state = extensionState.getState();
      final long id = lithoRenderUnit.getId();

      if (!state.hasDefaultViewAttributes(id)) {
        state.setDefaultViewAttributes(id, LithoMountData.getViewAttributeFlags(content));
      }

      MountState.setViewAttributes(content, output);
    }
  }

  @Override
  public void onUnbindItem(
      final ExtensionState<LithoViewAttributesState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    if (renderUnit instanceof LithoRenderUnit) {
      final LithoRenderUnit lithoRenderUnit = (LithoRenderUnit) renderUnit;
      final LayoutOutput output = lithoRenderUnit.output;
      final LithoViewAttributesState state = extensionState.getState();
      final int flags = state.getDefaultViewAttributes(lithoRenderUnit.getId());
      MountState.unsetViewAttributes(content, output, flags);
    }
  }

  @Override
  public boolean shouldUpdateItem(
      final RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData) {
    if (previousRenderUnit == nextRenderUnit) {
      return false;
    }

    final LithoRenderUnit prevLithoRenderUnit = (LithoRenderUnit) previousRenderUnit;
    final LithoRenderUnit nextLithoRenderUnit = (LithoRenderUnit) nextRenderUnit;

    return LithoRenderUnit.shouldUpdateMountItem(
            prevLithoRenderUnit, nextLithoRenderUnit, previousLayoutData, nextLayoutData)
        || MountState.shouldUpdateViewInfo(nextLithoRenderUnit.output, prevLithoRenderUnit.output);
  }
}
