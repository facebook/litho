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

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.transitions.TransitionRenderUnit;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class LithoRenderUnit extends RenderUnit<Object> implements TransitionRenderUnit {

  public static final int STATE_UNKNOWN = 0;
  public static final int STATE_UPDATED = 1;
  public static final int STATE_DIRTY = 2;

  static final int LAYOUT_FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int LAYOUT_FLAG_DISABLE_TOUCHABLE = 1 << 1;
  static final int LAYOUT_FLAG_MATCH_HOST_BOUNDS = 1 << 2;
  static final int LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED = 1 << 3;
  static final int LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES = 1 << 4;

  @IntDef({STATE_UPDATED, STATE_UNKNOWN, STATE_DIRTY})
  @Retention(RetentionPolicy.SOURCE)
  public @interface UpdateState {}

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
