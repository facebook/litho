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

import android.widget.FrameLayout
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.LithoView
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Size
import com.facebook.litho.useState
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import kotlin.math.max

/** Renders the given component in a nested LithoView. */
class SimpleNestedTreeComponent(private val contentComponent: Component) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val componentTree = useState {
      ComponentTree.create(ComponentContext.makeCopyForNestedTree(context))
          .incrementalMount(false)
          .build()
    }
    return LithoPrimitive(
        layoutBehavior = SimpleNestedTreeLayoutBehavior(contentComponent, componentTree.value),
        mountBehavior =
            MountBehavior(
                ViewAllocator { context ->
                  // FrameLayout is used here due to some bugs around having a LithoView as the
                  // direct content type.
                  val frameLayout = FrameLayout(context)
                  frameLayout.addView(
                      LithoView(context),
                      FrameLayout.LayoutParams.MATCH_PARENT,
                      FrameLayout.LayoutParams.MATCH_PARENT)
                  frameLayout
                }) {
                  bind(componentTree.value) { content ->
                    val lithoView = content.getChildAt(0) as LithoView
                    lithoView.componentTree = componentTree.value
                    onUnbind { lithoView.componentTree = null }
                  }
                },
        style = null)
  }
}

private class SimpleNestedTreeLayoutBehavior(
    private val contentComponent: Component,
    private val componentTree: ComponentTree
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val size = Size()
    componentTree.setRootAndSizeSpecSync(
        contentComponent, sizeConstraints.toWidthSpec(), sizeConstraints.toHeightSpec(), size)
    return PrimitiveLayoutResult(
        width = max(sizeConstraints.minWidth, size.width),
        height = max(sizeConstraints.minHeight, size.height))
  }
}
