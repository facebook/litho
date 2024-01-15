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

package com.facebook.litho

import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult
import com.facebook.yoga.YogaMeasureFunction
import com.facebook.yoga.YogaMeasureMode
import com.facebook.yoga.YogaMeasureOutput
import com.facebook.yoga.YogaNode

class LithoYogaMeasureFunction : YogaMeasureFunction {

  override fun measure(
      cssNode: YogaNode,
      width: Float,
      widthMode: YogaMeasureMode,
      height: Float,
      heightMode: YogaMeasureMode
  ): Long {
    @Suppress("UNCHECKED_CAST")
    val context: LayoutContext<LithoLayoutContext> =
        LithoLayoutResult.getLayoutContextFromYogaNode(cssNode)
    val result: LithoLayoutResult = LithoLayoutResult.getLayoutResultFromYogaNode(cssNode)
    val widthSpec: Int = SizeSpec.makeSizeSpecFromCssSpec(width, widthMode)
    val heightSpec: Int = SizeSpec.makeSizeSpecFromCssSpec(height, heightMode)
    val size: MeasureResult = result.measure(context, widthSpec, heightSpec)
    return YogaMeasureOutput.make(size.width, size.height)
  }
}
