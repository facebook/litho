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

package com.facebook.litho.widget

import android.view.View
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.RenderAndMeasureCounter
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.litho.widget.RenderAndLayoutCountingTester.Listener
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec

class RenderAndLayoutCountingTester(
    private val renderAndMeasureCounter: RenderAndMeasureCounter,
    private val listener: Listener? = null,
    private val style: Style? = null,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    listener?.onPrepare()
    renderAndMeasureCounter.incrementRenderCount()

    return LithoPrimitive(
        layoutBehavior =
            RenderAndLayoutCountingTesterLayoutBehavior(renderAndMeasureCounter, listener),
        mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
        style = style,
    )
  }

  interface Listener {
    fun onPrepare()

    fun onMeasure()
  }
}

private class RenderAndLayoutCountingTesterLayoutBehavior(
    private val renderAndMeasureCounter: RenderAndMeasureCounter,
    private val listener: Listener?,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    listener?.onMeasure()
    renderAndMeasureCounter.incrementMeasureCount()
    return PrimitiveLayoutResult(
        width = SizeSpec.getSize(sizeConstraints.toWidthSpec()),
        height = SizeSpec.getSize(sizeConstraints.toHeightSpec()),
    )
  }
}
