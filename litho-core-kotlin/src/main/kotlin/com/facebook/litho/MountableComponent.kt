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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.debug.LithoDebugEventAttributes
import com.facebook.rendercore.Mountable
import com.facebook.rendercore.RenderUnit.DelegateBinder.createDelegateBinder
import com.facebook.rendercore.debug.DebugEventDispatcher
import com.facebook.rendercore.incrementalmount.ExcludeFromIncrementalMountBinder
import com.facebook.rendercore.primitives.utils.hasEquivalentFields
import com.facebook.yoga.YogaFlexDirection

/**
 * Base class for Kotlin mountable components. This class encapsulates some of the Mount Spec APIs.
 * All Kotlin mountable components must extend this class.
 */
@Deprecated(
    "Mountable API is deprecated. Use Primitive API instead. Docs: https://fburl.com/staticdocs/j7w6qqyz; example component: https://fburl.com/code/9kigeb7a",
    replaceWith = ReplaceWith("PrimitiveComponent()", "com.facebook.litho.PrimitiveComponent"))
abstract class MountableComponent() : Component() {

  final override fun prepare(resolveContext: ResolveContext, c: ComponentContext): PrepareResult {
    val mountableComponentScope = MountableComponentScope(c, resolveContext)
    val mountableRenderResult = mountableComponentScope.render()

    mountableComponentScope.cleanUp()

    var commonProps: CommonProps? = null
    if (mountableRenderResult.style != null) {
      commonProps = CommonProps()
      mountableRenderResult.style.applyCommonProps(c, commonProps)
    }

    // generate ID and set it on the Mountable
    val idGenerator = c.renderUnitIdGenerator
    if (idGenerator == null) {
      throw IllegalStateException("Attempt to use a released RenderStateContext")
    } else {
      mountableRenderResult.mountable.id =
          idGenerator.calculateLayoutOutputId(c.globalKey, OutputUnitType.CONTENT)
    }

    mountableRenderResult.mountable.addOptionalMountBinder(
        createDelegateBinder(
            mountableRenderResult.mountable, DynamicValuesBinder(mountableComponentScope.binders)))
    if (mountableComponentScope.shouldExcludeFromIncrementalMount) {
      mountableRenderResult.mountable.addAttachBinder(
          createDelegateBinder(
              mountableRenderResult.mountable, ExcludeFromIncrementalMountBinder.INSTANCE))
    }

    return PrepareResult(
        mountableRenderResult.mountable,
        mountableComponentScope.transitions,
        mountableComponentScope.useEffectEntries,
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
        DebugEventDispatcher.generateTraceIdentifier(
            com.facebook.litho.debug.LithoDebugEvent.ComponentPrepared)
    if (componentPrepareTraceIdentifier != null) {
      val attributes = HashMap<String, Any?>()
      attributes[LithoDebugEventAttributes.RunsOnMainThread] = ThreadUtils.isMainThread()
      attributes[LithoDebugEventAttributes.Component] = getSimpleName()
      DebugEventDispatcher.beginTrace(
          componentPrepareTraceIdentifier,
          com.facebook.litho.debug.LithoDebugEvent.ComponentPrepared,
          resolveContext.treeId.toString(),
          attributes)
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("prepare:" + getSimpleName())
    }

    val prepareResult: PrepareResult? =
        try {
          prepare(resolveContext, scopedComponentInfo.context)
        } finally {
          if (prepareEvent != null && componentsLogger != null) {
            componentsLogger.logPerfEvent(prepareEvent)
          }
          componentPrepareTraceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
        }

    if (prepareResult != null) {
      if (prepareResult.mountable != null) {
        node.mountable = prepareResult.mountable
      }

      Resolver.applyTransitionsAndUseEffectEntriesToNode(
          prepareResult.transitions, prepareResult.useEffectEntries, node)

      commonProps = prepareResult.commonProps
    }
    if (isTracing) {
      // end of prepare
      ComponentsSystrace.endSection()
    }
    return ComponentResolveResult(node, commonProps)
  }

  /** This function must return [Mountable] which are immutable. */
  abstract fun MountableComponentScope.render(): MountableRenderResult

  final override fun getMountType(): MountType = MountType.MOUNTABLE

  final override fun canMeasure(): Boolean {
    return true
  }

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

  final override fun makeShallowCopy() = super.makeShallowCopy()

  final override fun onCreateMountContent(context: Context) = super.onCreateMountContent(context)

  final override fun resolve(resolveContext: ResolveContext, c: ComponentContext): LithoNode? =
      super.resolve(resolveContext, c)

  final override fun shouldUpdate(
      previous: Component,
      prevStateContainer: StateContainer?,
      next: Component,
      nextStateContainer: StateContainer?
  ) = super.shouldUpdate(previous, prevStateContainer, next, nextStateContainer)

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
 * A class that holds a [Mountable] and [Style] that should be applied to the [MountableComponent].
 */
class MountableRenderResult(val mountable: Mountable<*>, val style: Style?)
