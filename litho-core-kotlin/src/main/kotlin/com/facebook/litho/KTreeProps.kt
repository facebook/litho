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

/**
 * Create a TreeProp variable within a Component. The TreeProp variable is shared with its descendant components.
 * However, descendant components can override the value of the TreeProp for all its children.
 */
inline fun <reified T> DslScope.createTreeProp(initializer: () -> T) {
  createTreeProp(T::class.java, initializer())
}

@PublishedApi
internal fun <T> DslScope.createTreeProp(clazz: Class<T>, value: T) {
  if (!context.isParentTreePropsCloned) {
    context.treeProps = TreeProps.acquire(context.treeProps)
    context.isParentTreePropsCloned = true
  }
  context.treeProps?.put(clazz, value)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> DslScope.useTreeProp(): T? = context.getTreeProp(T::class.java)
