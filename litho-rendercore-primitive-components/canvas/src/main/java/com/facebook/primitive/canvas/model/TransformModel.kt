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

package com.facebook.primitive.canvas.model

import android.annotation.SuppressLint
import android.graphics.Matrix
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.primitive.canvas.values
import com.facebook.primitive.utils.types.Point

sealed interface CanvasTransformModel {
  fun applyTo(matrix: Matrix)
}

sealed interface CanvasTransformChildModel {
  fun applyTo(matrix: Matrix)
}

/**
 * A definition for a transform which holds a 3x3 matrix for transforming coordinates.
 *
 * @property a The horizontal scaling. A value of 1 results in no scaling
 * @property b The vertical skewing
 * @property c The horizontal skewing
 * @property d The vertical scaling. A value of 1 results in no scaling
 * @property tx The horizontal translation (moving)
 * @property ty The vertical translation (moving)
 * @property children The child transforms which will be post applied in the same order as they're
 *   specified
 */
@SuppressLint("NotInvokedPrivateMethod")
@DataClassGenerate
data class CanvasTransform(
    private val a: Float,
    private val b: Float,
    private val c: Float,
    private val d: Float,
    private val tx: Float,
    private val ty: Float,
    private val children: List<CanvasTransformChildModel>
) : CanvasTransformModel {
  override fun applyTo(matrix: Matrix) {
    val matrixValues = matrix.values
    matrixValues[Matrix.MSCALE_X] = a
    matrixValues[Matrix.MSKEW_X] = c
    matrixValues[Matrix.MTRANS_X] = tx
    matrixValues[Matrix.MSKEW_Y] = b
    matrixValues[Matrix.MSCALE_Y] = d
    matrixValues[Matrix.MTRANS_Y] = ty
    matrix.values = matrixValues

    for (i in children.indices) {
      children[i].applyTo(matrix)
    }
  }

  companion object {
    val IDENTITY: CanvasTransform =
        CanvasTransform(a = 1f, b = 0f, c = 0f, d = 1f, tx = 0f, ty = 0f, children = listOf())
  }
}

/**
 * A definition for a child transform which inverts the specified transform.
 *
 * @property transform The transform that should be inverted
 * @throws IllegalArgumentException If the specified transform can't be inverted
 */
@SuppressLint("NotInvokedPrivateMethod")
@DataClassGenerate
data class CanvasInverseTransform(private val transform: CanvasTransformModel) :
    CanvasTransformChildModel {
  override fun applyTo(matrix: Matrix) {
    transform.applyTo(matrix)
    if (!matrix.invert(matrix)) {
      throw IllegalArgumentException("Can't invert matrix: ${matrix.toShortString()} ")
    }
  }
}

/**
 * A definition for a child transform which postconcats the current transform with the specified
 * translation. M' = T(dx, dy) * M
 *
 * @property dx The horizontal translation
 * @property dy The vertical translation
 */
@SuppressLint("NotInvokedPrivateMethod")
@DataClassGenerate
data class CanvasTranslate(private val dx: Float, private val dy: Float) :
    CanvasTransformChildModel {
  override fun applyTo(matrix: Matrix) {
    matrix.postTranslate(dx, dy)
  }
}

/**
 * A definition for a child transform which postconcats the current transform with the specified
 * scale. M' = S(sx, sy, pivot.x, pivot.y) * M
 *
 * @property sx The horizontal scale
 * @property sy The vertical scale
 * @property pivot The point around which scale will be applied
 */
@SuppressLint("NotInvokedPrivateMethod")
@DataClassGenerate
data class CanvasScale(
    private val sx: Float,
    private val sy: Float,
    private val pivot: Point,
) : CanvasTransformChildModel {
  override fun applyTo(matrix: Matrix) {
    matrix.postScale(sx, sy, pivot.x, pivot.y)
  }
}

/**
 * A definition for a child transform which postconcats the current transform with the specified
 * rotation. M' = R(degrees, pivot.x, pivot.y) * M
 *
 * @property degrees The rotation degrees
 * @property pivot The point around which scale will be applied
 */
@SuppressLint("NotInvokedPrivateMethod")
@DataClassGenerate
data class CanvasRotate(
    private val degrees: Float,
    private val pivot: Point,
) : CanvasTransformChildModel {
  override fun applyTo(matrix: Matrix) {
    matrix.postRotate(degrees, pivot.x, pivot.y)
  }
}

/**
 * A definition for a child transform which postconcats the current transform with the specified
 * skewing. M' = K(kx, ky, pivot.x, pivot.y) * M
 *
 * @property kx The horizontal skewing
 * @property ky The vertical skewing
 * @property pivot The point around which scale will be applied
 */
@SuppressLint("NotInvokedPrivateMethod")
@DataClassGenerate
data class CanvasSkew(
    private val kx: Float,
    private val ky: Float,
    private val pivot: Point,
) : CanvasTransformChildModel {
  override fun applyTo(matrix: Matrix) {
    matrix.postSkew(kx, ky, pivot.x, pivot.y)
  }
}
