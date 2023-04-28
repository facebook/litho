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
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.Systracer;
import com.facebook.rendercore.primitives.PrimitiveRenderUnit;
import java.util.List;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class PrimitiveLithoRenderUnit extends LithoRenderUnit {

  private final PrimitiveRenderUnit<Object> mPrimitiveRenderUnit;

  private PrimitiveLithoRenderUnit(
      final Component component,
      final @Nullable NodeInfo nodeInfo,
      final int flags,
      final int importantForAccessibility,
      final PrimitiveRenderUnit primitiveRenderUnit,
      final @Nullable ComponentContext context) {
    super(
        primitiveRenderUnit.getId(),
        component,
        nodeInfo,
        flags,
        importantForAccessibility,
        primitiveRenderUnit.getRenderType(),
        context);

    mPrimitiveRenderUnit = primitiveRenderUnit;
  }

  public static PrimitiveLithoRenderUnit create(
      final Component component,
      final @Nullable ComponentContext context,
      final @Nullable NodeInfo nodeInfo,
      final int flags,
      final int importantForAccessibility,
      final PrimitiveRenderUnit primitiveRenderUnit) {
    return new PrimitiveLithoRenderUnit(
        component, nodeInfo, flags, importantForAccessibility, primitiveRenderUnit, context);
  }

  @Override
  public ContentAllocator<Object> getContentAllocator() {
    return mPrimitiveRenderUnit.getContentAllocator();
  }

  @Override
  public boolean doesMountRenderTreeHosts() {
    return mPrimitiveRenderUnit.doesMountRenderTreeHosts();
  }

  @Override
  protected void mountBinders(
      Context context, Object o, @Nullable Object layoutData, Systracer tracer) {
    mPrimitiveRenderUnit.mountBinders(
        context, o, Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData, tracer);
  }

  @Override
  protected void unmountBinders(
      Context context, Object o, @Nullable Object layoutData, Systracer tracer) {
    mPrimitiveRenderUnit.unmountBinders(
        context, o, Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData, tracer);
  }

  @Override
  protected void attachBinders(
      Context context, Object content, @Nullable Object layoutData, Systracer tracer) {
    mPrimitiveRenderUnit.attachBinders(
        context,
        content,
        Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData,
        tracer);
  }

  @Override
  protected void detachBinders(
      Context context, Object content, @Nullable Object layoutData, Systracer tracer) {
    mPrimitiveRenderUnit.detachBinders(
        context,
        content,
        Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData,
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
      boolean isAttached) {
    mPrimitiveRenderUnit.updateBinders(
        context,
        o,
        ((PrimitiveLithoRenderUnit) currentRenderUnit).mPrimitiveRenderUnit,
        Preconditions.checkNotNull((LithoLayoutData) currentLayoutData).layoutData,
        Preconditions.checkNotNull((LithoLayoutData) newLayoutData).layoutData,
        mountDelegate,
        isAttached);
  }

  @Override
  @Nullable
  public <T extends Binder<?, ?>> T findAttachBinderByClass(Class<T> klass) {
    return mPrimitiveRenderUnit.findAttachBinderByClass(klass);
  }

  @Nullable
  @Override
  public Map<Class<?>, DelegateBinder<?, Object>> getOptionalMountBinderTypeToDelegateMap() {
    return mPrimitiveRenderUnit.getOptionalMountBinderTypeToDelegateMap();
  }

  @Nullable
  @Override
  public List<DelegateBinder<?, Object>> getOptionalMountBinders() {
    return mPrimitiveRenderUnit.getOptionalMountBinders();
  }

  @Nullable
  @Override
  public Map<Class<?>, DelegateBinder<?, Object>> getAttachBinderTypeToDelegateMap() {
    return mPrimitiveRenderUnit.getAttachBinderTypeToDelegateMap();
  }

  @Nullable
  @Override
  public List<DelegateBinder<?, Object>> getAttachBinders() {
    return mPrimitiveRenderUnit.getAttachBinders();
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

  public PrimitiveRenderUnit<?> getPrimitiveRenderUnit() {
    return mPrimitiveRenderUnit;
  }
}
