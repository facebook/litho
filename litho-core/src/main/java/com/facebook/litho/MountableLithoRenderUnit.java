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
import com.facebook.rendercore.Mountable;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.Systracer;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class MountableLithoRenderUnit extends LithoRenderUnit {

  private final Mountable<Object> mMountable;

  private MountableLithoRenderUnit(
      final Component component,
      final @Nullable NodeInfo nodeInfo,
      final int flags,
      final int importantForAccessibility,
      final Mountable mountable,
      final @Nullable ComponentContext context) {
    super(
        mountable.getId(),
        component,
        nodeInfo,
        flags,
        importantForAccessibility,
        mountable.getRenderType(),
        context);

    mMountable = mountable;
  }

  public static MountableLithoRenderUnit create(
      final Component component,
      final @Nullable ComponentContext context,
      final @Nullable NodeInfo nodeInfo,
      final int flags,
      final int importantForAccessibility,
      final Mountable mountable) {
    return new MountableLithoRenderUnit(
        component, nodeInfo, flags, importantForAccessibility, mountable, context);
  }

  @Override
  public ContentAllocator<Object> getContentAllocator() {
    return mMountable.getContentAllocator();
  }

  @Override
  public boolean doesMountRenderTreeHosts() {
    return mMountable.doesMountRenderTreeHosts();
  }

  @Override
  protected void mountBinders(
      Context context, Object o, @Nullable Object layoutData, Systracer tracer) {
    mMountable.mountBinders(
        context, o, Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData, tracer);
  }

  @Override
  protected void unmountBinders(
      Context context, Object o, @Nullable Object layoutData, Systracer tracer) {
    mMountable.unmountBinders(
        context, o, Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData, tracer);
  }

  @Override
  protected void attachBinders(
      Context context, Object content, @Nullable Object layoutData, Systracer tracer) {
    mMountable.attachBinders(
        context,
        content,
        Preconditions.checkNotNull((LithoLayoutData) layoutData).layoutData,
        tracer);
  }

  @Override
  protected void detachBinders(
      Context context, Object content, @Nullable Object layoutData, Systracer tracer) {
    mMountable.detachBinders(
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
      boolean isAttached,
      Systracer tracer) {
    mMountable.updateBinders(
        context,
        o,
        ((MountableLithoRenderUnit) currentRenderUnit).mMountable,
        Preconditions.checkNotNull((LithoLayoutData) currentLayoutData).layoutData,
        Preconditions.checkNotNull((LithoLayoutData) newLayoutData).layoutData,
        mountDelegate,
        isAttached,
        tracer);
  }

  @Override
  @Nullable
  public <T extends Binder<?, ?>> T findAttachBinderByClass(Class<T> klass) {
    return mMountable.findAttachBinderByClass(klass);
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
