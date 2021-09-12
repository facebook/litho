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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.transitions.TransitionRenderUnit;

/** This {@link RenderUnit} encapsulates a Litho output to be mounted using Render Core. */
public class LithoRenderUnit extends RenderUnit<Object> implements TransitionRenderUnit {

  final long mId;
  final LayoutOutput output;
  final @Nullable ComponentContext mContext;

  LithoRenderUnit(long id, LayoutOutput output, @Nullable ComponentContext context) {
    super(getRenderType(output));
    addMountUnmountExtensions(extension(this, LithoMountBinder.INSTANCE));
    addAttachDetachExtension(extension(this, LithoBindBinder.INSTANCE));
    this.mContext = context;
    this.output = output;
    this.mId = id;
  }

  static @Nullable ComponentContext getComponentContext(MountItem item) {
    return ((LithoRenderUnit) item.getRenderTreeNode().getRenderUnit()).mContext;
  }

  static @Nullable ComponentContext getComponentContext(RenderTreeNode node) {
    return ((LithoRenderUnit) node.getRenderUnit()).mContext;
  }

  static @Nullable ComponentContext getComponentContext(LithoRenderUnit unit) {
    return unit.mContext;
  }

  static RenderTreeNode create(
      final LithoRenderUnit unit, final Rect bounds, final @Nullable RenderTreeNode parent) {

    return new RenderTreeNode(
        parent,
        unit,
        null,
        bounds,
        unit.output.getViewNodeInfo() != null ? unit.output.getViewNodeInfo().getPadding() : null,
        parent != null ? parent.getChildrenCount() : 0);
  }

  static Rect getMountBounds(Rect outRect, Rect bounds, int x, int y) {
    outRect.left = bounds.left - x;
    outRect.top = bounds.top - y;
    outRect.right = bounds.right - x;
    outRect.bottom = bounds.bottom - y;
    return outRect;
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

  public static LithoRenderUnit create(
      final long id,
      final Component component,
      final @Nullable ComponentContext context,
      final @Nullable NodeInfo nodeInfo,
      final @Nullable ViewNodeInfo viewNodeInfo,
      final Rect bounds,
      final int flags,
      final int importantForAccessibility,
      final @LayoutOutput.UpdateState int updateState,
      final @Nullable TransitionId transitionId) {
    final LayoutOutput output =
        new LayoutOutput(
            component,
            nodeInfo,
            viewNodeInfo,
            bounds,
            flags,
            importantForAccessibility,
            updateState,
            transitionId);

    return new LithoRenderUnit(id, output, context);
  }

  public static class LithoMountBinder implements Binder<LithoRenderUnit, Object> {

    public static final LithoMountBinder INSTANCE = new LithoMountBinder();

    @Override
    public boolean shouldUpdate(
        final LithoRenderUnit current,
        final LithoRenderUnit next,
        final Object currentData,
        final Object nextData) {

      final @Nullable ComponentContext nextContext = getComponentContext(next);
      final int previousIdFromNextOutput =
          nextContext != null ? LayoutState.getPreviousId(nextContext) : 0;

      final @Nullable ComponentContext currentContext = getComponentContext(current);
      final int idFromCurrentOutput =
          currentContext != null ? LayoutState.getId(currentContext) : 0;

      final boolean updateValueFromLayoutOutput = previousIdFromNextOutput == idFromCurrentOutput;

      return shouldUpdateMountItem(
          next.output, nextContext, current.output, currentContext, updateValueFromLayoutOutput);
    }

    @Override
    public void bind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      output.getComponent().mount(getComponentContext(unit), content);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      output.getComponent().unmount(getComponentContext(unit), content);
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

      output.getComponent().bind(getComponentContext(unit), content);
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final LithoRenderUnit unit,
        final Object data) {
      final LayoutOutput output = unit.output;
      output.getComponent().unbind(getComponentContext(unit), content);
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
