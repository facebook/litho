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

import static com.facebook.litho.LayoutState.KEY_LAYOUT_STATE_ID;
import static com.facebook.litho.LayoutState.KEY_PREVIOUS_LAYOUT_STATE_ID;
import static com.facebook.litho.MountState.shouldUpdateMountItem;
import static java.util.Collections.singletonList;

import android.content.Context;
import com.facebook.rendercore.RenderUnit;
import java.util.List;
import java.util.Map;

/** This {@link RenderUnit} encapsulates a Litho output to be mounted using Render Core. */
public class LithoRenderUnit extends RenderUnit<Object> {

  static final List<LithoMountBinder> sMountBinder = singletonList(LithoMountBinder.INSTANCE);
  static final List<LithoBindBinder> sBindBinders = singletonList(LithoBindBinder.INSTANCE);

  final LayoutOutput output;

  private int mDefaultViewAttributeFlags = -1;

  public LithoRenderUnit(LayoutOutput output) {
    super(getRenderType(output), sMountBinder, sBindBinders);
    this.output = output;
  }

  @Override
  public Object createContent(Context c) {
    return output.getComponent().createMountContent(c);
  }

  @Override
  public long getId() {
    return output.getId();
  }

  private boolean hasDefaultViewAttributeFlags() {
    return mDefaultViewAttributeFlags != -1;
  }

  private void setDefaultViewAttributeFlags(int flags) {
    mDefaultViewAttributeFlags = flags;
  }

  public int getDefaultViewAttributeFLags() {
    return mDefaultViewAttributeFlags;
  }

  private static RenderType getRenderType(LayoutOutput output) {
    if (output == null) {
      throw new IllegalArgumentException("Null output used for LithoRenderUnit.");
    }
    return output.getComponent().getMountType() == ComponentLifecycle.MountType.DRAWABLE
        ? RenderType.DRAWABLE
        : RenderType.VIEW;
  }

  public static class LithoMountBinder implements Binder<LithoRenderUnit, Object> {

    public static final LithoMountBinder INSTANCE = new LithoMountBinder();

    @Override
    public boolean shouldUpdate(
        final LithoRenderUnit current,
        final LithoRenderUnit next,
        final Object currentData,
        final Object nextData) {
      final int previousIdFromNextOutput = (int) ((Map) nextData).get(KEY_PREVIOUS_LAYOUT_STATE_ID);
      final int idFromCurrentOutput = (int) ((Map) currentData).get(KEY_LAYOUT_STATE_ID);
      final boolean updateValueFromLayoutOutput = previousIdFromNextOutput == idFromCurrentOutput;
      return shouldUpdateMountItem(next.output, current.output, updateValueFromLayoutOutput);
    }

    @Override
    public void bind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      LayoutOutput output = unit.output;
      if (!unit.hasDefaultViewAttributeFlags()) {
        unit.setDefaultViewAttributeFlags(LithoMountData.getViewAttributeFlags(content));
      }
      output.getComponent().mount(output.getComponent().getScopedContext(), content);
      MountState.setViewAttributes(content, output);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      LayoutOutput output = unit.output;
      output.getComponent().unmount(output.getComponent().getScopedContext(), content);
    }
  }

  public static class LithoBindBinder implements Binder<LithoRenderUnit, Object> {

    public static final LithoBindBinder INSTANCE = new LithoBindBinder();

    @Override
    public boolean shouldUpdate(
        final LithoRenderUnit current, final LithoRenderUnit next, final Object c, final Object n) {
      return true;
    }

    @Override
    public void bind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      LayoutOutput output = unit.output;
      output.getComponent().bind(output.getComponent().getScopedContext(), content);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      LayoutOutput output = unit.output;
      int flags = unit.getDefaultViewAttributeFLags();
      MountState.unsetViewAttributes(content, output, flags);
      output.getComponent().unbind(output.getComponent().getScopedContext(), content);
    }
  }
}
