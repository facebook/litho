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
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions
import org.junit.Test

class FiltersTest {

  private val counterKey = AttributeKey<Int>("counter")

  @Test
  fun `hasType successfully matches when the test node component has the same class`() {
    val testNode = TestNode(DummyComponent())
    val filter = hasType<DummyComponent>()

    Assertions.assertThat(filter.matches(testNode)).isTrue
  }

  @Test
  fun `hasType fails to match when the test node component has the another class`() {
    val testNode = TestNode(DummyComponent())
    val filter = hasType<Text>()

    Assertions.assertThat(filter.matches(testNode)).isFalse
  }

  @Test
  fun `hasAttribute successfully matches when the attribute has the correct value`() {
    val component = DummyComponent()
    component.setDebugAttributeKey(counterKey, 10)

    val filter = hasAttribute(counterKey, 10)
    val testNode = TestNode(component)
    Assertions.assertThat(filter.matches(testNode)).isTrue
  }

  @Test
  fun `hasAttribute fails to match when the attribute has a different value`() {
    val component = DummyComponent()
    component.setDebugAttributeKey(counterKey, 10)

    val filter = hasAttribute(counterKey, 20)
    val testNode = TestNode(component)
    Assertions.assertThat(filter.matches(testNode)).isFalse
  }

  private class DummyComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Text("Hello")
    }
  }
}
