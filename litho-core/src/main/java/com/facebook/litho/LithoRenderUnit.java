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

import static java.util.Collections.singletonList;

import android.content.Context;
import android.graphics.Rect;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.RenderUnit;
import java.util.List;

/** This {@link RenderUnit} encapsulates a Litho output to be mounted using Render Core. */
public class LithoRenderUnit extends RenderUnit<Object> {

  static final List<LithoMountBinder> sMountBinder = singletonList(LithoMountBinder.INSTANCE);
  static final List<LithoBindBinder> sBindBinders = singletonList(LithoBindBinder.INSTANCE);

  private final LayoutOutput output;

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
        final LithoRenderUnit current, final LithoRenderUnit next, final Object c, final Object n) {
      return LithoRenderUnit.shouldUpdate(current.output, next.output);
    }

    @Override
    public void bind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      LayoutOutput output = unit.output;
      output.getComponent().mount(output.getComponent().getScopedContext(), content);
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
      return LithoRenderUnit.shouldUpdate(current.output, next.output);
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
      output.getComponent().unbind(output.getComponent().getScopedContext(), content);
    }
  }

  public static boolean shouldUpdate(final LayoutOutput current, final LayoutOutput next) {
    if (ComponentsConfiguration.shouldForceComponentUpdateOnOrientationChange
        && next.getOrientation() != current.getOrientation()) {
      return true;
    }

    final Component nextComponent = next.getComponent();
    final Component currentComponent = current.getComponent();

    // If the mounted content depends on the size
    // and if the size has changed then
    // return true immediately.
    if (nextComponent.isMountSizeDependent() && !sameSize(current, next)) {
      return true;
    }

    if (next.getUpdateState() == LayoutOutput.STATE_DIRTY) {
      return true;
    }

    if (!nextComponent.callsShouldUpdateOnMount()) {
      return true;
    }

    return nextComponent.shouldComponentUpdate(currentComponent, nextComponent);
  }

  public static boolean sameSize(final LayoutOutput current, final LayoutOutput next) {
    Rect c = current.getBounds();
    Rect n = next.getBounds();
    return c.width() == n.width() && c.height() == n.height();
  }
}
