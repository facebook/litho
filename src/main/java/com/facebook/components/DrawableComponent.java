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
    MatrixDrawable drawable = (MatrixDrawable) content;
    final State<T> state = (State) component;

    drawable.mount(Reference.acquire(context, state.getDrawable()));
  }

  @Override
  protected void onBind(
      ComponentContext c,
      Object mountedContent,
      Component<?> component) {
    final MatrixDrawable mountedDrawable = (MatrixDrawable) mountedContent;
    final State state = (State) component;

    mountedDrawable.bind(state.getDrawableWidth(), state.getDrawableHeight());
  }

  @Override
  protected void onUnmount(
      ComponentContext context,
      Object mountedContent,
      Component<?> component) {
    final State state = (State) component;

