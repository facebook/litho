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

  private val collectionChildrenBuilder = mutableListOf<CollectionChild>()
  private var nextStaticId = 0
  private var typeToFreq: MutableMap<Int, Int>? = null

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
    if (typeToFreq == null) {
      typeToFreq = mutableMapOf()
    }

    return typeToFreq?.let {
      it[typeId] = (it[typeId] ?: 0) + 1
      "${typeId}:${it[typeId]}"
    }
  }

  fun child(
      component: Component?,
      id: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
  ) {
    val resolvedId = getResolvedId(id, component)
    component ?: return
    collectionChildrenBuilder.add(
        CollectionChild(resolvedId, component, null, isSticky, isFullSpan, spanSize, null))
  }

  fun child(
      id: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
      deps: Array<Any?>,
      componentFunction: () -> Component?,
  ) {
    collectionChildrenBuilder.add(
        CollectionChild(
            getResolvedId(id), null, componentFunction, isSticky, isFullSpan, spanSize, deps))
  }
}
