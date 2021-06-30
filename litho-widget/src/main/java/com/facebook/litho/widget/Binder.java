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

package com.facebook.litho.widget;

import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Size;
import javax.annotation.Nullable;

/**
 * This binder class is used to asynchronously layout Components given a list of {@link Component}
 * and attaching them to a {@link ViewGroup} through the {@link #bind(ViewGroup)} method.
 */
public interface Binder<V extends ViewGroup> {

  /**
   * Set the width and height of the {@link View} that will be passed to the subsequent {@link
   * #mount(ViewGroup)}, {@link #bind(ViewGroup)} and {@link #unmount(ViewGroup)} calls. Can be
   * called by any thread.
   *
   * @param width Usually the view width minus horizontal padding.
   * @param height Usually the view height minus vertical padding.
   */
  void setSize(int width, int height);

  /** Measure the content of this Binder. Call this method from the Component's onMeasure. */
  void measure(
      Size outSize,
      int widthSpec,
      int heightSpec,
      EventHandler<ReMeasureEvent> reMeasureEventHandler);

  /** Returns the component at the given position in the binder. */
  ComponentTree getComponentAt(int position);

  /**
   * Call this method before the {@link View} is mounted, i.e. within {@link
   * com.facebook.litho.Component#onMount(ComponentContext, Object)})}
   */
  void mount(V view);

  /**
   * Bind this {@link Binder} to a {@link View}. Remember to call {@link
   * ViewGroup#notifyDataSetChanged()} when your {@link Component}s are ready to be used.
   */
  void bind(V view);

  /**
   * Call this method when the view is unbound.
   *
   * @param view the view being unbound.
   */
  void unbind(V view);

  /**
   * Call this method when the view is unmounted.
   *
   * @param view the view being unmounted.
   */
  void unmount(V view);

  /**
   * Bind a {@link ViewportInfo.ViewportChanged} listener to this {@link Binder}. The listener will
   * be notified of Viewport changes.
   *
   * @param viewportChangedListener
   */
  void setViewportChangedListener(@Nullable ViewportInfo.ViewportChanged viewportChangedListener);

  /** Return true if wrap content is enabled, false otherwise. */
  boolean isWrapContent();

  /**
   * Only for horizontally scrolling layouts: return true if height is not known when measuring the
   * view and the first item will be measured to determine the height.
   */
  boolean canMeasure();

  /**
   * Only for horizontally scrolling layouts: set to true if height is not known when measuring the
   * view and the first item will need to be measured to determine the height of the view.
   */
  void setCanMeasure(boolean canMeasure);

  /** Detach items under the hood. */
  void detach();
}
