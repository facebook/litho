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

import androidx.annotation.ColorInt
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate

/**
 * A definition for a shadow which should be drawn under a shape on a Canvas.
 *
 * @property dx horizontal offset
 * @property dy vertical offset
 * @property radius blur radius
 * @property color color of the shadow
 */
@DataClassGenerate
data class CanvasShadowModel(
    val dx: Float,
    val dy: Float,
    val radius: Float,
    @ColorInt val color: Int
)
