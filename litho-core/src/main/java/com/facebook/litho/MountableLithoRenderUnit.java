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
import com.facebook.rendercore.Mountable;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.RenderUnit.DelegateBinder;
import com.facebook.rendercore.Systracer;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class MountableLithoRenderUnit extends LithoRenderUnit {

  private final Mountable<Object> mMountable;

  private MountableLithoRenderUnit(
      final Component component,
      final @Nullable SparseArray<DynamicValue<?>> commonDynamicProps,
      final @Nullable NodeInfo nodeInfo,
      final int flags,
      final int importantForAccessibility,
      final Mountable mountable,
      final @Nullable ComponentContext context,
      final @Nullable String debugKey) {
    super(
        mountable.getId(),
        component,
        (SparseArray) commonDynamicProps,
        nodeInfo,
        flags,
        importantForAccessibility,
        mountable.getRenderType(),
        context,
        debugKey);

    mMountable = mountable;
  }

  public static MountableLithoRenderUnit create(
      final Component component,
      final @Nullable SparseArray<DynamicValue<?>> commonDynamicProps,
      final @Nullable ComponentContext context,
      final @Nullable NodeInfo nodeInfo,
      final int flags,
      final int importantForAccessibility,
      final Mountable mountable,
      final @Nullable String debugKey) {
    return new MountableLithoRenderUnit(
        component,
        commonDynamicProps,
        nodeInfo,
        flags,
        importantForAccessibility,
        mountable,
        context,
        debugKey);
  }

  @Override
  public ContentAllocator<Object> getContentAllocator() {
    return mMountable.getContentAllocator();
  }

  @Override
  public boolean doesMountRenderTreeHosts() {
    return mMountable.doesMountRenderTreeHosts();
  }

  @Nullable
  @Override
  public <T> T getExtra(@IdRes final int key) {
    return mMountable.getExtra(key);
  }

  @Override
  protected void mountBinders(
      Context context, Object o, @Nullable Object layoutData, BindData bindData, Systracer tracer) {
    mMountable.mountBinders(
        context,
        o,
        Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData,
        bindData,
        tracer);
  }

  @Override
  protected void unmountBinders(
      Context context, Object o, @Nullable Object layoutData, BindData bindData, Systracer tracer) {
    mMountable.unmountBinders(
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
    mMountable.attachBinders(
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
    mMountable.detachBinders(
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
    mMountable.updateBinders(
        context,
        o,
        ((MountableLithoRenderUnit) currentRenderUnit).mMountable,
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
    return mMountable.findAttachBinderByClass(klass);
  }

  @Override
  public boolean containsAttachBinder(final DelegateBinder<?, ?, ?> delegateBinder) {
    return mMountable.containsAttachBinder(delegateBinder);
  }

  @Override
  public boolean containsOptionalMountBinder(final DelegateBinder<?, ?, ?> delegateBinder) {
    return mMountable.containsOptionalMountBinder(delegateBinder);
  }

  @Override
  public void addOptionalMountBinder(DelegateBinder<?, ? super Object, ?> binder) {
    mMountable.addOptionalMountBinder(binder);
  }

  @Override
  public Class<?> getRenderContentType() {
    return mMountable.getRenderContentType();
  }

  @Override
  public String getDescription() {
    // TODO: have a similar API for Mountable.
    return getComponent().getSimpleName();
  }

  public Mountable<?> getMountable() {
    return mMountable;
  }
}
