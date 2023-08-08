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

import android.content.Context;
import android.util.SparseArray;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.BindData;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.RenderUnit.DelegateBinder;
import com.facebook.rendercore.Systracer;
import com.facebook.rendercore.primitives.PrimitiveRenderUnit;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class PrimitiveLithoRenderUnit extends LithoRenderUnit {

  private final PrimitiveRenderUnit<Object> mPrimitiveRenderUnit;

  private PrimitiveLithoRenderUnit(
      final Component component,
      final @Nullable SparseArray<DynamicValue<?>> commonDynamicProps,
      final @Nullable NodeInfo nodeInfo,
      final int flags,
      final int importantForAccessibility,
      final PrimitiveRenderUnit primitiveRenderUnit,
      final @Nullable ComponentContext context,
      final @Nullable String debugKey) {
    super(
        primitiveRenderUnit.getId(),
        component,
        (SparseArray) commonDynamicProps,
        nodeInfo,
        flags,
        importantForAccessibility,
        primitiveRenderUnit.getRenderType(),
        context,
        debugKey);

    mPrimitiveRenderUnit = primitiveRenderUnit;
  }

  public static PrimitiveLithoRenderUnit create(
      final Component component,
      final @Nullable SparseArray<DynamicValue<?>> commonDynamicProps,
      final @Nullable ComponentContext context,
      final @Nullable NodeInfo nodeInfo,
      final int flags,
      final int importantForAccessibility,
      final PrimitiveRenderUnit primitiveRenderUnit,
      final @Nullable String debugKey) {
    return new PrimitiveLithoRenderUnit(
        component,
        commonDynamicProps,
        nodeInfo,
        flags,
        importantForAccessibility,
        primitiveRenderUnit,
        context,
        debugKey);
  }

  @Override
  public ContentAllocator<Object> getContentAllocator() {
    return mPrimitiveRenderUnit.getContentAllocator();
  }

  @Override
  public boolean doesMountRenderTreeHosts() {
    return mPrimitiveRenderUnit.doesMountRenderTreeHosts();
  }

  @Nullable
  @Override
  public <T> T getExtra(@IdRes final int key) {
    return mPrimitiveRenderUnit.getExtra(key);
  }

  @Override
  protected void mountBinders(
      Context context, Object o, @Nullable Object layoutData, BindData bindData, Systracer tracer) {
    mPrimitiveRenderUnit.mountBinders(
        context,
        o,
        Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData,
        bindData,
        tracer);
  }

  @Override
  protected void unmountBinders(
      Context context, Object o, @Nullable Object layoutData, BindData bindData, Systracer tracer) {
    mPrimitiveRenderUnit.unmountBinders(
        context,
        o,
        Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData,
        bindData,
        tracer);
  }

  @Override
  protected void attachBinders(
      Context context,
      Object content,
      @Nullable Object layoutData,
      BindData bindData,
      Systracer tracer) {
    mPrimitiveRenderUnit.attachBinders(
        context,
        content,
        Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData,
        bindData,
        tracer);
  }

  @Override
  protected void detachBinders(
      Context context,
      Object content,
      @Nullable Object layoutData,
      BindData bindData,
      Systracer tracer) {
    mPrimitiveRenderUnit.detachBinders(
        context,
        content,
        Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData,
        bindData,
        tracer);
  }

  @Override
  protected void updateBinders(
      Context context,
      Object o,
      RenderUnit<Object> currentRenderUnit,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData,
      @Nullable MountDelegate mountDelegate,
      BindData bindData,
      boolean isAttached,
      Systracer tracer) {
    mPrimitiveRenderUnit.updateBinders(
        context,
        o,
        ((PrimitiveLithoRenderUnit) currentRenderUnit).mPrimitiveRenderUnit,
        Preconditions.checkNotNull((LithoLayoutData) currentLayoutData).layoutData,
        Preconditions.checkNotNull((LithoLayoutData) newLayoutData).layoutData,
        mountDelegate,
        bindData,
        isAttached,
        tracer);
  }

  @Override
  @Nullable
  public <T extends Binder<?, ?, ?>> T findAttachBinderByClass(Class<T> klass) {
    return mPrimitiveRenderUnit.findAttachBinderByClass(klass);
  }

  @Override
  public boolean containsAttachBinder(final DelegateBinder<?, ?, ?> delegateBinder) {
    return mPrimitiveRenderUnit.containsAttachBinder(delegateBinder);
  }

  @Override
  public boolean containsOptionalMountBinder(final DelegateBinder<?, ?, ?> delegateBinder) {
    return mPrimitiveRenderUnit.containsOptionalMountBinder(delegateBinder);
  }

  @Override
  public Class<?> getRenderContentType() {
    return mPrimitiveRenderUnit.getClass();
  }

  @Override
  public String getDescription() {
    // TODO: have a similar API for Primitive.
    return getComponent().getSimpleName();
  }

  @Override
  public void addOptionalMountBinder(DelegateBinder<?, ? super Object, ?> binder) {
    mPrimitiveRenderUnit.addOptionalMountBinder(binder);
  }

  public PrimitiveRenderUnit<?> getPrimitiveRenderUnit() {
    return mPrimitiveRenderUnit;
  }
}
