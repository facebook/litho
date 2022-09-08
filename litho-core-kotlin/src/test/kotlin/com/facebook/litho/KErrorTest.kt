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

import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Unit tests for [useError]. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class KErrorTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()
  @Rule @JvmField val expectedException = ExpectedException.none()

  @Test
  fun useError_errorHandled() {
    lateinit var stateRef: AtomicReference<List<Exception>>

    class CrashingKComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        throw RuntimeException("crash from kotlin component")
      }
    }

    class UseErrorComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val errorState = useState { listOf<Exception>() }
        stateRef = AtomicReference(errorState.value)
        useErrorBoundary { exception: Exception ->
          assertThat(exception.message).contains("crash from kotlin component")
          errorState.update { prevErrors -> prevErrors + listOf(exception) }
        }

        return if (errorState.value.isEmpty()) CrashingKComponent() else Text("error caught")
      }
    }

    lithoViewRule.render { UseErrorComponent() }
    lithoViewRule.idle()

    val errorList = stateRef.get()
    assertThat(errorList.size).isEqualTo(1)
    assertThat(errorList.get(0).message).isEqualTo("crash from kotlin component")
  }

  @Test
  fun useError_noErrorHandlingSoErrorNotHandled() {
    expectedException.expect(RuntimeException::class.java)
    expectedException.expectMessage("crash from kotlin component")

    class CrashingKComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        throw RuntimeException("crash from kotlin component")
      }
    }

    class UseErrorComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        // useError not implemented, so no error handling
        return CrashingKComponent()
      }
    }

    lithoViewRule.render { UseErrorComponent() }
    lithoViewRule.idle()
  }

  @Test
  fun useError_errorHandledFromTwoLevelsDown() {
    lateinit var stateRef: AtomicReference<List<Exception>>

    class CrashingKComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        throw RuntimeException("crash from kotlin component 2 levels down")
      }
    }

    class IntermediaryKComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return CrashingKComponent()
      }
    }

    class UseErrorComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val errorState = useState { listOf<Exception>() }
        stateRef = AtomicReference(errorState.value)
        useErrorBoundary { exception: Exception ->
          assertThat(exception.message).contains("crash from kotlin component 2 levels down")
          errorState.update { prevErrors -> prevErrors + listOf(exception) }
        }
        return if (errorState.value.isEmpty()) IntermediaryKComponent() else Text("error caught")
      }
    }

    lithoViewRule.render { UseErrorComponent() }
    lithoViewRule.idle()
    val errorList = stateRef.get()
    assertThat(errorList.size).isEqualTo(1)
    assertThat(errorList.get(0).message).isEqualTo("crash from kotlin component 2 levels down")
  }

  @Test
  fun useError_noErrorNoHandling() {
    class NoncrashingKComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row()
      }
    }

    class UseErrorComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        useErrorBoundary { exception: Exception ->
          throw RuntimeException("There was no error, so why has useError run?!")
        }
        return NoncrashingKComponent()
      }
    }

    lithoViewRule.render { UseErrorComponent() }
    lithoViewRule.idle()
  }

  @Test
  fun useError_errorHandledFromUseState() {
    lateinit var stateRef: AtomicReference<List<Exception>>

    class CrashingKComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        useState { throw RuntimeException("crash from kotlin component's state") }
        return Row()
      }
    }

    class UseErrorComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val errorState = useState { listOf<Exception>() }
        stateRef = AtomicReference(errorState.value)
        useErrorBoundary { exception: Exception ->
          assertThat(exception.message).contains("crash from kotlin component's state")
          errorState.update { prevErrors -> prevErrors + listOf(exception) }
        }

        return if (errorState.value.isEmpty()) CrashingKComponent() else Text("error caught")
      }
    }

    lithoViewRule.render { UseErrorComponent() }
    lithoViewRule.idle()
    val errorList = stateRef.get()
    assertThat(errorList.size).isEqualTo(1)
    assertThat(errorList.get(0).message).isEqualTo("crash from kotlin component's state")
  }
}
