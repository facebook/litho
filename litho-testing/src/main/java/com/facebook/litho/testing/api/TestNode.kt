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

package com.facebook.litho.testing.api

import com.facebook.litho.AttributeKey
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.EventHandler

/**
 * A test node is used to hold all the information needed to represent a given component in the
 * testing tree. This information is be used by testing actions (filter, interaction, assertion) and
 * also to represent them in a visual way (e.g String representation).
 */
class TestNode(private val component: Component) {

  val componentType: Class<*> = component::class.java

  private val commonPropsAttributes =
      mapOf(
          TestNodeAttributes.TestKey to component.commonProps?.testKey,
          TestNodeAttributes.Enabled to (component.commonProps?.isEnabled ?: false),
          TestNodeAttributes.ContentDescription to component.commonProps?.contentDescription)

  val clickHandler: EventHandler<ClickEvent>?
    get() = component.commonProps?.clickHandler

  fun <T> getAttribute(key: AttributeKey<T>): T =
      commonPropsAttributes[key] as? T ?: component.getAttribute(key)

  var parent: TestNode? = null

  var children: List<TestNode> = mutableListOf()
}

internal object TestNodeAttributes {

  val TestKey: AttributeKey<String?> = AttributeKey<String?>("testKey")

  val ContentDescription: AttributeKey<CharSequence?> =
      AttributeKey<CharSequence?>("contentDescription")

  val Enabled: AttributeKey<Boolean?> = AttributeKey<Boolean?>("isEnabled")
}
