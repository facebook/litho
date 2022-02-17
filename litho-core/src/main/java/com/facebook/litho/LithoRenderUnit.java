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
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.transitions.TransitionRenderUnit;

@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class LithoRenderUnit extends RenderUnit<Object> implements TransitionRenderUnit {

  protected final long mId;
  protected final LayoutOutput output;
  protected final @Nullable ComponentContext mContext;

  protected LithoRenderUnit(
      long id, LayoutOutput output, RenderType renderType, @Nullable ComponentContext context) {
    super(renderType);
    this.mContext = context;
    this.output = output;
    this.mId = id;
  }

  public LayoutOutput getLayoutOutput() {
    return output;
  }

  public @Nullable ComponentContext getComponentContext() {
    return mContext;
  }

  @Override
  public long getId() {
    return mId;
  }

  @Override
  public boolean getMatchHostBounds() {
    return (output.getFlags() & LayoutOutput.LAYOUT_FLAG_MATCH_HOST_BOUNDS) != 0;
  }

  @Override
  public boolean isRecyclingDisabled() {
    return output.getComponent().isRecyclingDisabled();
  }

  @Override
  protected Class getDescription() {
    return output.getComponent().getClass();
  }

  static @Nullable ComponentContext getComponentContext(MountItem item) {
    return ((LithoRenderUnit) item.getRenderTreeNode().getRenderUnit()).getComponentContext();
  }

  static @Nullable ComponentContext getComponentContext(RenderTreeNode node) {
    return ((LithoRenderUnit) node.getRenderUnit()).getComponentContext();
  }

  static @Nullable ComponentContext getComponentContext(LithoRenderUnit unit) {
    return unit.getComponentContext();
  }

  public static boolean isMountableView(RenderUnit unit) {
    return unit.getRenderType() == RenderType.VIEW;
  }
}
