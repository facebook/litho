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
import com.facebook.litho.ComponentsSystrace.beginSection
import com.facebook.litho.ComponentsSystrace.endSection
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.debug.LithoDebugEventAttributes
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.debug.DebugEventDispatcher.beginTrace
import com.facebook.rendercore.debug.DebugEventDispatcher.endTrace
import com.facebook.rendercore.debug.DebugEventDispatcher.generateTraceIdentifier
import com.facebook.rendercore.incrementalmount.ExcludeFromIncrementalMountBinder
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.utils.hasEquivalentFields
import com.facebook.yoga.YogaFlexDirection

/**
 * Base class for Kotlin primitive components. This class encapsulates some of the Mount Spec APIs.
 * All Kotlin primitive components must extend this class.
 */
abstract class PrimitiveComponent : Component() {

  final override fun prepare(resolveContext: ResolveContext, c: ComponentContext): PrepareResult {
    val primitiveComponentScope = PrimitiveComponentScope(c, resolveContext)
    val lithoPrimitive = primitiveComponentScope.render()

    primitiveComponentScope.cleanUp()

    var commonProps: CommonProps? = null
    if (lithoPrimitive.style != null) {
      commonProps = CommonProps()
      lithoPrimitive.style.applyCommonProps(c, commonProps)
    }

    if (primitiveComponentScope.shouldExcludeFromIncrementalMount) {
      lithoPrimitive.primitive.renderUnit.addAttachBinder(
          RenderUnit.DelegateBinder.createDelegateBinder(
              lithoPrimitive.primitive.renderUnit, ExcludeFromIncrementalMountBinder.INSTANCE))
    }

    return PrepareResult(
        lithoPrimitive.primitive,
        primitiveComponentScope.transitions,
        primitiveComponentScope.useEffectEntries,
        commonProps)
  }

  final override fun resolve(
      resolveContext: ResolveContext,
      scopedComponentInfo: ScopedComponentInfo,
      parentWidthSpec: Int,
      parentHeightSpec: Int,
      componentsLogger: ComponentsLogger?
  ): ComponentResolveResult {
    val node = LithoNode()
    if (!ComponentsConfiguration.disableFlexDirectionInResolve) {
      node.flexDirection(YogaFlexDirection.COLUMN)
    }
    var commonProps: CommonProps? = null
    val isTracing = ComponentsSystrace.isTracing

    val prepareEvent =
        Resolver.createPerformanceEvent(
            this, componentsLogger, FrameworkLogEvents.EVENT_COMPONENT_PREPARE)

    val componentPrepareTraceIdentifier =
        generateTraceIdentifier(com.facebook.litho.debug.LithoDebugEvent.ComponentPrepared)
    if (componentPrepareTraceIdentifier != null) {
      val attributes = HashMap<String, Any?>()
      attributes[LithoDebugEventAttributes.RunsOnMainThread] = ThreadUtils.isMainThread()
      attributes[LithoDebugEventAttributes.Component] = getSimpleName()
      beginTrace(
          componentPrepareTraceIdentifier,
          com.facebook.litho.debug.LithoDebugEvent.ComponentPrepared,
          resolveContext.treeId.toString(),
          attributes)
    }

    if (isTracing) {
      beginSection("prepare:" + getSimpleName())
    }

    val prepareResult: PrepareResult? =
        try {
          prepare(resolveContext, scopedComponentInfo.context)
        } finally {
          if (prepareEvent != null && componentsLogger != null) {
            componentsLogger.logPerfEvent(prepareEvent)
          }
          componentPrepareTraceIdentifier?.let { endTrace(it) }
        }

    if (prepareResult != null) {
      if (prepareResult.primitive != null) {
        node.primitive = prepareResult.primitive
      }

      Resolver.applyTransitionsAndUseEffectEntriesToNode(
          prepareResult.transitions, prepareResult.useEffectEntries, node)

      commonProps = prepareResult.commonProps
    }
    if (isTracing) {
      // end of prepare
      endSection()
    }
    return ComponentResolveResult(node, commonProps)
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

  final override fun isEquivalentTo(other: Component?, shouldCompareCommonProps: Boolean) =
      super.isEquivalentTo(other, shouldCompareCommonProps)

  final override fun getSimpleName(): String = super.getSimpleName()

  final override fun isPureRender(): Boolean = true

  final override fun implementsShouldUpdate(): Boolean = true

  final override fun makeShallowCopy(): Component = super.makeShallowCopy()

  final override fun onCreateMountContent(context: Context): Any =
      super.onCreateMountContent(context)

  final override fun resolve(resolveContext: ResolveContext, c: ComponentContext): LithoNode? =
      super.resolve(resolveContext, c)

  final override fun shouldUpdate(
      previous: Component,
      prevStateContainer: StateContainer?,
      next: Component,
      nextStateContainer: StateContainer?
  ): Boolean = super.shouldUpdate(previous, prevStateContainer, next, nextStateContainer)

  final override fun render(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int
  ): RenderResult {
    return super.render(resolveContext, c, widthSpec, heightSpec)
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
