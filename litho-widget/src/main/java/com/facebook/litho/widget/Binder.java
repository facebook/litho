/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import javax.annotation.Nullable;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Size;

/**
 * This binder class is used to asynchronously layout Components given a list of
 * {@link Component} and attaching them to a {@link ViewGroup} through the
 * {@link #bind(ViewGroup)} method.
 */
public interface Binder<V extends ViewGroup> {

  /**
   * Set the width and height of the {@link View} that will be passed to the subsequent
   * {@link #mount(ViewGroup)}, {@link #bind(ViewGroup)} and {@link #unmount(ViewGroup)} calls.
   * Can be called by any thread.
   *
   * @param width Usually the view width minus horizontal padding.
   * @param height Usually the view height minus vertical padding.
   */
  void setSize(int width, int height);

  /**
   * Measure the content of this Binder. Call this method from the Component's onMeasure.
   */
  void measure(
      Size outSize,
      int widthSpec,
      int heightSpec,
      EventHandler<ReMeasureEvent> reMeasureEventHandler);

  /**
   * Returns the component at the given position in the binder.
   */
  ComponentTree getComponentAt(int position);

  /**
   * Call this method before the {@link View} is mounted, i.e. within
   * {@link com.facebook.litho.ComponentLifecycle#onMount(Context, Object, Component)}
   */
  void mount(V view);

  /**
   * Bind this {@link Binder} to a {@link View}. Remember to call
   * {@link #notifyDataSetChanged()} when your {@link Component}s are
   * ready to be used.
   */
  void bind(V view);

  /**
   * Call this method when the view is unbound.
   * @param view the view being unbound.
   */
  void unbind(V view);

  /**
   * Call this method when the view is unmounted.
   * @param view the view being unmounted.
   */
  void unmount(V view);

  /**
   * Bind a {@link ViewportInfo.ViewportChanged} listener to this {@link Binder}.
   * The listener will be notified of Viewport changes.
   * @param viewportChangedListener
   */
  void setViewportChangedListener(@Nullable ViewportInfo.ViewportChanged viewportChangedListener);
}
