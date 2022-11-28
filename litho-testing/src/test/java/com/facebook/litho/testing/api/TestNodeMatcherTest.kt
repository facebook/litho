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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestNodeMatcherTest {

  private val testNode = TestNode(CounterComponent(10))

  private val alwaysFailMatcher = TestNodeMatcher("always fails") { false }
  private val alwaysPassMatcher = TestNodeMatcher("always pass") { true }
  private val counterComponentTypeMatcher = hasType(CounterComponent::class.java)
  private val textTypeMatcher = hasType(Text::class.java)

  @Test
  fun `the TestNodeMatcher created via an and connection of two other matchers will match if both match`() {
    val testNode = TestNode(CounterComponent(10))
    val andConnectionMatcher = counterComponentTypeMatcher and alwaysPassMatcher
    assertThat(andConnectionMatcher.matches(testNode)).isTrue
  }

  @Test
  fun `the TestNodeMatcher created via an and connection of two other matchers will fail if one of them fails to match`() {
    val testNode = TestNode(CounterComponent(10))
    val andConnectionMatcher = counterComponentTypeMatcher and alwaysFailMatcher
    assertThat(andConnectionMatcher.matches(testNode)).isFalse
  }

  @Test
  fun `the TestNodeMatcher created via an or connection of two other matchers will match if one matches`() {
    val testNode = TestNode(CounterComponent(10))
    val orConnectionMatcher = alwaysFailMatcher or counterComponentTypeMatcher
    assertThat(orConnectionMatcher.matches(testNode)).isTrue
  }

  @Test
  fun `the TestNodeMatcher created via an or connection of two other matchers will fail if both of them fail to match`() {
    val orConnectionMatcher = textTypeMatcher or alwaysFailMatcher
    assertThat(orConnectionMatcher.matches(testNode)).isFalse
  }

  @Test
  fun `the TestNodeMatcher negation will pass if the original matcher fails`() {
    assertThat(counterComponentTypeMatcher.matches(testNode)).isTrue

    val negationMatcher = !counterComponentTypeMatcher
    assertThat(negationMatcher.matches(testNode)).isFalse
  }

  @Test
  fun `the TestNodeMatcher negation will fail if the original matcher passes`() {
    assertThat(textTypeMatcher.matches(testNode)).isFalse

    val negationMatcher = !textTypeMatcher
    assertThat(negationMatcher.matches(testNode)).isTrue
  }

  internal class CounterComponent(private val counter: Int) : KComponent() {

    override fun ComponentScope.render(): Component {
      return Text("Counter: $counter")
    }
  }
}
