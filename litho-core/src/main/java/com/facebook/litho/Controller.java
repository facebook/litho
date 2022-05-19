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
import com.facebook.rendercore.RenderUnit;

/**
 * A controller that can be passed as parameter to a {@link MountableComponent} for triggering
 * events from outside of the MountableComponent.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class Controller<ContentT>
    implements RenderUnit.Binder<Mountable<ContentT>, ContentT> {

  @Nullable ContentT mContent;

  public final @Nullable ContentT getContent() {
    return mContent;
  }

  @Override
  public boolean shouldUpdate(
      final Mountable<ContentT> currentMountable,
      final Mountable<ContentT> newMountable,
      final @Nullable Object currentLayoutData,
      final @Nullable Object nextLayoutData) {
    return true;
  }

  @Override
  public void bind(
      final Context context,
      final ContentT contentT,
      final Mountable<ContentT> mountable,
      final @Nullable Object layoutData) {
    mContent = contentT;
  }

  @Override
  public void unbind(
      final Context context,
      final ContentT contentT,
      final Mountable<ContentT> mountable,
      final @Nullable Object layoutData) {
    mContent = null;
  }
}
