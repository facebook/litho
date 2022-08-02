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
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.Mountable;
import java.util.List;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class MountableLithoRenderUnit extends LithoRenderUnit {

  private final Mountable<Object> mMountable;

  private MountableLithoRenderUnit(
      final LayoutOutput output,
      final Mountable mountable,
      final @Nullable ComponentContext context) {
    super(mountable.getId(), output, mountable.getRenderType(), context);

    mMountable = mountable;
  }

  public static MountableLithoRenderUnit create(
      final Component component,
      final @Nullable ComponentContext context,
      final @Nullable NodeInfo nodeInfo,
      final @Nullable ViewNodeInfo viewNodeInfo,
      final int flags,
      final int importantForAccessibility,
      final @LayoutOutput.UpdateState int updateState,
      final Mountable mountable) {
    final LayoutOutput output =
        new LayoutOutput(
            component, nodeInfo, viewNodeInfo, flags, importantForAccessibility, updateState);

    return new MountableLithoRenderUnit(output, mountable, context);
  }

  @Override
  public ContentAllocator getContentAllocator() {
    return mMountable.getContentAllocator();
  }

  @Override
  protected void mountExtensions(Context context, Object o, @Nullable Object layoutData) {
    mMountable.mountExtensions(context, o, layoutData);
  }

  @Override
  protected void unmountExtensions(Context context, Object o, @Nullable Object layoutData) {
    mMountable.unmountExtensions(context, o, layoutData);
  }

  @Override
  protected void attachExtensions(Context context, Object content, @Nullable Object layoutData) {
    mMountable.attachExtensions(context, content, layoutData);
  }

  @Override
  protected void detachExtensions(Context context, Object content, @Nullable Object layoutData) {
    mMountable.detachExtensions(context, content, layoutData);
  }

  @Nullable
  @Override
  public Map<Class<?>, Extension<?, Object>> getMountUnmountBinderTypeToExtensionMap() {
    return mMountable.getMountUnmountBinderTypeToExtensionMap();
  }

  @Nullable
  @Override
  public List<Extension<?, Object>> getMountUnmountExtensions() {
    return mMountable.getMountUnmountExtensions();
  }

  @Nullable
  @Override
  public Map<Class<?>, Extension<?, Object>> getAttachDetachBinderTypeToExtensionMap() {
    return mMountable.getAttachDetachBinderTypeToExtensionMap();
  }

  @Nullable
  @Override
  public List<Extension<?, Object>> getAttachDetachExtensions() {
    return mMountable.getAttachDetachExtensions();
  }

  @Override
  public Class<?> getRenderContentType() {
    return mMountable.getClass();
  }

  @Override
  public String getDescription() {
    // TODO: have a similar API for Mountable.
    return output.getComponent().getSimpleName();
  }

  public Mountable<?> getMountable() {
    return mMountable;
  }
}
