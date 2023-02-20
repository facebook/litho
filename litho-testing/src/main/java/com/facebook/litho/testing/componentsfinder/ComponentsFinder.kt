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

package com.facebook.litho.componentsfinder

import com.facebook.litho.Component
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoNode
import com.facebook.litho.LithoView
import com.facebook.litho.NestedTreeHolderResult
import com.facebook.litho.ScopedComponentInfo
import kotlin.reflect.KClass

/**
 * Returns the root component of the LithoView.
 *
 * example:
 * ```
 * class FBStory : KComponent() {
 * override fun ComponentScope.render() {
 * return Story()
 * }
 * ```
 *
 * `findRootComponentInLithoView` will only be able to return `FBStory` component, from a LithoView
 * with FBStory as root. If you would like to be able to find the `Story` component, see
 * [findDirectComponentInLithoView]
 */
fun getRootComponentInLithoView(lithoView: LithoView): Component? {
  val internalNode = getLayoutRoot(lithoView)?.node ?: return null
  return if (internalNode.scopedComponentInfos.isNotEmpty()) internalNode.headComponent else null
}

/**
 * Returns a component of the given class only if the component renders directly to the root
 * component of the LithoView
 *
 * example:
 * ```
 * class FBStory : KComponent() {
 * override fun ComponentScope.render() {
 * return Story()
 * }
 * ```
 *
 * findDirectComponentInLithoView will only be able to return Story Component from the LithoView
 * with FBStory as a root
 *
 * ```
 * class FBStory : KComponent() {
 * override fun ComponentScope.render() {
 * return Column{
 *    child(CommentComponent())
 *    child(AuthorComponent())
 *    }
 * }
 * findDirectComponentInLithoView here will only be able to return Column
 * ```
 */
fun findDirectComponentInLithoView(lithoView: LithoView, clazz: Class<out Component?>): Component? {
  val internalNode = getLayoutRoot(lithoView)?.node ?: return null
  return getOrderedScopedComponentInfos(internalNode)
      .map { it.component }
      .firstOrNull { it.javaClass == clazz }
}

/**
 * Returns a component of the given class only if the component renders directly to the root
 * component of the LithoView (for details see: [findDirectComponentInLithoView])
 */
fun findDirectComponentInLithoView(lithoView: LithoView, clazz: KClass<out Component>): Component? {
  return findDirectComponentInLithoView(lithoView, clazz.java)
}

/** Returns a component of the given class from the ComponentTree or null if not found */
fun findComponentInLithoView(lithoView: LithoView, clazz: Class<out Component?>): Component? {
  val layoutRoot = getLayoutRoot(lithoView) ?: return null
  return findComponentViaBreadthFirst(clazz, layoutRoot)
}

/** Returns a component of the given class from the ComponentTree or null if not found */
fun findComponentInLithoView(lithoView: LithoView, clazz: KClass<out Component>): Component? {
  return findComponentInLithoView(lithoView, clazz.java)
}

fun findAllDirectComponentsInLithoView(
    lithoView: LithoView,
    clazz: KClass<out Component>
): List<Component> {
  return findAllDirectComponentsInLithoView(lithoView, clazz.java)
}

fun findAllDirectComponentsInLithoView(
    lithoView: LithoView,
    clazz: Class<out Component>
): List<Component> {
  val internalNode = getLayoutRoot(lithoView)?.node ?: return emptyList()
  return getOrderedScopedComponentInfos(internalNode)
      .map { it.component }
      .filter { it.javaClass == clazz }
}

/**
 * Returns a list of all components of the given classes from the ComponentTree or an empty list if
 * not found
 */
fun findAllComponentsInLithoView(
    lithoView: LithoView,
    vararg clazz: Class<out Component?>
): List<Component> {
  val layoutResult = getLayoutRoot(lithoView) ?: return emptyList()
  return findAllComponentsViaBreadthFirstSearch(clazz, layoutResult)
}

/**
 * Returns a list of all components of the given classes from the ComponentTree or an empty list if
 * not found
 */
fun findAllComponentsInLithoView(
    lithoView: LithoView,
    vararg clazz: KClass<out Component>
): List<Component> {
  val javaClasses = Array(clazz.size) { i -> clazz[i].java }
  return findAllComponentsInLithoView(lithoView, *javaClasses)
}

private fun getLayoutRoot(lithoView: LithoView): LithoLayoutResult? {
  val commitedLayoutState =
      lithoView.componentTree?.committedLayoutState
          ?: throw IllegalStateException(
              "No ComponentTree/Committed Layout/Layout Root found. Please call render() first")
  return commitedLayoutState.rootLayoutResult
}

/**
 * Goes through nodes in a component tree via breadth first search. Returns a component of a given
 * class or null if not found
 */
private fun findComponentViaBreadthFirst(
    clazz: Class<out Component?>,
    layoutResult: LithoLayoutResult?
): Component? {
  componentBreadthFirstSearch(layoutResult) { scopedComponents ->
    val foundComponent = scopedComponents.firstOrNull { it.javaClass == clazz }
    if (foundComponent != null) {
      return foundComponent
    }
  }

  return null
}

/**
 * Goes through nodes in a component tree via breadth first search. Returns all components that
 * match the given classes or an empty list if none found
 */
private fun findAllComponentsViaBreadthFirstSearch(
    clazzArray: Array<out Class<out Component?>>,
    layoutResult: LithoLayoutResult?,
): List<Component> {
  val foundComponentsList = mutableListOf<Component>()

  componentBreadthFirstSearch(layoutResult) { scopedComponents ->
    val foundComponents = scopedComponents.filter { it.javaClass in clazzArray }
    foundComponentsList.addAll(foundComponents)
  }

  return foundComponentsList
}

/**
 * Internal function to handle BFS through a set of components.
 *
 * @param onHandleScopedComponents lambda which handles the scoped components of the particular
 *   layout. This enables the caller of the function to properly handle if any of those components
 *   match. (For example, if looking for a single component, you would want to return in the lambda
 *   if it matches. If looking for multiple, you would simply want to add all matching components to
 *   a list.)
 */
private inline fun componentBreadthFirstSearch(
    startingLayoutResult: LithoLayoutResult?,
    onHandleScopedComponents: (List<Component>) -> Unit
) {
  startingLayoutResult ?: return

  val visited = mutableSetOf(startingLayoutResult)
  val queue = ArrayDeque(visited)

  while (queue.isNotEmpty()) {
    val current = queue.removeFirst()
    onHandleScopedComponents(getOrderedScopedComponentInfos(current.node).map { it.component })

    if (current is NestedTreeHolderResult) {
      val nestedResult = current.nestedResult?.takeUnless { it in visited } ?: continue
      queue.add(nestedResult)
      continue
    }

    for (i in 0 until current.childCount) {
      val child = current.getChildAt(i).takeUnless { it in visited } ?: continue
      queue.add(child)
    }
  }
}

/**
 * @return list of the [ScopedComponentInfo]s, ordered from the head (closest to the root) to the
 *   tail.
 */
private fun getOrderedScopedComponentInfos(internalNode: LithoNode): List<ScopedComponentInfo> {
  return internalNode.scopedComponentInfos.reversed()
}
