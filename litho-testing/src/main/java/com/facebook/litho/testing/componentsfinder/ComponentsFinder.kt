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

import com.facebook.litho.Column as ColumnBuilder
import com.facebook.litho.Component
import com.facebook.litho.ComponentKeyUtils.getKeyForTesting
import com.facebook.litho.DeferredLithoLayoutResult
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoNode
import com.facebook.litho.LithoView
import com.facebook.litho.NullLithoLayoutResult
import com.facebook.litho.Row as RowBuilder
import com.facebook.litho.ScopedComponentInfo
import com.facebook.litho.layout.flexbox.KColumn
import com.facebook.litho.layout.flexbox.KRow
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
      .firstOrNull { it.matchesClass(clazz) }
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
    clazz: KClass<out Component>,
): List<Component> {
  return findAllDirectComponentsInLithoView(lithoView, clazz.java)
}

fun findAllDirectComponentsInLithoView(
    lithoView: LithoView,
    clazz: Class<out Component>,
): List<Component> {
  val internalNode = getLayoutRoot(lithoView)?.node ?: return emptyList()
  return getOrderedScopedComponentInfos(internalNode)
      .map { it.component }
      .filter { it.matchesClass(clazz) }
}

/**
 * Returns a list of all components of the given classes from the ComponentTree or an empty list if
 * not found
 */
fun findAllComponentsInLithoView(
    lithoView: LithoView,
    vararg clazz: Class<out Component?>,
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
    vararg clazz: KClass<out Component>,
): List<Component> {
  val javaClasses = Array(clazz.size) { i -> clazz[i].java }
  return findAllComponentsInLithoView(lithoView, *javaClasses)
}

/** Returns a component with the given key from the ComponentTree or null if not found */
fun findComponentInLithoView(lithoView: LithoView, key: String): Component? {
  val layoutRoot = getLayoutRoot(lithoView) ?: return null
  return findComponentViaBreadthFirst(key, layoutRoot)
}

private fun getLayoutRoot(lithoView: LithoView): LithoLayoutResult? {
  val committedLayoutState =
      lithoView.componentTree?.getCommittedLayoutState()
          ?: throw IllegalStateException(
              "No ComponentTree/Committed Layout/Layout Root found. Please call render() first",
          )
  return committedLayoutState.rootLayoutResult as? LithoLayoutResult
}

/**
 * Goes through nodes in a component tree via breadth first search. Returns a component of a given
 * class or null if not found
 */
private fun findComponentViaBreadthFirst(
    clazz: Class<out Component?>,
    layoutResult: LithoLayoutResult?,
): Component? {
  componentBreadthFirstSearch(layoutResult) { scopedComponents ->
    val foundComponent = scopedComponents.firstOrNull { it.matchesClass(clazz) }
    if (foundComponent != null) {
      return foundComponent
    }
  }

  return null
}

/**
 * Goes through nodes in a component tree via breadth first search. Returns a component with a given
 * key or null if not found
 */
private fun findComponentViaBreadthFirst(
    key: String,
    layoutResult: LithoLayoutResult?,
): Component? {
  componentBreadthFirstSearch(layoutResult) { scopedComponents ->
    val foundComponent = scopedComponents.firstOrNull { getKeyForTesting(it) == key }
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
    val foundComponents = scopedComponents.filter { c -> clazzArray.any { c.matchesClass(it) } }
    foundComponentsList.addAll(foundComponents)
  }

  return foundComponentsList
}

/**
 * Treat the legacy Spec-gen [Column] / [Row] classes as also matching their Kotlin replacements
 * ([KColumn] / [KRow]) so existing test assertions on `Column::class.java` keep working after the
 * `useKFlexboxContainerComponent` default flipped to `true` and the DSL now constructs KColumn /
 * KRow. Used by both the finder functions above and by `ComponentConditions.typeIs` so the two
 * paths agree.
 */
internal fun Component.matchesClass(clazz: Class<out Component?>): Boolean {
  val cls = this.javaClass
  if (cls == clazz) return true
  return when (cls) {
    KColumn::class.java -> clazz == ColumnBuilder::class.java
    KRow::class.java -> clazz == RowBuilder::class.java
    else -> false
  }
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
    onHandleScopedComponents: (List<Component>) -> Unit,
) {
  startingLayoutResult ?: return

  val visited = mutableSetOf(startingLayoutResult)
  val queue = ArrayDeque(visited)

  while (queue.isNotEmpty()) {
    val current = queue.removeFirst()
    if (current is NullLithoLayoutResult) {
      continue
    }

    onHandleScopedComponents(getOrderedScopedComponentInfos(current.node).map { it.component })

    if (current is DeferredLithoLayoutResult) {
      val nestedResult = current.result?.takeUnless { it in visited } ?: continue
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
