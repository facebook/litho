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

import android.annotation.SuppressLint
import android.graphics.Canvas
import androidx.annotation.FloatRange
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.primitive.canvas.CanvasState
import com.facebook.primitive.canvas.model.CanvasDrawIntoCanvas
import com.facebook.primitive.canvas.model.CanvasFill
import com.facebook.primitive.canvas.model.CanvasGroup
import com.facebook.primitive.canvas.model.CanvasLayer
import com.facebook.primitive.canvas.model.CanvasModel
import com.facebook.primitive.canvas.model.CanvasNodeModel
import com.facebook.primitive.canvas.model.CanvasStroke
import com.facebook.primitive.utils.types.BlendingMode
import com.facebook.primitive.utils.types.DEFAULT_BLENDING_MODE
import com.facebook.primitive.utils.types.DEFAULT_LINE_CAP
import com.facebook.primitive.utils.types.DEFAULT_LINE_JOIN
import com.facebook.primitive.utils.types.LineCap
import com.facebook.primitive.utils.types.LineJoin
import com.facebook.primitive.utils.types.Point
import com.facebook.primitive.utils.types.Size
import com.facebook.primitive.utils.types.center

class CanvasScope {
  @PublishedApi
  @DataClassGenerate
  internal data class DrawParams(
      var children: MutableList<CanvasNodeModel> = mutableListOf(),
      var size: Size = Size(0f, 0f)
  )

  @PublishedApi internal val drawParams: DrawParams = DrawParams()

  /** The size of the current drawing scope (group, layer or whole canvas) */
  val size: Size
    inline get() = drawParams.size

  /** The center of the current bounds of the drawing scope (group, layer or whole canvas) */
  val center: Point
    inline get() = drawParams.size.center

  /**
   * Fills the given Shape.
   *
   * @param shape The shape that will be drawn with fill style. A shape can be either a [Path] or
   *   [Shape]. For simple shapes prefer using [Shape] if possible because it offers better drawing
   *   performance.
   * @param shading The shading definition that describes how to fill the path. For example solid
   *   color or a gradient.
   * @param blendingMode The blend mode that will be used when filling the shape
   * @param shadow The shadow that will be drawn below the path
   */
  inline fun fill(
      shape: Shape,
      shading: Shading,
      blendingMode: BlendingMode = DEFAULT_BLENDING_MODE,
      shadow: Shadow? = null
  ) {
    drawParams.children.add(
        CanvasFill(shape.shapeModel, shading.shadingModel, blendingMode, shadow?.shadowModel))
  }

  /**
   * Strokes the given Shape.
   *
   * Note: Strokes are rendered in such a way where half of the stroke width is drawn inside of the
   * shape and half is drawn outside of the shape. Keep this in mind when drawing stroked shapes
   * close to the clip or screen bounds and make sure that the whole stroke width is rendered
   * properly along the entire shape.
   *
   * @param shape The shape that will be drawn with stroke style. A shape can be either a [Path] or
   *   [Shape]. For simple shapes prefer using [Shape] if possible because it offers better drawing
   *   performance.
   * @param shading The shading definition that describes how to stroke the path. For example solid
   *   color or a gradient.
   * @param blendingMode The blend mode that will be used when stroking the shape
   * @param shadow The shadow that will be drawn below the path
   * @param lineWidth The width for stroking. Pass 0 to stroke in hairline mode. Hairlines always
   *   draws a single pixel.
   * @param lineCap The line cap style
   * @param lineJoin The line join style
   * @param miterLimit The stroke miter value. This is used to control the behavior of miter joins
   *   when the joins angle is sharp. This value must be >= 0.
   * @param dashLengths The array of ON and OFF distances. It must contain an even number of entries
   *   (>=2), with the even indices specifying the "on" intervals, and the odd indices specifying
   *   the "off" intervals. This array controls the length of the dashes. The [lineWidth] controls
   *   the thickness of the dashes.
   * @param dashPhase The value which specifies how far into the dash pattern the line starts. For
   *   example, passing a value of 3 means the line is drawn with the dash pattern starting at three
   *   units from its beginning. Passing a value of 0 draws a line starting with the beginning of a
   *   dash pattern. Ignored if dash-lengths is null.
   */
  inline fun stroke(
      shape: Shape,
      shading: Shading,
      blendingMode: BlendingMode = DEFAULT_BLENDING_MODE,
      shadow: Shadow? = null,
      lineWidth: Float = CanvasStroke.DEFAULT_LINE_WIDTH,
      lineCap: LineCap = DEFAULT_LINE_CAP,
      lineJoin: LineJoin = DEFAULT_LINE_JOIN,
      miterLimit: Float = CanvasStroke.DEFAULT_MITER_LIMIT,
      dashLengths: FloatArray? = null,
      dashPhase: Float = 0f
  ) {
    drawParams.children.add(
        CanvasStroke(
            shape.shapeModel,
            shading.shadingModel,
            blendingMode,
            shadow?.shadowModel,
            lineWidth,
            lineCap,
            lineJoin,
            miterLimit,
            dashLengths,
            dashPhase))
  }

