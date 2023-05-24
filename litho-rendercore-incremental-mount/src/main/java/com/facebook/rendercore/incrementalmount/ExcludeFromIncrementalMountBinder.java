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

package com.facebook.rendercore.incrementalmount;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.RenderUnit.Binder;

/**
 * This Binder is only used to hold the information indicating whether the Mountable Component skips
 * Incremental Mount. If this is true then the Component will not be involved in Incremental Mount.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ExcludeFromIncrementalMountBinder implements Binder<RenderUnit<?>, Object, Void> {

  public static final ExcludeFromIncrementalMountBinder INSTANCE =
      new ExcludeFromIncrementalMountBinder();

  private ExcludeFromIncrementalMountBinder() {}

  @Override
  public boolean shouldUpdate(
      RenderUnit<?> currentModel,
      RenderUnit<?> newModel,
      @Nullable Object currentLayoutData,
      @Nullable Object nextLayoutData) {
    return false;
  }

  @Override
  public Void bind(
      Context context, Object o, RenderUnit<?> renderUnit, @Nullable Object layoutData) {
    return null;
  }

  @Override
  public void unbind(
      Context context,
      Object o,
      RenderUnit<?> renderUnit,
      @Nullable Object layoutData,
      Void bindData) {}
}
