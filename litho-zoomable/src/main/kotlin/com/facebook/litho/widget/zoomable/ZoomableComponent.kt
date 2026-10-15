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
import com.facebook.litho.TouchEvent
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.bindToRenderTreeView
import com.facebook.litho.layoutVersion
import com.facebook.litho.useCached
import com.facebook.litho.useNestedTree
import com.facebook.litho.view.onInterceptTouch
import com.facebook.litho.view.onTouch
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.zoomable.ZoomableViewBaseController
import kotlin.math.max

@ExperimentalLithoApi
class ZoomableComponent(
    private val poolSize: Int = 5,
    private val zoomableTouchGestureListener: ZoomableTouchGestureListener? = null,
    private val zoomableEventListener: ZoomableViewBaseController.Listener? = null,
    private val excludeFromIncrementalMount: Boolean = false,
    private val enableIncrementalMountForContent: Boolean = false,
    private val forwardScrollChanges: Boolean = true,
    private val style: Style = Style,
    private val child: () -> Component,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {

    val (
        state: NestedLithoTreeState,
        resolveResult: ResolveResult,
    ) = useNestedTree(
        root = child(),
        treeProps = context.treePropContainer,
        config =
            context.lithoConfiguration.componentsConfig.copy(
                incrementalMountEnabled = enableIncrementalMountForContent,
            ),
    )

    val lithoZoomableController = useCached { LithoZoomableController(androidContext) }

    val zoomableTouchGestureListener = useCached {
      zoomableTouchGestureListener ?: ZoomableTouchGestureListener()
    }

    val zoomableStyle =
        Style.onTouch { touchEvent: TouchEvent ->
              zoomableTouchGestureListener.onTouch(touchEvent.motionEvent)
            }
            .onInterceptTouch(true) { _ -> lithoZoomableController.interceptingTouch }

    return LithoPrimitive(
        layoutBehavior = ZoomableLayoutBehavior(resolveResult = resolveResult),
        mountBehavior =
            MountBehavior(ZoomableAllocatorCache.getAllocator(poolSize, forwardScrollChanges)) {
              shouldExcludeFromIncrementalMount = excludeFromIncrementalMount
              bindToRenderTreeView(state = state) { renderTreeView }

              // Lifecycle binder: init/destroy on mount/unmount only.
              // Uses Unit dep so it never re-fires on prop changes.
              bind(Unit) { content ->
                lithoZoomableController.init()
                lithoZoomableController.bindTo(content)

                onUnbind { lithoZoomableController.destroy() }
              }

              // Listener binder: re-fires only when listener deps change.
              bind(zoomableTouchGestureListener, zoomableEventListener) { _ ->
                zoomableTouchGestureListener.addZoomableTouchListener(
                    lithoZoomableController.zoomableTouchListener,
                )
                zoomableEventListener?.let { lithoZoomableController.addZoomableEventListener(it) }

                onUnbind {
                  zoomableTouchGestureListener.removeZoomableTouchListener(
                      lithoZoomableController.zoomableTouchListener,
                  )
                  zoomableEventListener?.let {
                    lithoZoomableController.removeZoomableEventListener(it)
                  }
                }
              }
            },
        style = style.plus(zoomableStyle),
    )
  }
}

private object ZoomableAllocatorCache {
  private var cachedAllocator: ViewAllocator<LithoZoomableView>? = null
  private var cachedPoolSize: Int = -1
  private var cachedForwardScrollChanges: Boolean = true

  @Synchronized
  fun getAllocator(
      poolSize: Int,
      forwardScrollChanges: Boolean,
  ): ViewAllocator<LithoZoomableView> {
    val current = cachedAllocator
    if (
        current != null &&
            cachedPoolSize == poolSize &&
            cachedForwardScrollChanges == forwardScrollChanges
    ) {
      return current
    }
    val newAllocator =
        ViewAllocator(canPreallocate = true, poolSize = poolSize) { context ->
          LithoZoomableView(context, forwardScrollChanges = forwardScrollChanges)
        }
    cachedPoolSize = poolSize
    cachedForwardScrollChanges = forwardScrollChanges
    cachedAllocator = newAllocator
    return newAllocator
  }
}

internal class ZoomableLayoutBehavior(
    private val resolveResult: ResolveResult,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {

    val constraints: SizeConstraints = sizeConstraints.copy(minWidth = 0, minHeight = 0)

    // Use the parent's layout version to ensure version consistency during double measures
    val layoutState =
        NestedLithoTree.layout(
            result = resolveResult,
            sizeConstraints = constraints,
            current = previousLayoutData as LayoutState?,
            version = layoutVersion,
        )

    // Ensure that width and height are not less than 0
    val width: Int = max(sizeConstraints.minWidth, layoutState.width)
    val height: Int = max(sizeConstraints.minHeight, layoutState.height)

    layoutState.toRenderTree()

    return PrimitiveLayoutResult(width = width, height = height, layoutData = layoutState)
  }
}
