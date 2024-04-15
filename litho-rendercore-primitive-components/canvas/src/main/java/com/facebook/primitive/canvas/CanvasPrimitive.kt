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

package com.facebook.primitive.canvas

import com.facebook.primitive.canvas.model.CanvasModel
import com.facebook.primitive.utils.types.CanvasLayerType
import com.facebook.primitive.utils.types.Size as CanvasSize
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.utils.fillSpace

/**
 * A function that returns Primitive canvas component that can be shared across frameworks built on
 * top of RenderCore such as Litho and Bloks.
 *
 * @param id Unique id identifying the [RenderUnit] in the tree of Node it is part of.
 * @param layerType The layer type of the underlying Canvas View. Some of the drawing commands may
 *   require to use [CanvasLayerType.Software] in order work correctly on older the Android
 *   versions.
 * @param modelProvider The lambda that returns an instance of the CanvasModel describing the
 *   drawing commands.
 * @return a Primitive canvas component.
 */
@Suppress("FunctionName")
fun CanvasPrimitive(
    id: Long,
    layerType: CanvasLayerType,
    modelProvider: (canvasSize: CanvasSize) -> CanvasModel
): Primitive {
  return Primitive(
      layoutBehavior = CanvasLayoutBehavior(modelProvider),
      mountBehavior =
          MountBehavior(id, ViewAllocator { context -> CanvasView(context) }) {
            bindWithLayoutData<CanvasModel> { content, canvasModel ->
              content.canvasModel = canvasModel
              onUnbind { content.canvasModel = null }
            }
            bindWithLayoutData<CanvasModel>(layerType) { content, canvasModel ->
              val defaultLayerType = content.layerType
              val canvasLayerType = layerType.toLayerType(canvasModel.needsSoftwareLayer())
              if (content.layerType != canvasLayerType) {
                content.setLayerType(canvasLayerType, null)
              }
              onUnbind {
                if (content.layerType != defaultLayerType) {
                  content.setLayerType(defaultLayerType, null)
                }
              }
            }
          })
}

private class CanvasLayoutBehavior(
    private val modelProvider: (canvasSize: CanvasSize) -> CanvasModel
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val size = Size.fillSpace(sizeConstraints, fallbackWidth = 0, fallbackHeight = 0)
    val canvasModel = modelProvider(CanvasSize(size.width.toFloat(), size.height.toFloat()))
    return PrimitiveLayoutResult(size = size, layoutData = canvasModel)
  }
}
