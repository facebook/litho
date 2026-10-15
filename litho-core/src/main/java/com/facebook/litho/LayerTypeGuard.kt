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

package com.facebook.litho

import android.view.View
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.rendercore.LogLevel

/**
 * Guards [View.LAYER_TYPE_HARDWARE] promotions against the device's GPU max texture size.
 *
 * A hardware layer is backed by a single GPU texture; if a view is larger than the device's
 * `GL_MAX_TEXTURE_SIZE`, hwui fails to allocate the layer and aborts at draw time with an
 * UNCATCHABLE native crash ("Unable to create layer for <View>, size WxH max size N"). This object
 * lets the host app supply the device cap so oversized hardware layers are downgraded to
 * [LayerType.LAYER_TYPE_NONE] instead of crashing the app.
 */
object LayerTypeGuard {

  private const val OVERSIZED_HARDWARE_LAYER_CATEGORY = "LayerType:OversizedHardwareLayer"

  /**
   * Max width/height in px allowed for a [LayerType.LAYER_TYPE_HARDWARE] layer before
   * [guardHardwareLayerForSize] downgrades it. The host app should set this to the device's
   * `GL_MAX_TEXTURE_SIZE` (e.g. from a cached `glGetIntegerv(GL_MAX_TEXTURE_SIZE)`) at startup.
   *
   * Default `-1` disables the guard (no behavior change).
   */
  @JvmStatic var maxHardwareLayerSizePx: Int = -1

  /**
   * Returns [requested] unchanged, except a [LayerType.LAYER_TYPE_HARDWARE] request for a [view]
   * whose width or height exceeds [maxHardwareLayerSizePx] (when that is set `> 0`) is downgraded
   * to [LayerType.LAYER_TYPE_NONE], to avoid the uncatchable hwui "Unable to create layer" native
   * abort on views larger than the GPU's `GL_MAX_TEXTURE_SIZE`.
   *
   * NONE — not SOFTWARE — is used on downgrade: a software layer allocates a Bitmap of the view
   * size (e.g. ~71MB for 1088x16384), trading the abort for an OOM. NONE allocates nothing and
   * renders directly; the only cost is that a grouped layer paint (alpha/color filter) is applied
   * per-primitive instead of as a group, which is negligible for an over-max-size view.
   */
  @JvmStatic
  fun guardHardwareLayerForSize(view: View, @LayerType requested: Int): Int {
    val max = maxHardwareLayerSizePx
    if (
        requested == LayerType.LAYER_TYPE_HARDWARE &&
            max > 0 &&
            (view.width > max || view.height > max)
    ) {
      DebugInfoReporter.report(
          category = OVERSIZED_HARDWARE_LAYER_CATEGORY,
          level = LogLevel.ERROR,
      ) {
        put("message", "Downgraded oversized HARDWARE layer to NONE to avoid hwui layer abort")
        put("size", "${view.width}x${view.height}")
        put("maxTextureSizePx", max)
      }
      return LayerType.LAYER_TYPE_NONE
    }
    return requested
  }
}
