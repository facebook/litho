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

import static com.facebook.rendercore.RenderUnit.Extension.extension;

import androidx.annotation.Nullable;
import com.facebook.rendercore.RenderUnit;
import java.util.List;

public class LithoRenderUnitFactory {
  private final @Nullable List<RenderUnit.Binder<LithoRenderUnit, Object>> mMountExtensions;
  private final @Nullable List<RenderUnit.Binder<LithoRenderUnit, Object>> mBindExtensions;
  private final int mMountExtensionsCount;
  private final int mBindExtensionsCount;

  LithoRenderUnitFactory(
      @Nullable List<RenderUnit.Binder<LithoRenderUnit, Object>> mountExtensions,
      @Nullable List<RenderUnit.Binder<LithoRenderUnit, Object>> bindExtensions) {
    mMountExtensions = mountExtensions;
    mMountExtensionsCount = mountExtensions == null ? 0 : mountExtensions.size();
    mBindExtensions = bindExtensions;
    mBindExtensionsCount = bindExtensions == null ? 0 : bindExtensions.size();
  }

  public LithoRenderUnit getRenderUnit(LayoutOutput layoutOutput) {
    final LithoRenderUnit renderUnit = new LithoRenderUnit(layoutOutput);
    for (int i = 0, size = mMountExtensionsCount; i < size; i++) {
      renderUnit.addMountUnmountExtension(extension(renderUnit, mMountExtensions.get(i)));
    }

    for (int i = 0, size = mBindExtensionsCount; i < size; i++) {
      renderUnit.addAttachDetachExtension(extension(renderUnit, mBindExtensions.get(i)));
    }

    return renderUnit;
  }
}
