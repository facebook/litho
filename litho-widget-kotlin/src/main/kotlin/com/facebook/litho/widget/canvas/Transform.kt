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

import com.facebook.primitive.canvas.model.CanvasInverseTransform
import com.facebook.primitive.canvas.model.CanvasRotate
import com.facebook.primitive.canvas.model.CanvasScale
import com.facebook.primitive.canvas.model.CanvasSkew
import com.facebook.primitive.canvas.model.CanvasTransform
import com.facebook.primitive.canvas.model.CanvasTransformChildModel
import com.facebook.primitive.canvas.model.CanvasTransformModel
import com.facebook.primitive.canvas.model.CanvasTranslate
import com.facebook.primitive.utils.types.Point

/** A transform which holds a 3x3 matrix for transforming coordinates. */
@JvmInline
value class Transform
internal constructor(@PublishedApi internal val transformModel: CanvasTransformModel) {
  companion object {
    val IDENTITY: Transform = Transform(CanvasTransform.IDENTITY)
  }
}

/**
 * Creates a transform which holds a 3x3 matrix for transforming coordinates.
 *
 * @param a The horizontal scaling. A value of 1 results in no scaling
 * @param b The vertical skewing
 * @param c The horizontal skewing
 * @param d The vertical scaling. A value of 1 results in no scaling
 * @param tx The horizontal translation (moving)
 * @param ty The vertical translation (moving)
 * @param block The lambda callback to issue transform commands
 */
fun Transform(
    a: Float = 1f,
    b: Float = 0f,
    c: Float = 0f,
    d: Float = 1f,
    tx: Float = 0f,
    ty: Float = 0f,
    block: TransformScope.() -> Unit
): Transform {
  val scope = TransformScope()
  block(scope)
  return Transform(CanvasTransform(a, b, c, d, tx, ty, scope.children))
}

class TransformScope {
  val children: MutableList<CanvasTransformChildModel> = mutableListOf()

  /**
   * Inverts the specified transform.
   *
   * @param a The horizontal scaling. A value of 1 results in no scaling
   * @param b The vertical skewing
   * @param c The horizontal skewing
   * @param d The vertical scaling. A value of 1 results in no scaling
   * @param tx The horizontal translation (moving)
   * @param ty The vertical translation (moving)
   * @param block The lambda callback to issue transform commands
   */
  inline fun inverse(
      a: Float = 1f,
      b: Float = 0f,
      c: Float = 0f,
      d: Float = 1f,
      tx: Float = 0f,
      ty: Float = 0f,
      block: TransformScope.() -> Unit
  ) {
    val scope = TransformScope()
    block(scope)
    children.add(CanvasInverseTransform(CanvasTransform(a, b, c, d, tx, ty, scope.children)))
  }

  /**
   * Postconcats the current transform with the specified translation.
   *
   * M' = T(dx, dy) * M
   *
   * @param dx The horizontal translation
   * @param dy The vertical translation
   */
  inline fun translate(dx: Float = 0f, dy: Float = 0f) {
    children.add(CanvasTranslate(dx, dy))
  }

  /**
   * Postconcats the current transform with the specified scale.
   *
   * M' = S(sx, sy, pivotX, pivotY) * M
   *
   * @param sx The horizontal scale
   * @param sy The vertical scale
   * @param pivot The point around which scale will be applied
   */
  inline fun scale(sx: Float = 1f, sy: Float = 1f, pivot: Point = Point(0f, 0f)) {
    children.add(CanvasScale(sx, sy, pivot))
  }

  /**
   * Postconcats the current transform with the specified rotation.
   *
   * M' = R(degrees, pivotX, pivotY) * M
   *
   * @param degrees The rotation degrees
   * @param pivot The point around which scale will be applied
   */
  inline fun rotate(degrees: Float = 0f, pivot: Point = Point(0f, 0f)) {
    children.add(CanvasRotate(degrees, pivot))
  }

  /**
   * Postconcats the current transform with the specified skewing.
   *
   * M' = K(kx, ky, pivotX, pivotY) * M
   *
   * @param kx The horizontal skewing
   * @param ky The vertical skewing
   * @param pivot The point around which scale will be applied
   */
  inline fun skew(kx: Float = 0f, ky: Float = 0f, pivot: Point = Point(0f, 0f)) {
    children.add(CanvasSkew(kx, ky, pivot))
  }
}
