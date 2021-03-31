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

import kotlin.reflect.KClass

/**
 * Defines a single tree prop, i.e. a mapping from type to concrete instance of that type that is
 * visible to the subtree, to be used with [TreePropProvider].
 *
 * You can create a Pair with the `to` infix function, e.g. `String::class to "Hello World!"`.
 */
typealias ClassValuePair<T> = Pair<KClass<T>, T>

/**
 * A component which provides one or more tree props to the given child hierarchy. A tree prop is a
 * mapping from a type to an instance of that type which is accessible throughout the entire subtree
 * where it's defined.
 *
 * Tree props are useful for providing theme info, logging tags, or other things that generally need
 * to be available throughout a hierarchy, without having to manual thread them through as
 * individual props. Tree props can be accessed in children via [useTreeProp].
 */
@Suppress("FunctionNaming")
inline fun TreePropProvider(
    vararg props: ClassValuePair<*>,
    component: () -> Component?
): TreePropProviderImpl? {
  val resolvedComponent = component() ?: return null
  return TreePropProviderImpl(props, child = resolvedComponent)
}

/** See [TreePropProvider]. */
class TreePropProviderImpl(
    private val props: Array<out ClassValuePair<*>>,
    private val child: Component
) : KComponent() {
  override fun ComponentScope.render(): Component {
    props.forEach { createTreeProp(it.first.javaObjectType, it.second) }
    return child
  }
}

private fun <T> ComponentScope.createTreeProp(clazz: Class<out T>, value: T) {
  if (!context.isParentTreePropsCloned) {
    context.treeProps = TreeProps.acquire(context.treeProps)
    context.isParentTreePropsCloned = true
  }
  context.treeProps?.put(clazz, value)
}

/**
 * Returns the instance registered for this type in this hierarchy. Tree props are registered for a
 * sub-hierarchy via [TreePropProvider] or [com.facebook.litho.annotations.OnCreateTreeProp] in the
 * specs API.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> ComponentScope.useTreeProp(): T? = context.getTreeProp(T::class.java)
