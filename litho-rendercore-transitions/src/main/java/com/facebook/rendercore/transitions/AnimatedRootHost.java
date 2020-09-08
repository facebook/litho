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

package com.facebook.rendercore.transitions;

/**
 * A root {@link com.facebook.rendercore.Host} using the transition extension would need to
 * implement this interface to be able to receive size updates when animating. The size information
 * received in the callbacks should be used in {@link android.view.View#onMeasure(int, int)} to
 * override the size information when animating. This needs to be different from a regular {@link
 * com.facebook.rendercore.Host} where we call measure and layout manually.
 */
public interface AnimatedRootHost {

  /**
   * Sets the width that the root {@link com.facebook.rendercore.Host} should take on the next
   * measure pass and then requests a layout. This should be called from animation-driving code on
   * each frame to animate the size of the root {@link com.facebook.rendercore.Host}.
   */
  void setAnimatedWidth(int width);

  /**
   * Sets the height that the root {@link com.facebook.rendercore.Host} should take on the next
   * measure pass and then requests a layout. This should be called from animation-driving code on
   * each frame to animate the size of the root {@link com.facebook.rendercore.Host}.
   */
  void setAnimatedHeight(int height);
}
