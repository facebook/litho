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

package com.facebook.primitive.utils.types

import android.annotation.SuppressLint
import android.graphics.Path
import android.view.View
import com.facebook.proguard.annotations.DoNotStrip

/**
 * This class contains the list of possible layer types for the underlying Canvas View.
 *
 * See [Path.FillType].
 */
@SuppressLint("NotAccessedPrivateField")
@JvmInline
value class CanvasLayerType internal constructor(@DoNotStrip private val value: Int) {
  companion object {
    /**
     * The canvas view does not have a layer.
     *
     * @see android.view.View.LAYER_TYPE_NONE
     */
    val None: CanvasLayerType = CanvasLayerType(0)

    /**
     * The canvas view has a software layer.
     *
     * @see android.view.View.LAYER_TYPE_SOFTWARE
     */
    val Software: CanvasLayerType = CanvasLayerType(1)

    /**
     * The canvas view has a hardware layer.
     *
     * @see android.view.View.LAYER_TYPE_HARDWARE
     */
    val Hardware: CanvasLayerType = CanvasLayerType(2)

    /**
     * Some of the drawing operations require software layer on older versions of Android. This mode
     * will automatically detect that and use [SOFTWARE] type if needed. Otherwise [NONE] type will
     * be used.
     *
     * @see <a
     *   href="https://developer.android.com/topic/performance/hardware-accel#drawing-support">Drawing
     *   Support</a>
     */
    val Auto: CanvasLayerType = CanvasLayerType(-1)
  }

  override fun toString(): String =
      when (this) {
        None -> "None"
        Software -> "Software"
        Hardware -> "Hardware"
        Auto -> "Auto"
        else -> "Unknown"
      }

  fun toLayerType(needsSoftwareLayer: Boolean): Int {
    return when (this) {
      Software -> View.LAYER_TYPE_SOFTWARE
      Hardware -> View.LAYER_TYPE_HARDWARE
      Auto -> if (needsSoftwareLayer) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_NONE
      else -> View.LAYER_TYPE_NONE
    }
  }
}

val DEFAULT_CANVAS_LAYER_TYPE: CanvasLayerType = CanvasLayerType.Auto
