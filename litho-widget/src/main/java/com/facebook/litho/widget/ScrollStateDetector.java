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

import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.UiThread;
import javax.annotation.Nullable;

/**
 * Intercepts {@link MotionEvent} and other signals to determine when scrolling has started and
 * stopped, in turn controlling the firing of a {@link ScrollStateListener}.
 */
public interface ScrollStateDetector {

  /**
   * Informs when hostView receives a touch event via onInterceptTouchEvent.
   *
   * @param hostView the host view associated with the touch event
   * @param motionEvent the touch event that was received
   */
  @UiThread
  void onInterceptTouchEvent(View hostView, MotionEvent motionEvent);

  /**
   * Informs when hostView receives a touch event via onTouchEvent.
   *
   * @param hostView the host view associated with the touch event
   * @param motionEvent the touch event that was received
   */
  @UiThread
  void onTouchEvent(View hostView, MotionEvent motionEvent);

  /**
   * Informs when hostView receives a scroll changed event.
   *
   * @param hostView the host view associated with the scroll change
   */
  @UiThread
  void onScrollChanged(View hostView);

  /**
   * Informs when hostView onDraw is called.
   *
   * @param hostView the host view being drawn
   */
  @UiThread
  void onDraw(View hostView);

  /**
   * Informs when hostView receives a fling.
   *
   * @param hostView the host view associated with the fling
   */
  @UiThread
  void fling(View hostView);

  /**
   * Sets a listener that will be called back when this detector detects scroll state changes.
   *
   * @param listener the listener
   */
  @UiThread
  void setListener(@Nullable ScrollStateListener listener);
}
