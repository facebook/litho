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

import static com.facebook.litho.ComponentHostUtils.maybeSetDrawableState;
import static com.facebook.litho.LayoutState.KEY_LAYOUT_STATE_ID;
import static com.facebook.litho.LayoutState.KEY_PREVIOUS_LAYOUT_STATE_ID;
import static com.facebook.litho.MountState.shouldUpdateMountItem;
import static com.facebook.litho.MountState.shouldUpdateViewInfo;
import static com.facebook.rendercore.RenderUnit.Extension.extension;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.rendercore.RenderUnit;
import java.util.Map;

/** This {@link RenderUnit} encapsulates a Litho output to be mounted using Render Core. */
public class LithoRenderUnit extends RenderUnit<Object> {

  final LayoutOutput output;

  private int mDefaultViewAttributeFlags = -1;

  public LithoRenderUnit(LayoutOutput output) {
    super(getRenderType(output));
    addMountUnmountExtensions(
        extension(this, LithoMountBinder.INSTANCE),
        extension(this, LithoViewAttributeBinder.INSTANCE));
    addAttachDetachExtension(extension(this, LithoBindBinder.INSTANCE));
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
      final LayoutOutput output = unit.output;
      output.getComponent().mount(output.getComponent().getScopedContext(), content);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
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
      final LayoutOutput output = unit.output;
      if (content instanceof Drawable) {
        final Drawable drawable = (Drawable) content;
        if (drawable.getCallback() instanceof View) {
          final View view = (View) drawable.getCallback();
          maybeSetDrawableState(view, drawable, output.getFlags(), output.getNodeInfo());
        }
      }

      output.getComponent().bind(output.getComponent().getScopedContext(), content);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      output.getComponent().unbind(output.getComponent().getScopedContext(), content);
    }
  }

  public static class LithoViewAttributeBinder implements Binder<LithoRenderUnit, Object> {

    public static final LithoViewAttributeBinder INSTANCE = new LithoViewAttributeBinder();

    @Override
    public boolean shouldUpdate(
        final LithoRenderUnit current,
        final LithoRenderUnit next,
        final Object currentData,
        final Object nextData) {
      return shouldUpdateViewInfo(next.output, current.output);
    }

    @Override
    public void bind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      if (!unit.hasDefaultViewAttributeFlags()) {
        unit.setDefaultViewAttributeFlags(LithoMountData.getViewAttributeFlags(content));
      }
      MountState.setViewAttributes(content, output);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      final int flags = unit.getDefaultViewAttributeFLags();
      MountState.unsetViewAttributes(content, output, flags);
    }
  }
}
