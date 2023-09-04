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
import com.facebook.rendercore.primitives.utils.hasEquivalentFields

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

  final override fun render(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int
  ): RenderResult {
    val componentScope = ComponentScope(c, resolveContext)
    val componentResult = componentScope.render()
    componentScope.cleanUp()
    return RenderResult(
        componentResult, componentScope.transitions, componentScope.useEffectEntries)
  }

  final override fun resolve(
      resolveContext: ResolveContext,
      scopedComponentInfo: ScopedComponentInfo,
      parentWidthSpec: Int,
      parentHeightSpec: Int,
      componentsLogger: ComponentsLogger?
  ): ComponentResolveResult {
    val c: ComponentContext = scopedComponentInfo.context
    val renderResult: RenderResult = render(resolveContext, c, parentWidthSpec, parentHeightSpec)
    val root = renderResult.component

    val node: LithoNode? =
        if (root != null) {
          Resolver.resolve(resolveContext, c, root)
        } else {
          NullNode()
        }

    if (node != null) {
      Resolver.applyTransitionsAndUseEffectEntriesToNode(
          renderResult.transitions, renderResult.useEffectEntries, node)
    }

    // KComponent doesn't itself hold any CommonProps so return null
    return ComponentResolveResult(node, null)
  }

  abstract fun ComponentScope.render(): Component?

  /**
   * Compare this component to a different one to check if they are equivalent. This is used to be
   * able to skip rendering a component again.
   */
  final override fun isEquivalentProps(
      other: Component?,
      shouldCompareCommonProps: Boolean
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

  // All other Component lifecycle methods are made final and no-op here as they shouldn't be
  // overriden.

  final override fun isEquivalentTo(other: Component?, shouldCompareCommonProps: Boolean) =
      super.isEquivalentTo(other, shouldCompareCommonProps)

  final override fun canMeasure() = false

  final override fun getMountType() = super.getMountType()

  final override fun getSimpleName(): String = super.getSimpleName()

  final override fun isPureRender() = false

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
}

/**
 * Sets a manual key on the given Component returned in the lambda, e.g.
 *
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
 *
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
