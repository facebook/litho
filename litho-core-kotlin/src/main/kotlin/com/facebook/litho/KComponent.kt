/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import android.view.View
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/** Base class for Kotlin Components. */
abstract class KComponent : Component() {
  companion object {

    /** Method that will ensure the KComponent class is loaded. */
    @JvmStatic
    fun preload() =
        object : KComponent() {
          override fun ComponentScope.render(): Component? = null
        }
  }

  final override fun render(c: ComponentContext, stateHandler: StateHandler): RenderResult {
    val componentScope = ComponentScope(c, stateHandler)
    val componentResult = componentScope.render()
    return RenderResult(
        componentResult, componentScope.transitions, componentScope.useEffectEntries)
  }

  abstract fun ComponentScope.render(): Component?

  /**
   * Compare this component to a different one to check if they are equivalent. This is used to be
   * able to skip rendering a component again.
   */
  final override fun isEquivalentTo(other: Component?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    if (id == other.id) {
      return true
    }
    if (!hasEquivalentFields(other as KComponent)) {
      return false
    }

    return true
  }

  /** Compare all private final fields in the components. */
  private fun hasEquivalentFields(other: KComponent): Boolean {
    for (field in javaClass.declaredFields) {
      val wasAccessible = field.isAccessible
      if (!wasAccessible) {
        field.isAccessible = true
      }
      val field1 = field.get(this)
      val field2 = field.get(other)
      if (!wasAccessible) {
        field.isAccessible = false
      }

      if (!EquivalenceUtils.areObjectsEquivalent(field1, field2)) {
        return false
      }
    }

    return true
  }

  // All other Component lifecycle methods are made final and no-op here as they shouldn't be
  // overriden.

  final override fun acceptTriggerEventImpl(
      eventTrigger: EventTrigger<*>,
      eventState: Any,
      params: Array<out Any>
  ) = super.acceptTriggerEventImpl(eventTrigger, eventState, params)

  final override fun applyPreviousRenderData(previousRenderData: RenderData) =
      super.applyPreviousRenderData(previousRenderData)

  final override fun bindDynamicProp(dynamicPropIndex: Int, value: Any?, content: Any) =
      super.bindDynamicProp(dynamicPropIndex, value, content)

  final override fun canMeasure() = false

  final override fun canPreallocate() = false

  final override fun canResolve() = false

  final override fun copyInterStageImpl(
      copyIntoInterStagePropsContainer: InterStagePropsContainer?,
      copyFromInterStagePropsContainer: InterStagePropsContainer?
  ) = super.copyInterStageImpl(copyIntoInterStagePropsContainer, copyFromInterStagePropsContainer)

  final override fun createInitialState(c: ComponentContext) = super.createInitialState(c)

  final override fun createInterStagePropsContainer() = super.createInterStagePropsContainer()

  final override fun createStateContainer() = super.createStateContainer()

  final override fun dispatchOnEnteredRange(c: ComponentContext, name: String) =
      super.dispatchOnEnteredRange(c, name)

  final override fun dispatchOnEventImpl(eventHandler: EventHandler<*>, eventState: Any) =
      super.dispatchOnEventImpl(eventHandler, eventState)

  final override fun dispatchOnExitedRange(c: ComponentContext, name: String) =
      super.dispatchOnExitedRange(c, name)

  internal final override fun getCommonDynamicProps() = super.getCommonDynamicProps()

  final override fun getDynamicProps() = super.getDynamicProps()

  final override fun getExtraAccessibilityNodeAt(c: ComponentContext, x: Int, y: Int) =
      super.getExtraAccessibilityNodeAt(c, x, y)

  final override fun getExtraAccessibilityNodesCount(c: ComponentContext) =
      super.getExtraAccessibilityNodesCount(c)

  final override fun getMountType() = super.getMountType()

  final override fun getScopedContext(): ComponentContext? = super.getScopedContext()

  final override fun getSimpleName(): String = super.getSimpleName()

  final override fun getTreePropsForChildren(c: ComponentContext, treeProps: TreeProps?) =
      super.getTreePropsForChildren(c, treeProps)

  final override fun hasAttachDetachCallback() = false

  final override fun hasChildLithoViews() = false

  internal final override fun hasCommonDynamicProps() = super.hasCommonDynamicProps()

  final override fun hasOwnErrorHandler() = false

  final override fun hasState() = false

  final override fun implementsAccessibility() = false

  final override fun implementsExtraAccessibilityNodes() = false

  final override fun isMountSizeDependent() = false

