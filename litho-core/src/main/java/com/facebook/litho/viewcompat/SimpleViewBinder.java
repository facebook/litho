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
 * Empty implementation of {@link com.facebook.litho.viewcompat.ViewBinder}. This can be useful if
 * we need to override only one method.
 */
public class SimpleViewBinder<V extends View> implements ViewBinder<V> {

  @Override
  public void prepare() {}

  @Override
  public void bind(V view) {}

  @Override
  public void unbind(V view) {}
}
