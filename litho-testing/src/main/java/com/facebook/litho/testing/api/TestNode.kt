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

import com.facebook.litho.ActionAttributeKey
import com.facebook.litho.AttributeKey
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.EventHandler
import com.facebook.litho.SpecGeneratedComponent

/**
 * A test node is used to hold all the information needed to represent a given component in the
 * testing tree. This information is be used by testing actions (filter, interaction, assertion) and
 * also to represent them in a visual way (e.g String representation).
 */
class TestNode(private val component: Component) {

  val componentType: Class<*> = component::class.java

  private val commonPropsAttributes: Map<AttributeKey<*>, *> =
      mapOf(
          TestNodeAttributes.TestKey to
              if (component is SpecGeneratedComponent) component.commonProps?.testKey else null,
          TestNodeAttributes.Enabled to
              if (component is SpecGeneratedComponent) (component.commonProps?.isEnabled ?: false)
              else false,
          TestNodeAttributes.ContentDescription to
              if (component is SpecGeneratedComponent) component.commonProps?.contentDescription
              else null,
          TestNodeActionAttributes.OnClick to
              if (component is SpecGeneratedComponent) component.commonProps?.clickHandler
              else null)

  private val componentAttributes: Map<AttributeKey<*>, *> = component.debugAttributes

  val clickHandler: EventHandler<ClickEvent>?
    get() = if (component is SpecGeneratedComponent) component.commonProps?.clickHandler else null

  fun <T> getAttribute(key: AttributeKey<T>): T =
      commonPropsAttributes[key] as? T ?: component.getDebugAttribute(key)

  val attributes: Set<Pair<AttributeKey<*>, *>> =
      (commonPropsAttributes + componentAttributes).entries.map { it.toPair() }.toSet()

  var parent: TestNode? = null

  var children: List<TestNode> = mutableListOf()
}

internal object TestNodeAttributes {

  val TestKey: AttributeKey<String?> = AttributeKey<String?>("testKey")

  val ContentDescription: AttributeKey<CharSequence?> =
      AttributeKey<CharSequence?>("contentDescription")

  val Enabled: AttributeKey<Boolean?> = AttributeKey<Boolean?>("isEnabled")
}

internal object TestNodeActionAttributes {

  val OnClick: ActionAttributeKey<EventHandler<ClickEvent>> =
      ActionAttributeKey<EventHandler<ClickEvent>>("OnClick")
}