  final override fun isPureRender() = false

  final override fun makeShallowCopy() = super.makeShallowCopy()

  final override fun needsPreviousRenderData() = false

  final override fun onAttached(c: ComponentContext) = super.onAttached(c)

  final override fun onBind(c: ComponentContext, mountedContent: Any) =
      super.onBind(c, mountedContent)

  final override fun onBoundsDefined(c: ComponentContext, layout: ComponentLayout) =
      super.onBoundsDefined(c, layout)

  final override fun onCreateMountContent(context: Context) = super.onCreateMountContent(context)

  final override fun onCreateMountContentPool() = super.onCreateMountContentPool()

  final override fun onCreateTransition(c: ComponentContext) = super.onCreateTransition(c)

  final override fun onDetached(c: ComponentContext) = super.onDetached(c)

  final override fun onError(c: ComponentContext, e: Exception) = super.onError(c, e)

  final override fun onLoadStyle(c: ComponentContext) = super.onLoadStyle(c)

  final override fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size
  ) = super.onMeasure(c, layout, widthSpec, heightSpec, size)

  final override fun onMeasureBaseline(c: ComponentContext, width: Int, height: Int) =
      super.onMeasureBaseline(c, width, height)

  final override fun onMount(c: ComponentContext, convertContent: Any) =
      super.onMount(c, convertContent)

  final override fun onPopulateAccessibilityNode(
      c: ComponentContext,
      host: View,
      accessibilityNode: AccessibilityNodeInfoCompat
  ) = super.onPopulateAccessibilityNode(c, host, accessibilityNode)

  final override fun onPopulateExtraAccessibilityNode(
      c: ComponentContext,
      accessibilityNode: AccessibilityNodeInfoCompat,
      extraNodeIndex: Int,
      componentBoundsX: Int,
      componentBoundsY: Int
  ) =
      super.onPopulateExtraAccessibilityNode(
          c, accessibilityNode, extraNodeIndex, componentBoundsX, componentBoundsY)

  final override fun onPrepare(c: ComponentContext) = super.onPrepare(c)

  final override fun onUnbind(c: ComponentContext, mountedContent: Any) =
      super.onUnbind(c, mountedContent)

  final override fun onUnmount(c: ComponentContext, mountedContent: Any) =
      super.onUnmount(c, mountedContent)

  final override fun poolSize() = super.poolSize()

  final override fun populateTreeProps(parentTreeProps: TreeProps?) =
      super.populateTreeProps(parentTreeProps)

  final override fun recordEventTrigger(c: ComponentContext, container: EventTriggersContainer) =
      super.recordEventTrigger(c, container)

  final override fun recordRenderData(c: ComponentContext, toRecycle: RenderData) =
      super.recordRenderData(c, toRecycle)

  final override fun resolve(
      layoutContext: LayoutStateContext,
      c: ComponentContext
  ): InternalNode? = super.resolve(layoutContext, c)

  final override fun shouldAlwaysRemeasure() = false

  final override fun shouldUpdate(
      previous: Component?,
      prevStateContainer: StateContainer?,
      next: Component?,
      nextStateContainer: StateContainer?
  ) = super.shouldUpdate(previous, prevStateContainer, next, nextStateContainer)

  final override fun transferState(
      previousStateContainer: StateContainer,
      nextStateContainer: StateContainer
  ) = super.transferState(previousStateContainer, nextStateContainer)
}

/**
 * Sets a manual key on the given Component returned in the lambda, e.g.
 * ```
 * key("my_key") { Text(...) }
 * ```
 */
inline fun key(key: String, componentLambda: () -> Component): Component {
  val component = componentLambda()
  setKeyForComponentInternal(component, key)
  return component
}

/**
 * Sets a handle on the given Component returned in the lambda, e.g.
 * ```
 * handle(Handle()) { Text(...) }
 * ```
 */
inline fun handle(handle: Handle, componentLambda: () -> Component): Component {
  val component = componentLambda()
  setHandleForComponentInternal(component, handle)
  return component
}

/**
 * This is extracted out since we don't want to expose Component.setKey in the public API and will
 * hopefully change this implementation in the future.
 */
@PublishedApi
internal fun setKeyForComponentInternal(component: Component, key: String) {
  component.key = key
}

/**
 * This is extracted out since we don't want to expose Component.handle in the public API and will
 * hopefully change this implementation in the future.
 */
@PublishedApi
internal fun setHandleForComponentInternal(component: Component, handle: Handle) {
  component.handle = handle
}
