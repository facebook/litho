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

  @UiThread
  void onTouchEvent(View hostView, MotionEvent motionEvent);

  @UiThread
  void onScrollChanged(View hostView);

  @UiThread
  void onDraw(View hostView);

  @UiThread
  void fling(View hostView);

  @UiThread
  void setListener(@Nullable ScrollStateListener listener);
}
