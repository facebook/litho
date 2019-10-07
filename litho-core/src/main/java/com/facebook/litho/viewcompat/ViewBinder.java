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

/**
 * Binds data to a view.
 *
 * @param <V> the type of View.
 */
public interface ViewBinder<V extends View> {

  /**
   * Prepares the binder to be bound to a view.
   *
   * <p>Use this method to perform calculations ahead of time and save them.
   */
  void prepare();

  /**
   * Binds data to the given view so it can be rendered on screen. This will always be called after
   * prepare so that you can use stored output from prepare here if needed.
   *
   * @param view the view to bind.
   */
  void bind(V view);

  /**
   * Cleans up a view that goes off screen after it has already been bound.
   *
   * @param view the view to unbind.
   */
  void unbind(V view);
}