  /**
   * Creates a group. Groups can be used to treat multiple drawing commands as a single one and
   * applying transforms or clips to all of them at the same time. For a group, first the transform
   * is applied, then the content is drawn inside the transformed coordinates and then the clip is
   * applied to the drawn content.
   *
   * @param transform The transform that will be applied to the children of the group
   * @param size The size of the group
   * @param clip The path describing the shape of the clip that will be applied to the children of
   *   the group
   * @param clipToBounds The group will be clipped to its bounds if this is set to true and [clip]
   *   is null. Defaults to false.
   * @param block The lambda callback to issue drawing commands
   */
  @SuppressLint("KotlinScopeFunctionsMisuse")
  inline fun group(
      transform: Transform = Transform.IDENTITY,
      size: Size? = null,
      clip: Path? = null,
      clipToBounds: Boolean = false,
      block: CanvasScope.() -> Unit
  ) {
    val groupChildren = mutableListOf<CanvasNodeModel>()
    val (prevChildren, prevSize) = drawParams

    drawParams.apply {
      this.children = groupChildren
      this.size = size ?: prevSize
    }
    block()
    drawParams.apply {
      this.children = prevChildren
      this.size = prevSize
    }

    drawParams.children.add(
        CanvasGroup(
            transform.transformModel,
            size ?: prevSize,
            clip?.pathModel,
            clipToBounds,
            groupChildren))
  }

  /**
   * Creates a layer which allocates and redirects drawing to an offscreen buffer. It may be useful
   * in cases like applying transparency or blending modes to multiple drawing commands at once.
   * Layers should be as small as possible and should be used only when necessary because they may
   * cause performance issues if used incorrectly.
   *
   * @param transform The transform that will be applied to the children of the layer
   * @param size The size of the layer
   * @param clip The path describing the shape of the clip that will be applied to the children of
   *   the layer
   * @param alpha The alpha that will be applied to the layer when compositing it with the parent
   *   layer
   * @param blendingMode The blend mode that will be applied to the layer when compositing it with
   *   the parent layer
   * @param block The lambda callback to issue drawing commands
   */
  @SuppressLint("KotlinScopeFunctionsMisuse")
  inline fun layer(
      transform: Transform = Transform.IDENTITY,
      size: Size? = null,
      clip: Path? = null,
      @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
      blendingMode: BlendingMode = DEFAULT_BLENDING_MODE,
      block: CanvasScope.() -> Unit
  ) {
    val layerChildren = mutableListOf<CanvasNodeModel>()
    val (prevChildren, prevSize) = drawParams

    drawParams.apply {
      this.children = layerChildren
      this.size = size ?: prevSize
    }
    block()
    drawParams.apply {
      this.children = prevChildren
      this.size = prevSize
    }

    drawParams.children.add(
        CanvasLayer(
            transform.transformModel,
            size ?: prevSize,
            clip?.pathModel,
            alpha,
            blendingMode,
            layerChildren))
  }

  /**
   * Provides access to draw directly with the underlying [Canvas]. This is helpful for situations
   * to re-use the existing [Canvas] drawing logic.
   *
   * @param block The lambda callback to issue drawing commands on the provided [Canvas]
   * @see <a
   *   href="https://developer.android.com/topic/performance/hardware-accel#drawing-support">Drawing
   *   Support</a>
   */
  inline fun drawIntoCanvas(noinline block: (Canvas) -> Unit) {
    drawParams.children.add(CanvasDrawIntoCanvas(block))
  }

  /**
   * Draws into the provided [Canvas] with the commands specified in the lambda with this
   * [CanvasScope] as a receiver.
   *
   * @param canvas The target canvas to render into
   * @param size The size relative to the current canvas transform in which the [CanvasScope] should
   *   draw within
   * @param canvasState The canvas state, refer to the documentation of [CanvasState] to learn how
   *   to instantiate it properly.
   * @param block The lambda that is called to issue drawing commands on this [CanvasScope]
   */
  inline fun draw(
      canvas: Canvas,
      size: Size,
      canvasState: CanvasState,
      block: CanvasScope.() -> Unit
  ) {
    val scope = CanvasScope()
    val canvasRootModel = scope.createModel(size, canvasState, block)
    canvasRootModel.draw(canvas)
  }

  inline fun createModel(
      size: Size,
      canvasState: CanvasState,
      block: CanvasScope.() -> Unit
  ): CanvasModel {
    drawParams.size = size
    block()
    return CanvasModel(canvasState, drawParams.children)
  }
}
