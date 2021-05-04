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

package com.facebook.litho.viewcompat;

import android.view.View;
import com.facebook.infer.annotation.Nullsafe;

/**
 * Empty implementation of {@link com.facebook.litho.viewcompat.ViewBinder}. This can be useful if
 * we need to override only one method.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class SimpleViewBinder<V extends View> implements ViewBinder<V> {

  @Override
  public void prepare() {}

  @Override
  public void bind(V view) {}

  @Override
  public void unbind(V view) {}
}
