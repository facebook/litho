/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.drawable.Drawable;

import com.facebook.litho.reference.Reference;

class DrawableComponent<T extends Drawable> extends ComponentLifecycle {

  static DrawableComponent sInstance;

  static synchronized DrawableComponent get() {
    if (sInstance == null) {
      sInstance = new DrawableComponent();
    }

    return sInstance;
  }

  @Override
