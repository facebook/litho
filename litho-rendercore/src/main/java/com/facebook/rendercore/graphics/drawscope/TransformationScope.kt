/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore.graphics.drawscope

import android.graphics.Canvas

/** Scope for building multiple canvas transformations that can be applied together. */
class TransformationScope internal constructor(private val canvas: Canvas) {

  /**
   * Applies a rotation transformation around the canvas origin (0,0)
   *
   * @param degrees The rotation angle in degrees (positive = clockwise).
   */
  fun rotate(degrees: Float) {
    canvas.rotate(degrees)
  }

  /**
   * Applies a rotation transformation around a specific pivot point.
   *
   * @param degrees The rotation angle in degrees (positive = clockwise).
   * @param pivotX The x-coordinate of the pivot point.
   * @param pivotY The y-coordinate of the pivot point.
   */
  fun rotate(degrees: Float, pivotX: Float, pivotY: Float) {
    canvas.rotate(degrees, pivotX, pivotY)
  }

  /**
   * Applies a uniform scale transformation.
   *
   * @param scale The scale factor for both axes.
   */
  fun scale(scale: Float) {
    canvas.scale(scale, scale)
  }

  /**
   * Applies a scale transformation with different factors for each axis.
   *
   * @param scaleX The scale factor for the x-axis.
   * @param scaleY The scale factor for the y-axis.
   */
  fun scale(scaleX: Float, scaleY: Float) {
    canvas.scale(scaleX, scaleY)
  }

  /**
   * Applies a scale transformation around a specific pivot point.
   *
   * @param scaleX The scale factor for the x-axis.
   * @param scaleY The scale factor for the y-axis.
   * @param pivotX The x-coordinate of the pivot point.
   * @param pivotY The y-coordinate of the pivot point.
   */
  fun scale(scaleX: Float, scaleY: Float, pivotX: Float, pivotY: Float) {
    canvas.scale(scaleX, scaleY, pivotX, pivotY)
  }

  /**
   * Applies a translation transformation.
   *
   * @param dx The distance to translate horizontally.
   * @param dy The distance to translate vertically.
   */
  fun translate(dx: Float, dy: Float) {
    canvas.translate(dx, dy)
  }
}
