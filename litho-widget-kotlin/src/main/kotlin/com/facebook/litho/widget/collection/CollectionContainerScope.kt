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

package com.facebook.litho.widget.collection

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ContainerDsl
import com.facebook.litho.ResourcesScope

@ContainerDsl
class CollectionContainerScope(override val context: ComponentContext) : ResourcesScope {

  val collectionChildren
    get() = collectionChildrenBuilder.toList()

  internal val effectiveIndexToId: MutableMap<Int, MutableSet<Any>> by lazy { mutableMapOf() }
  internal val idToChild: MutableMap<Any, CollectionChild> by lazy { mutableMapOf() }

  private val collectionChildrenBuilder = mutableListOf<CollectionChild>()
  private var nextStaticId = 0
  private val typeToFreq: MutableMap<Int, Int> by lazy { mutableMapOf() }

  /** Prepare the final id that will be assigned to the child. */
  private fun getResolvedId(id: Any?, component: Component? = null): Any {
    // Generate an id that is unique to the [CollectionContainerScope] in which it was defined
    // If an id has been explicitly defined on the child, use that
    // If the child has a component generate an id including the type and frequency
    // Otherwise the child has a null component or a lambda generator, so generate an id.
    return id ?: generateIdForComponent(component) ?: generateStaticId()
  }

  private fun generateStaticId(): Any {
    return "staticId:${nextStaticId++}"
  }

  /** Generate an id for a non-null Component. */
  private fun generateIdForComponent(component: Component?): Any? {
    val typeId = component?.typeId ?: return null
    typeToFreq[typeId] = (typeToFreq[typeId] ?: 0) + 1
    return "${typeId}:${typeToFreq[typeId]}"
  }

  private fun maybeRegisterForOnEnterOrNearCallbacks(child: CollectionChild) {
    child.onNearViewport ?: return

    idToChild[child.id] = child
    val index = collectionChildrenBuilder.lastIndex
    val effectiveIndex = index - child.onNearViewport.offset
    effectiveIndexToId.getOrPut(effectiveIndex) { mutableSetOf() }.add(child.id)
  }

  /**
   * Add a child [Component] to the collection.
   *
   * @param component The component to add
   * @param id A unique identifier for the child
   * @param isSticky Fix the child to the top of the collection if it is scrolled out of view
   * @param isFullSpan Span the child across all columns
   * @param spanSize Span the specified number of columns
   * @param onNearViewport A callback that will be invoked when the child is close to or enters the
   * visible area.
   */
  fun child(
      component: Component?,
      id: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
      onNearViewport: OnNearCallback? = null,
  ) {
    val resolvedId = getResolvedId(id, component)
    component ?: return
    val child =
        CollectionChild(
            resolvedId,
            component,
            null,
            isSticky,
            isFullSpan,
            spanSize,
            null,
            onNearViewport,
        )
    collectionChildrenBuilder.add(child)
    maybeRegisterForOnEnterOrNearCallbacks(child)
  }

  /**
   * Add a child [Component] created by the provided [componentFunction] function.
   *
   * The [Component] will be created by invoking [componentFunction] when the child is first
   * rendered. The [Component] will be reused in subsequent renders unless there is a change to
   * [deps]. [deps] is an array of dependencies that should contain any props or state that are used
   * inside [componentFunction].
   *
   * @param id A unique identifier for the child
   * @param isSticky Fix the child to the top of the collection if it is scrolled out of view
   * @param isFullSpan Span the child across all columns
   * @param spanSize Span the specified number of columns
   * @param onNearViewport A callback that will be invoked when the child is close to or enters the
   * visible area.
   * @param deps An array of prop and state values used by [componentFunction] to create the
   * [Component]. A change to one of these values will cause [componentFunction] to recreate the
   * [Component].
   * @param componentFunction A function that returns a [Component]
   */
  fun child(
      id: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
      onNearViewport: OnNearCallback? = null,
      deps: Array<Any?>,
      componentFunction: ComponentCreationScope.() -> Component?,
  ) {
    val child =
        CollectionChild(
            getResolvedId(id),
            null,
            { ComponentCreationScope(context).componentFunction() },
            isSticky,
            isFullSpan,
            spanSize,
            deps,
            onNearViewport,
        )
    collectionChildrenBuilder.add(child)
    maybeRegisterForOnEnterOrNearCallbacks(child)
  }

  /**
   * Add a list of children generated by applying [componentFunction] to each item in a list of
   * models.
   *
   * @param items Data models to be rendered as children
   * @param id A function to create a unique id from each data model
   * @param componentFunction A function that generates a [Component] from a data model
   */
  fun <T> children(
      items: List<T>,
      id: (T) -> Any,
      componentFunction: ComponentCreationScope.(T) -> Component?,
  ) {
    val componentCreationScope = ComponentCreationScope(context)
    items.forEach { item ->
      child(id = id(item), component = componentCreationScope.componentFunction(item))
    }
  }

  /**
   * Add a list of children generated by applying [componentFunction] to each item in a list of
   * models.
   *
   * @param items Data models to be rendered as children
   * @param id A function to create a unique id from each data model
   * @param deps A function to create a list of deps from each data model
   * @param componentFunction A function that generates a [Component] from a data model
   */
  fun <T> children(
      items: List<T>,
      id: (T) -> Any,
      deps: (T) -> Array<Any?>,
      componentFunction: ComponentCreationScope.(T) -> Component?,
  ) {
    val componentCreationScope = ComponentCreationScope(context)
    items.forEach { item ->
      child(
          id = id(item),
          deps = deps(item),
          componentFunction = { componentCreationScope.componentFunction(item) })
    }
  }
}
