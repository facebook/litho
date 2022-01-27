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
import com.facebook.litho.LithoView
import com.facebook.litho.NestedTreeHolderResult
import kotlin.reflect.KClass

/**
 * Returns a component of the given class only if the component renders directly to the root
 * component of the LithoView
 *
 * example:
 * ```
 * FBStory
 * --Story
 *    |__Text
 *   |__Text
 * --Column
 *   |__Text
 *   |__Text
 * ```
 * findDirectComponentInLithoView will only be able to return Story or Column from the LithoView
 * with FBStory as a root
 */
fun findDirectComponentInLithoView(lithoView: LithoView, clazz: Class<out Component?>): Component? {
  val internalNode = getLayoutRoot(lithoView)?.node ?: return null
  return internalNode.components.firstOrNull { it.javaClass == clazz }
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
  return findComponentRecursively(clazz, layoutRoot)
}

/** Returns a component of the given class from the ComponentTree or null if not found */
fun findComponentInLithoView(lithoView: LithoView, clazz: KClass<out Component>): Component? {
  return findComponentInLithoView(lithoView, clazz.java)
}

/**
 * Returns a list of all components of the given classes from the ComponentTree or an empty list if
 * not found
 */
fun findAllComponentsInLithoView(
    lithoView: LithoView,
    vararg clazz: Class<out Component?>
): List<Component> {
  val componentsList = mutableListOf<Component>()
  val internalNode = getLayoutRoot(lithoView) ?: return componentsList
  findComponentsRecursively(clazz, internalNode, componentsList)
  return componentsList
}

/**
 * Returns a list of all components of the given classes from the ComponentTree or an empty list if
 * not found
 */
fun findAllComponentsInLithoView(
    lithoView: LithoView,
    vararg clazz: KClass<out Component>
): List<Component> {
  val javaClasses = mutableListOf<Class<out Component>>()
  clazz.forEach { javaClasses.add(it.java) }
  val componentsList = mutableListOf<Component>()
  val layoutRoot = getLayoutRoot(lithoView) ?: return componentsList
  findComponentsRecursively(javaClasses.toTypedArray(), layoutRoot, componentsList)
  return componentsList
}

private fun getLayoutRoot(lithoView: LithoView): LithoLayoutResult? {
  val commitedLayoutState =
      lithoView.componentTree?.committedLayoutState
          ?: throw IllegalStateException(
              "No ComponentTree/Committed Layout/Layout Root found. Please call render() first")
  return commitedLayoutState.layoutRoot
}

/**
 * Recursively goes through nodes in a component tree, returns a component of a given class or null
 * if not found
 */
private fun findComponentRecursively(
    clazz: Class<out Component?>,
    layoutResult: LithoLayoutResult?
): Component? {
  layoutResult ?: return null

  val internalNode = layoutResult.node
  val component = internalNode.components.firstOrNull { it.javaClass == clazz }
  if (component != null) {
    return component
  }
  if (layoutResult is NestedTreeHolderResult) {
    return findComponentRecursively(clazz, layoutResult.nestedResult)
  } else {
    val childCount = internalNode.childCount
    for (i in 0 until childCount) {
      val childComponent = findComponentRecursively(clazz, layoutResult.getChildAt(i))
      if (childComponent != null) {
        return childComponent
      }
    }
  }
  return null
}
/**
 * Recursively goes through nodes in a component tree, and adds component of given class to the list
 * or empty list if not found
 */
private fun findComponentsRecursively(
    clazzArray: Array<out Class<out Component?>>,
    layoutResult: LithoLayoutResult?,
    componentsList: MutableList<Component>
) {
  layoutResult ?: return

  val internalNode = layoutResult.node
  val components = internalNode.components.filter { clazzArray.contains(it.javaClass) }
  if (components != null) {
    componentsList.addAll(components)
  }

  if (layoutResult is NestedTreeHolderResult) {
    findComponentsRecursively(clazzArray, layoutResult.nestedResult, componentsList)
  } else {
    val childCount = internalNode.childCount
    for (i in 0 until childCount) {
      findComponentsRecursively(clazzArray, layoutResult.getChildAt(i), componentsList)
    }
  }
}
