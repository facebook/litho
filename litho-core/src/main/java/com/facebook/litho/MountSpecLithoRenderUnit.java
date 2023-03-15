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

import static com.facebook.litho.ComponentHostUtils.maybeSetDrawableState;
import static com.facebook.litho.LithoLayoutData.getInterStageProps;
import static com.facebook.litho.LithoLayoutData.verifyAndGetLithoLayoutData;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.RenderUnit;

/** This {@link RenderUnit} encapsulates a Litho output to be mounted using Render Core. */
public class MountSpecLithoRenderUnit extends LithoRenderUnit implements ContentAllocator<Object> {

  private boolean mIsShouldUpdateCachingEnabled;
  private boolean mIsShouldUpdateResultCached;
  private boolean mCachedShouldUpdateResult;

  private MountSpecLithoRenderUnit(
      long id,
      final Component component,
      final @Nullable NodeInfo nodeInfo,
      final @Nullable Rect touchBoundsExpansion,
      final int flags,
      final int importantForAccessibility,
      final @UpdateState int updateState,
      @Nullable ComponentContext context) {
    super(
        id,
        component,
        nodeInfo,
        touchBoundsExpansion,
        flags,
        importantForAccessibility,
        updateState,
        getRenderType(component),
        context);
    addOptionalMountBinders(
        DelegateBinder.createDelegateBinder(
            this, MountSpecLithoRenderUnit.LithoMountBinder.INSTANCE));
    addAttachBinder(
        DelegateBinder.createDelegateBinder(
            this, MountSpecLithoRenderUnit.LithoBindBinder.INSTANCE));
  }

  public static MountSpecLithoRenderUnit create(
      final long id,
      final Component component,
      final @Nullable ComponentContext context,
      final @Nullable NodeInfo nodeInfo,
      final @Nullable Rect touchBoundsExpansion,
      final int flags,
      final int importantForAccessibility,
      final @UpdateState int updateState) {

    return new MountSpecLithoRenderUnit(
        id,
        component,
        nodeInfo,
        touchBoundsExpansion,
        flags,
        importantForAccessibility,
        updateState,
        context);
  }

  @Override
  protected void onStartUpdateRenderUnit() {
    mIsShouldUpdateCachingEnabled = true;
  }

  @Override
  protected void onEndUpdateRenderUnit() {
    mIsShouldUpdateCachingEnabled = false;
    mIsShouldUpdateResultCached = false;
  }

  @Override
  @Nullable
  public MountItemsPool.ItemPool createRecyclingPool() {
    try {
      final Component component = getComponent();
      return (component instanceof SpecGeneratedComponent)
          ? ((SpecGeneratedComponent) component).createRecyclingPool()
          : null;
    } catch (Exception e) {
      if (mContext != null) {
        ComponentUtils.handle(mContext, e);
      }
      return null;
    }
  }

  @Override
  public Object createContent(Context c) {
    return getComponent().createMountContent(c);
  }

  @Override
  public Object getPoolableContentType() {
    return getRenderContentType();
  }

  @Override
  public boolean isRecyclingDisabled() {
    final Component component = getComponent();
    return (component instanceof SpecGeneratedComponent)
        && ((SpecGeneratedComponent) component).isRecyclingDisabled();
  }

  @Override
  public String getDescription() {
    return getComponent().getSimpleName();
  }

  @Override
  public ContentAllocator<Object> getContentAllocator() {
    return this;
  }

  @Override
  public Class<?> getRenderContentType() {
    return getComponent().getClass();
  }

  public static boolean shouldUpdateMountItem(
      final MountSpecLithoRenderUnit current,
      final MountSpecLithoRenderUnit next,
      final @Nullable Object currentData,
      final @Nullable Object nextData) {
    if (current.mIsShouldUpdateCachingEnabled && current.mIsShouldUpdateResultCached) {
      return current.mCachedShouldUpdateResult;
    }

    final LithoLayoutData currentLithoData = verifyAndGetLithoLayoutData(currentData);
    final LithoLayoutData nextLithoData = verifyAndGetLithoLayoutData(nextData);

    final @Nullable ComponentContext nextContext = getComponentContext(next);
    final int previousIdFromNextOutput = nextLithoData.previousLayoutStateId;

    final @Nullable ComponentContext currentContext = getComponentContext(current);
    final int idFromCurrentOutput = currentLithoData.currentLayoutStateId;

    final boolean updateValueFromLayoutOutput = previousIdFromNextOutput == idFromCurrentOutput;

    final boolean result =
        shouldUpdateMountItem(
            next,
            (LithoLayoutData) nextData,
            nextContext,
            current,
            (LithoLayoutData) currentData,
            currentContext,
            updateValueFromLayoutOutput);

    if (current.mIsShouldUpdateCachingEnabled && !current.mIsShouldUpdateResultCached) {
      current.mCachedShouldUpdateResult = result;
      current.mIsShouldUpdateResultCached = true;
    }

    return result;
  }

