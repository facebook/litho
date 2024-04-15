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

package com.facebook.litho.widget.canvas

import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.useCached
import com.facebook.primitive.canvas.CanvasPrimitive
import com.facebook.primitive.canvas.CanvasState
import com.facebook.primitive.utils.types.CanvasLayerType
import com.facebook.primitive.utils.types.DEFAULT_CANVAS_LAYER_TYPE
import com.facebook.primitive.utils.types.Size

/**
 * The Canvas Component which provides a means for drawing 2D graphics.
 *
 * @property layerType The layer type of the underlying Canvas View. Some of the drawing commands
 *   may require to use [CanvasLayerType.Software] in order work correctly on older the Android
 *   versions. Default is [CanvasLayerType.Auto]
 * @property matrixPoolSize The Matrix pool max size
 * @property pathCacheSize The Path cache max size
 * @property block The lambda callback to issue drawing commands
 */
class CanvasComponent(
    private val layerType: CanvasLayerType = DEFAULT_CANVAS_LAYER_TYPE,
    private val matrixPoolSize: Int = CanvasState.DEFAULT_MATRIX_POOL_SIZE,
    private val pathCacheSize: Int = CanvasState.DEFAULT_PATH_CACHE_SIZE,
    private val style: Style? = null,
    private val block: CanvasScope.() -> Unit
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val canvasState = useCached { CanvasState(matrixPoolSize, pathCacheSize) }

    return LithoPrimitive(
        primitive =
            CanvasPrimitive(
                id = createPrimitiveId(),
                layerType = layerType,
                modelProvider = { canvasSize: Size ->
                  CanvasScope().createModel(canvasSize, canvasState, block)
                }),
        style = style)
  }
}
