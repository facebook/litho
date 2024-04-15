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

import com.facebook.primitive.canvas.model.CanvasPath
import com.facebook.primitive.canvas.model.CanvasPathAdd
import com.facebook.primitive.canvas.model.CanvasPathArc
import com.facebook.primitive.canvas.model.CanvasPathChildModel
import com.facebook.primitive.canvas.model.CanvasPathCircle
import com.facebook.primitive.canvas.model.CanvasPathClose
import com.facebook.primitive.canvas.model.CanvasPathCubicTo
import com.facebook.primitive.canvas.model.CanvasPathEllipse
import com.facebook.primitive.canvas.model.CanvasPathLineTo
import com.facebook.primitive.canvas.model.CanvasPathModel
import com.facebook.primitive.canvas.model.CanvasPathMoveTo
import com.facebook.primitive.canvas.model.CanvasPathQuadTo
import com.facebook.primitive.canvas.model.CanvasPathRect
import com.facebook.primitive.utils.types.DEFAULT_FILL_RULE
import com.facebook.primitive.utils.types.FillRule
import com.facebook.primitive.utils.types.Point
import com.facebook.primitive.utils.types.Size

@JvmInline
value class Path internal constructor(@PublishedApi internal val pathModel: CanvasPathModel)

/**
 * A path which contains a mathematical description of shapes or lines to be drawn on a Canvas.
 *
 * @param fillRule The path's fill type. Defines how "inside" is computed
 * @param block The lambda callback to issue path commands
 */
fun Path(fillRule: FillRule = DEFAULT_FILL_RULE, block: PathScope.() -> Unit): Path {
  return Path(PathScope().createModel(fillRule, block))
}

class PathScope {
  @PublishedApi internal val children: MutableList<CanvasPathChildModel> = mutableListOf()

  /**
   * Sets the beginning of the next contour to the point (x,y).
   *
   * @param point The start point of a new contour
   */
  inline fun moveTo(point: Point) {
    children.add(CanvasPathMoveTo(point))
  }

  /**
   * Adds a line from the last point to the specified point (x,y). If no [moveTo] children has been
   * added to this Path, then the first point is automatically set to (0,0).
   *
   * @param point The end point of a line
   */
  inline fun lineTo(point: Point) {
    children.add(CanvasPathLineTo(point))
  }

  /**
   * Adds a quadratic bezier from the last point, approaching control point
   * (controlPoint.x,controlPoint.y), and ending at (endPoint.x,endPoint.y). If no [moveTo] children
   * has been added to this Path, then the first point is automatically set to (0,0).
   *
   * @param controlPoint The control point on a quadratic curve
   * @param endPoint The end point on a quadratic curve
   */
  inline fun quadTo(controlPoint: Point, endPoint: Point) {
    children.add(CanvasPathQuadTo(controlPoint, endPoint))
  }

  /**
   * Adds a cubic bezier from the last point, approaching control points
   * (controlPoint1.x,controlPoint1.y) and (controlPoint2.x,controlPoint2.y), and ending at
   * (endPoint.x,endPoint.y). If no [moveTo] children has been added to this Path, then the first
   * point is automatically set to (0,0).
   *
   * @param controlPoint1 The 1st control point on a cubic curve
   * @param controlPoint2 The 2nd control point on a cubic curve
   * @param endPoint The end point on a cubic curve
   */
  inline fun cubicTo(controlPoint1: Point, controlPoint2: Point, endPoint: Point) {
    children.add(CanvasPathCubicTo(controlPoint1, controlPoint2, endPoint))
  }

  /**
   * Adds the specified arc to the path as a new contour.
   *
   * @param center The center of the arc
   * @param radius The radius of the arc
   * @param startDegrees The angle to the starting point of the arc
   * @param endDegrees The angle to the end point of the arc
   * @param clockwise true to make a clockwise arc; false to make a counterclockwise arc
   */
  inline fun arc(
      center: Point,
      radius: Float,
      startDegrees: Float,
      endDegrees: Float,
      clockwise: Boolean = true,
  ) {
    children.add(CanvasPathArc(center, radius, startDegrees, endDegrees, clockwise))
  }

  /**
   * Adds a closed (optionally) round-rectangle contour.
   *
   * @param topLeft The coordinates of the top left point of the rectangle
   * @param size The size of the rectangle
   * @param cornerRadius The radius of the rounded corners
   */
  inline fun rect(topLeft: Point, size: Size, cornerRadius: Float = 0f) {
    children.add(CanvasPathRect(topLeft, size, cornerRadius))
  }

  /**
   * Adds closed oval contour to the contour.
   *
   * @param topLeft The coordinates of the top left point of the ellipse
   * @param size The size of the ellipse
   */
  inline fun ellipse(topLeft: Point, size: Size) {
    children.add(CanvasPathEllipse(topLeft, size))
  }

  /**
   * Adds a closed circle contour to the contour.
   *
   * @param center The center of the circle
   * @param radius The radius of the circle
   */
  inline fun circle(center: Point, radius: Float) {
    children.add(CanvasPathCircle(center, radius))
  }

  /**
   * Closes the current contour. If the current point is not equal to the first point of the
   * contour, a line segment is automatically added.
   */
  inline fun close() {
    children.add(CanvasPathClose)
  }

  /**
   * Adds a copy of src path to the current path, transformed by transformModel.
   *
   * @param src The path to add as a new contour to the current path
   * @param transform The transform that will be applied to the src before adding it to the current
   *   path
   */
  inline fun add(src: Path, transform: Transform = Transform.IDENTITY) {
    children.add(CanvasPathAdd(src.pathModel, transform.transformModel))
  }

  /**
   * Creates the PathModel described by the operations invoked in PathScope.
   *
   * @param fillRule The path's fill type. Defines how "inside" is computed
   * @param block The lambda callback to issue path commands
   * @return The PathModel object created from the operations invoked in this scope.
   */
  inline fun createModel(fillRule: FillRule, block: PathScope.() -> Unit): CanvasPathModel {
    block()
    return CanvasPath(fillRule, children)
  }
}
