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

import android.content.Context
import android.util.SparseArray
import android.view.View
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.utils.hasEquivalentFields

/**
 * Base class for Kotlin primitive components. This class encapsulates some of the Mount Spec APIs.
 * All Kotlin primitive components must extend this class.
 */
abstract class PrimitiveComponent : Component() {

  final override fun prepare(
      resolveStateContext: ResolveStateContext,
      c: ComponentContext
  ): PrepareResult {
    val primitiveComponentScope = PrimitiveComponentScope(c, resolveStateContext)
    val lithoPrimitive = primitiveComponentScope.render()

    primitiveComponentScope.cleanUp()

    var commonProps: CommonProps? = null
    if (lithoPrimitive.style != null) {
      commonProps = CommonProps()
      lithoPrimitive.style.applyCommonProps(c, commonProps)
    }

    return PrepareResult(
        lithoPrimitive.primitive,
        primitiveComponentScope.transitions,
        primitiveComponentScope.useEffectEntries,
        commonProps)
  }

  /** This function must return [LithoPrimitive] which are immutable. */
  abstract fun PrimitiveComponentScope.render(): LithoPrimitive

  final override fun getMountType(): MountType = MountType.PRIMITIVE

  final override fun canMeasure(): Boolean = true

  /**
   * Compare this component to a different one to check if they are equivalent. This is used to be
   * able to skip rendering a component again.
   */
  final override fun isEquivalentProps(
      other: Component?,
      shouldCompareCommonProps: Boolean,
  ): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    if (id == other.id) {
      return true
    }
    if (!hasEquivalentFields(this, other)) {
      return false
    }

    return true
  }

  // All other Component lifecycle methods are final and no-op here as they shouldn't be overridden.

  final override fun canResolve(): Boolean = false

  internal final override fun getCommonDynamicProps(): SparseArray<DynamicValue<*>>? =
      super.getCommonDynamicProps()

  final override fun getSimpleName(): String = super.getSimpleName()

  internal final override fun hasCommonDynamicProps(): Boolean = super.hasCommonDynamicProps()

  final override fun isPureRender(): Boolean = true

  final override fun implementsShouldUpdate(): Boolean = true

  final override fun makeShallowCopy(): Component = super.makeShallowCopy()

  final override fun onCreateMountContent(context: Context): Any =
      super.onCreateMountContent(context)

  final override fun onCreateTransition(c: ComponentContext): Transition? =
      super.onCreateTransition(c)

  final override fun onLoadStyle(c: ComponentContext) = super.onLoadStyle(c)

  final override fun onPopulateAccessibilityNode(
      c: ComponentContext,
      host: View,
      accessibilityNode: AccessibilityNodeInfoCompat,
      interStagePropsContainer: InterStagePropsContainer?
  ) = super.onPopulateAccessibilityNode(c, host, accessibilityNode, interStagePropsContainer)

  final override fun onPopulateExtraAccessibilityNode(
      c: ComponentContext,
      accessibilityNode: AccessibilityNodeInfoCompat,
      extraNodeIndex: Int,
      componentBoundsX: Int,
      componentBoundsY: Int,
      interStagePropsContainer: InterStagePropsContainer?
  ) =
      super.onPopulateExtraAccessibilityNode(
          c,
          accessibilityNode,
          extraNodeIndex,
          componentBoundsX,
          componentBoundsY,
          interStagePropsContainer)

  final override fun resolve(
      resolveStateContext: ResolveStateContext,
      c: ComponentContext
  ): LithoNode? = super.resolve(resolveStateContext, c)

  final override fun shouldUpdate(
      previous: Component,
      prevStateContainer: StateContainer?,
      next: Component,
      nextStateContainer: StateContainer?
  ): Boolean = super.shouldUpdate(previous, prevStateContainer, next, nextStateContainer)

  final override fun render(
      resolveStateContext: ResolveStateContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int
  ): RenderResult {
    return super.render(resolveStateContext, c, widthSpec, heightSpec)
  }

  final override fun isEqualivalentTreeProps(
      current: ComponentContext,
      next: ComponentContext
  ): Boolean {
    return super.isEqualivalentTreeProps(current, next)
  }
}

/**
 * A class that represents a [Primitive] with [Style] that should be applied to the
 * [PrimitiveComponent].
 */
class LithoPrimitive(val primitive: Primitive, val style: Style?) {
  constructor(
      layoutBehavior: LayoutBehavior,
      mountBehavior: MountBehavior<*>,
      style: Style?
  ) : this(Primitive(layoutBehavior, mountBehavior), style)
}
