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

/**
 * Allows to associate values to [AttributeKey] in an [AttributesAcceptor] that will be stored in
 * the encapsulating [Component]
 *
 * These attributes can be used for testing purposes - allowing the filtering of test nodes or
 * asserting on their characteristics.
 *
 * @see [hasAttribute] usages on how to select/assert test nodes.
 */
fun Style.attribute(body: (AttributesAcceptor) -> Unit): Style {
  return this + AttributeStyleItem(TestAttribute, body)
}

operator fun <T> AttributesAcceptor.set(key: AttributeKey<T>, value: T) {
  setDebugAttributeKey(key, value)
}

private class AttributeStyleItem(
    override val field: TestAttribute,
    override val value: (AttributesAcceptor) -> Unit,
) : StyleItem<AttributesAcceptor.() -> Unit> {

  override fun applyToComponent(context: ComponentContext, component: Component) {
    value(component)
  }
}

private object TestAttribute : StyleItemField
