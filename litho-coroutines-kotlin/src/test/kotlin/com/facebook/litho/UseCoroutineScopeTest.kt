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

import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.lang.Exception
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [useCoroutine]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(LithoTestRunner::class)
class UseCoroutineScopeTest {

  @Rule @JvmField val lithoViewRule = LithoTestRule()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun `coroutine scope canceled when component is removed from the hierarchy`() {
    var scope: CoroutineScope? = null

    class UseCoroutineScopeComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        scope = useComponentCoroutineScope()

        return Row()
      }
    }

    class ParentComponent(val enabled: Boolean) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return if (enabled) {
          return UseCoroutineScopeComponent()
        } else {
          null
        }
      }
    }

    val lithoView = lithoViewRule.render { ParentComponent(true) }
    testDispatcher.scheduler.runCurrent()

    assertThat(scope?.isActive).isTrue()

    lithoView.setRoot(ParentComponent(false))
    testDispatcher.scheduler.runCurrent()

    assertThat(scope?.isActive).isFalse()

    lithoView.detachFromWindow().release()
  }

  @Test
  fun `coroutine scope canceled when component tree is released`() {
    var scope: CoroutineScope? = null

    class UseCoroutineScopeComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        scope = useComponentCoroutineScope()

        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineScopeComponent() }
    testDispatcher.scheduler.runCurrent()

    assertThat(scope?.isActive).isTrue()

    lithoView.detachFromWindow().release()
    testDispatcher.scheduler.runCurrent()

    assertThat(scope?.isActive).isFalse()
  }

  @Test
  fun `coroutine is launched and canceled`() {
    val coroutineCalls = mutableListOf<String>()

    class UseCoroutineScopeComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val scope = useComponentCoroutineScope()

        scope.launch {
          coroutineCalls += "launch"
          try {
            awaitCancellation()
          } finally {
            coroutineCalls += "cancel"
          }
        }

        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineScopeComponent() }
    testDispatcher.scheduler.runCurrent()

    lithoView.detachFromWindow().release()
    testDispatcher.scheduler.runCurrent()

    assertThat(coroutineCalls).containsExactly("launch", "cancel")
  }

  @Test
  fun `multiple coroutines are launched and canceled`() {
    val coroutineCalls = mutableListOf<String>()

    class UseCoroutineScopeComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val scope = useComponentCoroutineScope()

        scope.launch {
          coroutineCalls += "coroutine1: launch"
          try {
            awaitCancellation()
          } finally {
            coroutineCalls += "coroutine1: cancel"
          }
        }

        scope.launch {
          coroutineCalls += "coroutine2: launch"
          try {
            awaitCancellation()
          } finally {
            coroutineCalls += "coroutine2: cancel"
          }
        }

        scope.launch {
          coroutineCalls += "coroutine3: launch"
          try {
            awaitCancellation()
          } finally {
            coroutineCalls += "coroutine3: cancel"
          }
        }

        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineScopeComponent() }
    testDispatcher.scheduler.runCurrent()

    assertThat(coroutineCalls)
        .containsExactly("coroutine1: launch", "coroutine2: launch", "coroutine3: launch")

    coroutineCalls.clear()
    lithoView.detachFromWindow().release()
    testDispatcher.scheduler.runCurrent()

    assertThat(coroutineCalls)
        .containsExactly("coroutine1: cancel", "coroutine2: cancel", "coroutine3: cancel")
  }

  @Test
  fun `exception in one coroutine doesn't cancel other coroutines`() {
    val coroutineCalls = mutableListOf<String>()

    class UseCoroutineScopeComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val scope = useComponentCoroutineScope()

        scope.launch {
          coroutineCalls += "coroutine1: launch"
          try {
            awaitCancellation()
          } finally {
            coroutineCalls += "coroutine1: cancel"
          }
        }

        scope.launch(
            CoroutineExceptionHandler { _, exception ->
              if (exception.message == "Test exception") {
                coroutineCalls += "coroutine2: exception"
              }
            }) {
              coroutineCalls += "coroutine2: launch"
              throw Exception("Test exception")
            }

        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineScopeComponent() }
    testDispatcher.scheduler.runCurrent()

    assertThat(coroutineCalls)
        .containsExactly("coroutine1: launch", "coroutine2: launch", "coroutine2: exception")

    coroutineCalls.clear()
    lithoView.detachFromWindow().release()
    testDispatcher.scheduler.runCurrent()

    assertThat(coroutineCalls).containsExactly("coroutine1: cancel")
  }

  @Test
  fun `passing parent Job to useComponentCoroutineScope throws`() {
    class UseCoroutineScopeComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val scope = useComponentCoroutineScope(coroutineContext = Job())
        return Row()
      }
    }

    val lithoView = lithoViewRule.createTestLithoView()
    try {
      lithoView.setRoot(UseCoroutineScopeComponent())
    } catch (e: Exception) {
      assertThat(e.cause?.message)
          .isEqualTo("CoroutineContext passed to useComponentCoroutineScope can't contain a Job.")
    } finally {
      lithoView.detachFromWindow().release()
    }
  }

  @Test
  fun `useComponentCoroutineScope returns the same instance across state updates`() {
    val scopes = mutableListOf<CoroutineScope>()

    class UseCoroutineScopeComponent(val prop: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        scopes.add(useComponentCoroutineScope())
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineScopeComponent(1) }
    lithoView.setRoot(UseCoroutineScopeComponent(2))

    assertThat(scopes.size).isEqualTo(2)
    assertThat(scopes[0]).isSameAs(scopes[1])

    lithoView.detachFromWindow().release()
  }
}
