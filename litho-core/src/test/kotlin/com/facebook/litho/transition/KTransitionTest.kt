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

@file:OptIn(ExperimentalLithoApi::class)

package com.facebook.litho.transition

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.Diff
import com.facebook.litho.KComponent
import com.facebook.litho.Transition
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import kotlin.properties.Delegates
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class KTransitionTest {

  @Rule @JvmField val mLithoTestRule = LithoTestRule()

  @Before
  fun setup() {
    ComponentsConfiguration.isAnimationDisabled = false
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.isAnimationDisabled = true
  }

  @Test
  fun useTransition_nullDependency_updatesTransitionOnce() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var updateCounter = 0

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition(null) {
          updateCounter++
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent())
    assertThat(updateCounter).isEqualTo(1)

    testLithoView.mount(TestComponent())
    testLithoView.mount(TestComponent())
    assertThat(updateCounter).isEqualTo(1)
  }

  @Test
  fun useTransition_fixedDependency_updatesTransitionOnce() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var updateCounter = 0

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition(Unit) {
          updateCounter++
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent())
    assertThat(updateCounter).isEqualTo(1)

    testLithoView.mount(TestComponent())
    testLithoView.mount(TestComponent())
    assertThat(updateCounter).isEqualTo(1)
  }

  @Test
  fun useTransition_dynamicDependency_updatesTransitionAlways() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var updateCounter = 0

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition(Any()) {
          updateCounter++
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent())
    assertThat(updateCounter).isEqualTo(1)

    testLithoView.mount(TestComponent())
    testLithoView.mount(TestComponent())
    assertThat(updateCounter).isEqualTo(3)
  }

  @Test
  fun useTransition_changingDependency_updatesTransitionOnChange() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var updateCounter = 0

    class TestComponent(val dep: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition(dep) {
          updateCounter++
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent(10))
    assertThat(updateCounter).isEqualTo(1)

    testLithoView.mount(TestComponent(10))
    testLithoView.mount(TestComponent(100))
    assertThat(updateCounter).isEqualTo(2)
  }

  @Test
  fun useTransition_twoFixedDependencies_updatesTransitionOnce() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var updateCounter = 0

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition("hello", 100) {
          updateCounter++
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent())
    assertThat(updateCounter).isEqualTo(1)

    testLithoView.mount(TestComponent())
    testLithoView.mount(TestComponent())
    assertThat(updateCounter).isEqualTo(1)
  }

  @Test
  fun useTransition_anyChangingDependency_updatesTransitionOnChange() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var updateCounter = 0

    class TestComponent(val dep: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition("hello", dep) {
          updateCounter++
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent(10))
    assertThat(updateCounter).isEqualTo(1)

    testLithoView.mount(TestComponent(10))
    testLithoView.mount(TestComponent(100))
    assertThat(updateCounter).isEqualTo(2)
  }

  @Test
  fun useTransition_multipleChangingDependencies_updatesTransitionOnAnyChange() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var updateCounter = 0

    class TestComponent(val dep1: Int, val dep2: String) : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition("hello", dep1, dep2) {
          updateCounter++
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent(10, "Hello"))
    assertThat(updateCounter).isEqualTo(1)

    testLithoView.mount(TestComponent(100, "Hello"))
    assertThat(updateCounter).isEqualTo(2)

    testLithoView.mount(TestComponent(100, "World"))
    assertThat(updateCounter).isEqualTo(3)

    testLithoView.mount(TestComponent(1000, "Goodbye"))
    assertThat(updateCounter).isEqualTo(4)
  }

  @Test
  fun useTransition_changingDependency_propagatesCorrectDiff() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var outerDiff1: Diff<Int> by Delegates.notNull()
    var outerDiff2: Diff<String> by Delegates.notNull()

    class TestComponent(val intDep: Int, val stringDep: String) : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition(intDep, stringDep) {
          outerDiff1 = diffOf(intDep)
          outerDiff2 = diffOf(stringDep)
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent(10, "first"))
    with(outerDiff1) {
      assertThat(previous).isEqualTo(null)
      assertThat(next).isEqualTo(10)
    }
    with(outerDiff2) {
      assertThat(previous).isEqualTo(null)
      assertThat(next).isEqualTo("first")
    }

    testLithoView.mount(TestComponent(100, "second"))
    with(outerDiff1) {
      assertThat(previous).isEqualTo(10)
      assertThat(next).isEqualTo(100)
    }
    with(outerDiff2) {
      assertThat(previous).isEqualTo("first")
      assertThat(next).isEqualTo("second")
    }
  }

  @Test
  fun useTransition_alternateChangingDependency_propagatesCorrectDiff() {
    val testLithoView = mLithoTestRule.createTestLithoView()
    var diff: Diff<Int> by Delegates.notNull()

    class TestComponent(private val dep: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useTransition(dep) {
          diff = diffOf(dep)
          Transition.allLayout()
        }
        return Text(text = "Hello")
      }
    }
    testLithoView.mount(TestComponent(10))
    assertThat(diff.previous).isEqualTo(null)
    assertThat(diff.next).isEqualTo(10)

    // Mount with the same dependency
    testLithoView.mount(TestComponent(10))
    // Then mount with a different dependency
    testLithoView.mount(TestComponent(100))

    assertThat(diff.previous).isEqualTo(10)
    assertThat(diff.next).isEqualTo(100)
  }

  private fun TestLithoView.mount(component: Component) {
    ComponentTestHelper.mountComponent(lithoView, componentTree, component)
  }
}
