/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.graphics.drawable.Drawable;

import com.facebook.components.reference.Reference;

class DrawableComponent<T extends Drawable> extends ComponentLifecycle {

  static DrawableComponent sInstance;

  static synchronized DrawableComponent get() {
    if (sInstance == null) {
      sInstance = new DrawableComponent();
    }

    return sInstance;
  }

  @Override
  protected void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      Component<?> component) {
    final State state = (State) component;

    state.setDrawableWidth(layout.getWidth());
    state.setDrawableHeight(layout.getHeight());
  }

  @Override
  protected Object onCreateMountContent(ComponentContext c) {
    return new MatrixDrawable();
  }

  @Override
  protected void onMount(
      ComponentContext context,
      Object content,
      Component component) {
