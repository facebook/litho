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

import androidx.annotation.ColorInt
import com.facebook.primitive.canvas.model.CanvasShadowModel

@JvmInline
value class Shadow internal constructor(@PublishedApi internal val shadowModel: CanvasShadowModel)

/**
 * A shadow which should be drawn under a shape on a Canvas.
 *
 * @param dx horizontal offset
 * @param dy vertical offset
 * @param radius blur radius
 * @param color color of the shadow
 */
fun Shadow(dx: Float, dy: Float, radius: Float, @ColorInt color: Int): Shadow {
  return Shadow(CanvasShadowModel(dx, dy, radius, color))
}
