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

import static com.facebook.rendercore.RenderUnit.Extension.extension;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.MountItemsPool;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class MountableLithoRenderUnit extends LithoRenderUnit implements ContentAllocator {

  private final Mountable<?> mMountable;

  private MountableLithoRenderUnit(
      final long id,
      final LayoutOutput output,
      final Mountable mountable,
      final @Nullable ComponentContext context) {
    super(id, output, mountable.getRenderType(), context);
    this.mMountable = mountable;

    addMountUnmountExtentions(mMountable);
  }

  private void addMountUnmountExtentions(final Mountable mountable) {
    List<Binder> binders = mountable.getBinders();
    if (binders != null) {
      for (Binder binder : binders) {
        addMountUnmountExtensions(extension(mMountable, binder));
      }
    }
  }

  public static MountableLithoRenderUnit create(
      final long id,
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

    return new MountableLithoRenderUnit(id, output, mountable, context);
  }

  @Override
  public Object createContent(Context c) {
    return mMountable.createContent(c);
  }

  @Override
  public Object getPoolableContentType() {
    return getRenderContentType();
  }

  @Override
  public ContentAllocator getContentAllocator() {
    return this;
  }

  @Override
  public Class<?> getRenderContentType() {
    return mMountable.getClass();
  }

  @Override
  public MountItemsPool.ItemPool createRecyclingPool() {
    return mMountable.onCreateMountContentPool();
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
