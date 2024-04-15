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

import com.facebook.primitive.canvas.model.CanvasShape
import com.facebook.primitive.canvas.model.CanvasShapeArc
import com.facebook.primitive.canvas.model.CanvasShapeCircle
import com.facebook.primitive.canvas.model.CanvasShapeEllipse
import com.facebook.primitive.canvas.model.CanvasShapeLine
import com.facebook.primitive.canvas.model.CanvasShapeRect
import com.facebook.primitive.utils.types.Point
import com.facebook.primitive.utils.types.Size

@JvmInline
value class Shape private constructor(@PublishedApi internal val shapeModel: CanvasShape) {
  companion object {

    /**
     * A line segment with the specified start and stop x,y coordinates that can be drawn on a
     * Canvas.
     *
     * @param topLeft The coordinates of the top left point of the rectangle
     * @param size The size of the rectangle
     * @param cornerRadius The radius of the rounded corners
     */
    fun line(startPoint: Point, endPoint: Point): Shape {
      return Shape(CanvasShapeLine(startPoint, endPoint))
    }

    /**
     * A closed (optionally) round-rectangle that can be drawn on a Canvas.
     *
     * @param topLeft The coordinates of the top left point of the rectangle
     * @param size The size of the rectangle
     * @param cornerRadius The radius of the rounded corners
     */
    fun rect(topLeft: Point, size: Size, cornerRadius: Float = 0f): Shape {
      return Shape(CanvasShapeRect(topLeft, size, cornerRadius))
    }

    /**
     * A closed circle that can be drawn on a Canvas.
     *
     * @param center The center of the circle
     * @param radius The radius of the circle
     */
    fun circle(center: Point, radius: Float): Shape {
      return Shape(CanvasShapeCircle(center, radius))
    }

    /**
     * An arc shape that can be drawn on a Canvas.
     *
     * @param center The center of the arc
     * @param radius The radius of the arc
     * @param startDegrees The angle to the starting point of the arc
     * @param endDegrees The angle to the end point of the arc
     * @param clockwise true to make a clockwise arc; false to make a counterclockwise arc
     */
    fun arc(
        center: Point,
        radius: Float,
        startDegrees: Float,
        endDegrees: Float,
        clockwise: Boolean = true,
    ): Shape {
      return Shape(CanvasShapeArc(center, radius, startDegrees, endDegrees, clockwise))
    }

    /**
     * A closed ellipse that can be drawn on a Canvas.
     *
     * @param topLeft The coordinates of the top left point of the ellipse
     * @param size The size of the ellipse
     */
    fun ellipse(topLeft: Point, size: Size): Shape {
      return Shape(CanvasShapeEllipse(topLeft, size))
    }

    /**
     * A path which contains a mathematical description of shapes or lines to be drawn on a Canvas.
     *
     * @param path The path object
     */
    fun path(path: Path): Shape {
      return Shape(path.pathModel)
    }
  }
}
