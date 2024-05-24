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

package com.facebook.litho.widget.zoomable

import com.facebook.litho.Component
import com.facebook.litho.LayoutState
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.NestedLithoTree
import com.facebook.litho.NestedLithoTreeState
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.ResolveResult
import com.facebook.litho.Style
import com.facebook.litho.bindToRenderTreeView
import com.facebook.litho.useCached
import com.facebook.litho.useNestedTree
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import kotlin.math.max

class ZoomableComponent(private val style: Style? = null, private val child: () -> Component) :
    PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {

    val (
        state: NestedLithoTreeState,
        resolveResult: ResolveResult,
    ) = useNestedTree(root = child(), treeProps = context.treePropContainer)

    val controller = useCached { ZoomableController() }
    return LithoPrimitive(
        layoutBehavior =
            ZoomableLayoutBehavior(
                resolveResult = resolveResult,
            ),
        mountBehavior =
            MountBehavior(ALLOCATOR) {
              bindToRenderTreeView(state = state) { renderTreeView }

              bind(controller) { content ->
                controller.bindView(content)

                onUnbind { controller.bindView(null) }
              }
            },
        style = style)
  }

  companion object {
    private val ALLOCATOR: ViewAllocator<LithoZoomableView> = ViewAllocator { context ->
      LithoZoomableView(context)
    }
  }
}

internal class ZoomableLayoutBehavior(
    private val resolveResult: ResolveResult,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {

    val constraints: SizeConstraints = sizeConstraints.copy(minWidth = 0, minHeight = 0)

    val layoutState =
        NestedLithoTree.layout(
            result = resolveResult,
            sizeConstraints = constraints,
            current = previousLayoutData as LayoutState?,
        )

    // Ensure that width and height are not less than 0
    val width: Int = max(sizeConstraints.minWidth, layoutState.width)
    val height: Int = max(sizeConstraints.minHeight, layoutState.height)

    layoutState.toRenderTree()

    return PrimitiveLayoutResult(width = width, height = height, layoutData = layoutState)
  }
}
