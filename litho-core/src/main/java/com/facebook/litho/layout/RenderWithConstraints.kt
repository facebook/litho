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

package com.facebook.litho.layout

import android.content.Context
import com.facebook.litho.CalculationContext
import com.facebook.litho.CommonProps
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentResolveResult
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentsSystrace
import com.facebook.litho.LithoNode
import com.facebook.litho.NestedTreeHolder
import com.facebook.litho.NullNode
import com.facebook.litho.RenderResult
import com.facebook.litho.ResolveContext
import com.facebook.litho.Resolver
import com.facebook.litho.ScopedComponentInfo
import com.facebook.litho.StateContainer
import com.facebook.litho.Style
import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.debug.LithoDebugEventAttributes
import com.facebook.litho.runInRecorderScope
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventDispatcher
import com.facebook.rendercore.utils.hasEquivalentFields

class RenderWithConstraintsScope
internal constructor(
    context: ComponentContext,
) : ComponentScope(context)

class RenderWithConstraints(
    private val style: Style? = null,
    private val render: RenderWithConstraintsScope.(SizeConstraints) -> Component?
) : Component() {

  override fun resolveDeferred(
      calculationContext: CalculationContext,
      componentContext: ComponentContext,
      parentContext: ComponentContext
  ): ComponentResolveResult {
    val node =
        NestedTreeHolder(
            componentContext.treePropContainer,
            calculationContext.cache.getCachedNode(this),
            parentContext,
        )

    val commonProps =
        if (style != null) {
          CommonProps().apply { style.applyCommonProps(componentContext, this) }
        } else {
          null
        }

    return ComponentResolveResult(node, commonProps)
  }

  override fun resolve(
      calculationContext: ResolveContext,
      componentScope: ScopedComponentInfo,
      parentWidthSpec: Int,
      parentHeightSpec: Int,
  ): ComponentResolveResult {

    val c: ComponentContext = componentScope.context
    val renderResult: RenderResult<Component?> =
        DebugEventDispatcher.trace(
            type = LithoDebugEvent.ComponentRendered,
            renderStateId = { calculationContext.treeId.toString() },
            attributesAccumulator = { accumulator ->
              accumulator[LithoDebugEventAttributes.Component] = simpleName
              accumulator[DebugEventAttribute.Name] = simpleName
            },
        ) {
          ComponentsSystrace.trace("render:$simpleName") {
            componentScope.runInRecorderScope(calculationContext) {
              val scope = RenderWithConstraintsScope(componentScope.context)
              val result =
                  scope.withResolveContext(calculationContext) {
                    scope.render(
                        SizeConstraints.Companion.fromMeasureSpecs(
                            parentWidthSpec, parentHeightSpec),
                    )
                  }
              RenderResult(result, scope.transitionData, scope.useEffectEntries)
            }
          }
        }

    val root = renderResult.value
    val node: LithoNode? =
        if (root != null) {
          Resolver.resolve(calculationContext, c, root)
        } else {
          NullNode()
        }

    if (node != null) {
      Resolver.applyTransitionsAndUseEffectEntriesToNode(
          renderResult.transitionData,
          renderResult.useEffectEntries,
          node,
      )
    }

    val commonProps =
        if (node != null && node !is NullNode) {
          CommonProps().apply { style?.applyCommonProps(c, this) }
        } else {
          null
        }

    return ComponentResolveResult(node, commonProps)
  }

  /**
   * Compare this component to a different one to check if they are equivalent. This is used to be
   * able to skip rendering a component again.
   */
  override fun isEquivalentProps(other: Component?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    if (instanceId == other.instanceId) {
      return true
    }
    if (!hasEquivalentFields(this, other)) {
      return false
    }

    return true
  }

  // All other Component lifecycle methods are made final

  override fun isEquivalentTo(other: Component?): Boolean = super.isEquivalentTo(other)

  override fun canMeasure(): Boolean = true

  override fun getMountType(): MountType = super.getMountType()

  override fun getSimpleName(): String = super.getSimpleName()

  override fun isPureRender(): Boolean = false

  override fun makeShallowCopy(): Component = super.makeShallowCopy()

  override fun onCreateMountContent(context: Context): Any = super.onCreateMountContent(context)

  override fun shouldUpdate(
      previous: Component,
      prevStateContainer: StateContainer?,
      next: Component,
      nextStateContainer: StateContainer?
  ): Boolean = super.shouldUpdate(previous, prevStateContainer, next, nextStateContainer)
}
