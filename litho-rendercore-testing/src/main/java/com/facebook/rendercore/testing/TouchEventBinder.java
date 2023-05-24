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

package com.facebook.rendercore.testing;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.RenderUnit;

public class TouchEventBinder implements RenderUnit.Binder<ViewWrapperUnit, View, Void> {

  private final View.OnTouchListener mListener;

  public TouchEventBinder(final View.OnTouchListener listener) {
    mListener = listener;
  }

  @Override
  public boolean shouldUpdate(
      final ViewWrapperUnit currentModel,
      final ViewWrapperUnit newModel,
      final @Nullable Object currentLayoutData,
      final @Nullable Object nextLayoutData) {
    return true;
  }

  @Override
  public Void bind(
      final Context context,
      final View view,
      final ViewWrapperUnit binder,
      final @Nullable Object layoutData) {
    view.setOnTouchListener(mListener);
    return null;
  }

  @Override
  public void unbind(
      final Context context,
      final View view,
      final ViewWrapperUnit binder,
      final @Nullable Object layoutData,
      final Void bindData) {
    view.setOnTouchListener(null);
  }
}