  public static class LithoMountBinder
      implements RenderUnit.Binder<MountSpecLithoRenderUnit, Object> {

    public static final MountSpecLithoRenderUnit.LithoMountBinder INSTANCE =
        new MountSpecLithoRenderUnit.LithoMountBinder();

    @Override
    public boolean shouldUpdate(
        final MountSpecLithoRenderUnit current,
        final MountSpecLithoRenderUnit next,
        final @Nullable Object currentData,
        final @Nullable Object nextData) {
      if (next.getComponent() instanceof HostComponent) {
        return false;
      }

      return shouldUpdateMountItem(current, next, currentData, nextData);
    }

    @Override
    public void bind(
        final Context context,
        final Object content,
        final MountSpecLithoRenderUnit unit,
        final @Nullable Object data) {
      final Component component = unit.getComponent();
      ((SpecGeneratedComponent) component)
          .mount(getComponentContext(unit), content, getInterStageProps(data));
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final MountSpecLithoRenderUnit unit,
        final @Nullable Object data) {
      ((SpecGeneratedComponent) unit.getComponent())
          .unmount(getComponentContext(unit), content, getInterStageProps(data));
    }
  }

  public static class LithoBindBinder
      implements RenderUnit.Binder<MountSpecLithoRenderUnit, Object> {

    public static final MountSpecLithoRenderUnit.LithoBindBinder INSTANCE =
        new MountSpecLithoRenderUnit.LithoBindBinder();

    @Override
    public boolean shouldUpdate(
        final MountSpecLithoRenderUnit current,
        final MountSpecLithoRenderUnit next,
        final @Nullable Object c,
        final @Nullable Object n) {
      return true;
    }

    @Override
    public void bind(
        final Context context,
        final Object content,
        final MountSpecLithoRenderUnit unit,
        final @Nullable Object data) {
      if (content instanceof Drawable) {
        final Drawable drawable = (Drawable) content;
        if (drawable.getCallback() instanceof View) {
          final View view = (View) drawable.getCallback();
          maybeSetDrawableState(view, drawable, unit.getFlags(), unit.getNodeInfo());
        }
      }

      ((SpecGeneratedComponent) unit.getComponent())
          .bind(getComponentContext(unit), content, getInterStageProps(data));
    }

    @Override
    public void unbind(
        final Context context,
        final Object content,
        final MountSpecLithoRenderUnit unit,
        final @Nullable Object data) {
      ((SpecGeneratedComponent) unit.getComponent())
          .unbind(getComponentContext(unit), content, getInterStageProps(data));
    }
  }

  private static RenderUnit.RenderType getRenderType(Component component) {
    if (component == null) {
      throw new IllegalArgumentException("Null output used for LithoRenderUnit.");
    }
    return component.getMountType() == Component.MountType.DRAWABLE
        ? RenderUnit.RenderType.DRAWABLE
        : RenderUnit.RenderType.VIEW;
  }

  static boolean shouldUpdateMountItem(
      final LithoRenderUnit nextRenderUnit,
      final @Nullable LithoLayoutData nextLayoutData,
      final @Nullable ComponentContext nextContext,
      final LithoRenderUnit currentRenderUnit,
      final @Nullable LithoLayoutData currentLayoutData,
      final @Nullable ComponentContext currentContext,
      final boolean useUpdateValueFromLayoutOutput) {
    @LithoRenderUnit.UpdateState final int updateState = nextRenderUnit.getUpdateState();
    final Component currentComponent = currentRenderUnit.getComponent();
    final Component nextComponent = nextRenderUnit.getComponent();

    // If the two components have different sizes and the mounted content depends on the size we
    // just return true immediately.
    if (nextComponent instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) nextComponent).isMountSizeDependent()
        && !sameSize(
            Preconditions.checkNotNull(nextLayoutData),
            Preconditions.checkNotNull(currentLayoutData))) {
      return true;
    }

    if (useUpdateValueFromLayoutOutput) {
      if (updateState == LithoRenderUnit.STATE_UPDATED) {

        // Check for incompatible ReferenceLifecycle.
        return currentComponent instanceof DrawableComponent
            && nextComponent instanceof DrawableComponent
            && shouldUpdate(currentComponent, currentContext, nextComponent, nextContext);
      } else if (updateState == LithoRenderUnit.STATE_DIRTY) {
        return true;
      }
    }

    return shouldUpdate(currentComponent, currentContext, nextComponent, nextContext);
  }

  private static boolean shouldUpdate(
      Component currentComponent,
      ComponentContext currentScopedContext,
      Component nextComponent,
      ComponentContext nextScopedContext) {

    final boolean isTracing = ComponentsSystrace.isTracing();

    try {
      if (isTracing) {
        ComponentsSystrace.beginSection("MountState.shouldUpdate");
      }
      return currentComponent.shouldComponentUpdate(
          currentScopedContext, currentComponent, nextScopedContext, nextComponent);
    } catch (Exception e) {
      ComponentUtils.handle(nextScopedContext, e);
      return true;
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  static boolean sameSize(final LithoLayoutData next, final LithoLayoutData current) {
    return next.width == current.width && next.height == current.height;
  }
}
