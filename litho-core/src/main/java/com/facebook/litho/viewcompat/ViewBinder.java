/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.viewcompat;

import android.view.View;

/**
 * Binds data to a view.
 * @param <V> the type of View.
 */
public interface ViewBinder<V extends View> {

  /**
   * Prepares the binder to be bound to a view.
   *
   * Use this method to perform calculations ahead of time and save them.
   */
  void prepare();

  /**
   * Binds data to the given view so it can be rendered on screen. This will always be called
   * after prepare so that you can use stored output from prepare here if needed.
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
