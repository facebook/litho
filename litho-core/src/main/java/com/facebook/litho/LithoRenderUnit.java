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
import static com.facebook.litho.MountState.shouldUpdateMountItem;
import static com.facebook.rendercore.RenderUnit.Extension.extension;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.transitions.TransitionRenderUnit;

/** This {@link RenderUnit} encapsulates a Litho output to be mounted using Render Core. */
public class LithoRenderUnit extends RenderUnit<Object> implements TransitionRenderUnit {

  final LayoutOutput output;
  final long mId;

  private int mDefaultViewAttributeFlags = -1;

  public LithoRenderUnit(long id, LayoutOutput output) {
    super(getRenderType(output));
    addMountUnmountExtensions(extension(this, LithoMountBinder.INSTANCE));
    addAttachDetachExtension(extension(this, LithoBindBinder.INSTANCE));
    this.output = output;
    this.mId = id;
  }

  @Override
  public boolean isRecyclingDisabled() {
    // Avoid recycling hosts in Litho
    return this.output.getComponent() instanceof HostComponent;
  }

  @Override
  @Nullable
  public MountItemsPool.ItemPool getRecyclingPool() {
    final MountContentPool mountContentPool = output.getComponent().onCreateMountContentPool();
    return new MountContentPoolWrapper(mountContentPool);
  }

  @Override
  public Object createContent(Context c) {
    return output.getComponent().createMountContent(c);
  }

  @Override
  public long getId() {
    return mId;
  }

  @Override
  public Object getRenderContentType() {
    return output.getComponent().getClass();
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
    return output.getComponent().getMountType() == Component.MountType.DRAWABLE
        ? RenderType.DRAWABLE
        : RenderType.VIEW;
  }

  @Override
  public boolean getMatchHostBounds() {
    return (output.getFlags() & LayoutOutput.LAYOUT_FLAG_MATCH_HOST_BOUNDS) != 0;
  }

  static @Nullable ComponentContext getContext(@Nullable Object data) {
    if (data == null) {
      return null;
    }

    if (data instanceof ComponentContext) {
      return (ComponentContext) data;
    }

    return ((ScopedComponentInfo) data).getContext();
  }

  public static class LithoMountBinder implements Binder<LithoRenderUnit, Object> {

    public static final LithoMountBinder INSTANCE = new LithoMountBinder();

    @Override
    public boolean shouldUpdate(
        final LithoRenderUnit current,
        final LithoRenderUnit next,
        final Object currentData,
        final Object nextData) {

      // TODO: Evaluate if this is even relevant anymore
      final int previousIdFromNextOutput =
          nextData != null ? LayoutState.getPreviousId(getContext(nextData)) : 0;
      final int idFromCurrentOutput =
          currentData != null ? LayoutState.getId(getContext(currentData)) : 0;

      final boolean updateValueFromLayoutOutput = previousIdFromNextOutput == idFromCurrentOutput;

      return shouldUpdateMountItem(
          next.output,
          getContext(nextData),
          current.output,
          getContext(currentData),
          updateValueFromLayoutOutput);
    }

    @Override
    public void bind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      output.getComponent().mount(getContext(data), content);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      output.getComponent().unmount(getContext(data), content);
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

      output.getComponent().bind(getContext(data), content);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      output.getComponent().unbind(getContext(data), content);
    }
  }

  /**
   * Default wrapper for Litho's MountContentPool to work with RenderCore's MountItemsPools. The
   * MountContentPool is acquired via the Component's onCreateMountContentPool. RenderCore utilizes
   * the MountItemsPool.ItemPool interface via RenderUnit's getRecyclingPool method.
   */
  private class MountContentPoolWrapper implements MountItemsPool.ItemPool {
    private final MountContentPool mMountContentPool;

    public MountContentPoolWrapper(MountContentPool mountContentPool) {
      mMountContentPool = mountContentPool;
    }

    @Override
    public Object acquire(Context c, RenderUnit renderUnit) {
      if (!(renderUnit instanceof LithoRenderUnit)) {
        return null;
      }

      final Component component = ((LithoRenderUnit) renderUnit).output.getComponent();
      return mMountContentPool.acquire(c, component);
    }

    @Override
    public void release(Object item) {
      mMountContentPool.release(item);
    }

    @Override
    public void maybePreallocateContent(Context c, RenderUnit renderUnit) {
      if (!(renderUnit instanceof LithoRenderUnit)) {
        return;
      }

      final Component component = ((LithoRenderUnit) renderUnit).output.getComponent();
      mMountContentPool.maybePreallocateContent(c, component);
    }
  }
}
