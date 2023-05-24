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

package com.facebook.mountable.canvas

import android.content.Context
import android.view.View
import com.facebook.mountable.canvas.model.CanvasModel
import com.facebook.mountable.utils.types.CanvasLayerType
import com.facebook.mountable.utils.types.Size
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.Mountable
import com.facebook.rendercore.RenderUnit

/**
 * A mountable canvas component that can be shared across frameworks built on top of RenderCore such
 * as Litho and Bloks.
 *
 * @param layerType The layer type of the underlying Canvas View. Some of the drawing commands may
 *   require to use [CanvasLayerType.Software] in order work correctly on older the Android
 *   versions.
 * @property modelProvider The lambda that returns an instance of the CanvasModel describing the
 *   drawing commands.
 */
class CanvasMountable(
    layerType: CanvasLayerType,
    private val modelProvider: (canvasSize: Size) -> CanvasModel
) : Mountable<CanvasView>(RenderType.VIEW), ContentAllocator<CanvasView> {

  init {
    addOptionalMountBinder(DelegateBinder.createDelegateBinder(Unit, CANVAS_MODEL_BINDER))
    addOptionalMountBinder(DelegateBinder.createDelegateBinder(layerType, CANVAS_LAYER_TYPE_BINDER))
  }

  override fun createContent(context: Context): CanvasView = CanvasView(context)

  override fun measure(
      context: LayoutContext<*>,
      widthSpec: Int,
      heightSpec: Int,
      previousLayoutData: Any?
  ): MeasureResult {
    val width = fillOrGone(widthSpec)
    val height = fillOrGone(heightSpec)
    val canvasModel = modelProvider(Size(width.toFloat(), height.toFloat()))
    return MeasureResult(width, height, canvasModel)
  }

  override fun getContentAllocator(): ContentAllocator<CanvasView> {
    return this
  }

  private fun fillOrGone(measureSpec: Int): Int {
    val defaultSize = 0

    val specMode = View.MeasureSpec.getMode(measureSpec)
    val specSize = View.MeasureSpec.getSize(measureSpec)
    return when (specMode) {
      View.MeasureSpec.AT_MOST,
      View.MeasureSpec.EXACTLY -> specSize
      View.MeasureSpec.UNSPECIFIED -> defaultSize
      else -> defaultSize
    }
  }
}

private val CANVAS_MODEL_BINDER =
    object : RenderUnit.Binder<Unit, CanvasView, Any?> {
      override fun shouldUpdate(
          currentModel: Unit,
          newModel: Unit,
          currentLayoutData: Any?,
          nextLayoutData: Any?
      ): Boolean {
        val currentCanvasModel = currentLayoutData as CanvasModel
        val newCanvasModel = nextLayoutData as CanvasModel
        return currentCanvasModel != newCanvasModel
      }

      override fun bind(
          context: Context,
          content: CanvasView,
          model: Unit,
          layoutData: Any?
      ): Any? {
        content.canvasModel = layoutData as CanvasModel
        return null
      }

      override fun unbind(
          context: Context,
          content: CanvasView,
          model: Unit,
          layoutData: Any?,
          bindData: Any?
      ) {
        content.canvasModel = null
      }
    }

private val CANVAS_LAYER_TYPE_BINDER =
    object : RenderUnit.Binder<CanvasLayerType, CanvasView, Any?> {
      override fun shouldUpdate(
          currentModel: CanvasLayerType,
          newModel: CanvasLayerType,
          currentLayoutData: Any?,
          nextLayoutData: Any?
      ): Boolean {
        return currentModel != newModel
      }

      override fun bind(
          context: Context,
          content: CanvasView,
          model: CanvasLayerType,
          layoutData: Any?
      ): Any? {
        val canvasModel = layoutData as CanvasModel
        val layerType = model.toLayerType(canvasModel.needsSoftwareLayer())
        if (content.layerType != layerType) {
          content.setLayerType(layerType, null)
        }
        return null
      }

      override fun unbind(
          context: Context,
          content: CanvasView,
          model: CanvasLayerType,
          layoutData: Any?,
          bindData: Any?
      ) {
        content.setLayerType(View.LAYER_TYPE_NONE, null)
      }
    }
