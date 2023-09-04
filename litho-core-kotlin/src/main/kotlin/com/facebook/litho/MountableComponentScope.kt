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

import com.facebook.rendercore.Mountable

/** The implicit receiver for [MountableComponent.render] call. */
class MountableComponentScope
internal constructor(context: ComponentContext, resolveContext: ResolveContext) :
    ComponentScope(context, resolveContext) {
  internal val binders: ArrayList<DynamicPropsHolder<Any?, Mountable<*>>> by lazy { ArrayList(2) }
  var shouldExcludeFromIncrementalMount: Boolean = false

  /**
   * Creates a binding between the dynamic value, and the content’s property.
   *
   * @param defaultValue value that will be set to the Content after unbind
   * @param valueSetter function or function reference that will set the dynamic value on the
   *   content
   */
  fun <ValueT, ContentT> DynamicValue<ValueT>.bindTo(
      defaultValue: ValueT,
      valueSetter: (ContentT, ValueT) -> Unit
  ) {
    addBinder(DynamicPropsHolder(this, defaultValue, valueSetter))
  }

  /**
   * Indicates whether the component skips Incremental Mount.
   *
   * @param shouldExclude if this is true then the Component will not be involved in Incremental
   *   Mount.
   */
  fun excludeFromIncrementalMount(shouldExclude: Boolean) {
    shouldExcludeFromIncrementalMount = shouldExclude
  }

  /** Retrieves a styled attribute value for provided {@param id}. */
  fun getIntAttrValue(
      c: ComponentContext,
      id: Int,
      attrs: IntArray,
      defaultValue: Boolean
  ): Boolean {
    val a = c.obtainStyledAttributes(attrs, 0)
    var i = 0
    val size = a.indexCount
    while (i < size) {
      val attr = a.getIndex(i)
      if (attr == id) {
        return a.getInt(attr, 0) != 0
      }
      i++
    }
    a.recycle()
    return defaultValue
  }

  private fun <ValueT, ContentT> addBinder(binder: DynamicPropsHolder<ContentT, ValueT>) {
    binders.add(binder as DynamicPropsHolder<Any?, Mountable<*>>)
  }
}
