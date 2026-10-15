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

import android.graphics.Bitmap
import android.graphics.ColorFilter
import android.graphics.MaskFilter
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.RectF
import android.graphics.Shader
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.geometry.Point
import com.facebook.rendercore.graphics.Color

interface DrawScope {

  val center: Point
  val size: IntSize

  fun transform(transformations: TransformationScope.() -> Unit, block: DrawScope.() -> Unit)

  fun drawArc(
      oval: RectF,
      startAngle: Float,
      sweepAngle: Float,
      useCenter: Boolean,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawArc(
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      startAngle: Float,
      sweepAngle: Float,
      useCenter: Boolean,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawBitmap(
      bitmap: Bitmap,
      left: Float,
      top: Float,
      color: Color = Color.fromInt(android.graphics.Color.BLACK),
      drawStyle: DrawStyle = Fill,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawCircle(
      radius: Dimen,
      center: Point,
      color: Color,
      drawStyle: DrawStyle = Fill,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawLine(
      start: Point,
      end: Point,
      color: Color,
      strokeWidth: Dimen,
      strokeCap: Cap? = Cap.BUTT,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawOval(
      oval: RectF,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawOval(
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawPath(
      path: Path,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawPoints(
      pts: FloatArray,
      color: Color,
      strokeWidth: Dimen,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawRect(
      rect: RectF,
      color: Color,
      drawStyle: DrawStyle = Fill,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawRect(
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      color: Color,
      drawStyle: DrawStyle = Fill,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawRoundRect(
      rect: RectF,
      color: Color,
      rx: Float,
      ry: Float = rx,
      drawStyle: DrawStyle = Fill,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawRoundRect(
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      color: Color,
      rx: Float,
      ry: Float = rx,
      drawStyle: DrawStyle = Fill,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
  )

  fun drawText(
      text: String,
      x: Float,
      y: Float,
      color: Color,
      drawStyle: DrawStyle = Fill,
      shader: Shader? = null,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
      textAlign: Paint.Align = Paint.Align.LEFT,
      textScaleX: Float = 1.0f,
      textSize: Float? = null,
      textSkewX: Float = 0f,
  )
}
