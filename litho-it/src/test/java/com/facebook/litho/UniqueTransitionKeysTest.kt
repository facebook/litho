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

import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.transition.transitionKey
import java.lang.RuntimeException
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class UniqueTransitionKeysTest {
  @JvmField @Rule val lithoViewRule: LithoViewRule = LithoViewRule()
  @JvmField @Rule var expectedException: ExpectedException = ExpectedException.none()

  @Test
  fun testGetTransitionKeyMapping() {
    class HasUniqueTransitionKeys : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row {
          child(
              Row(
                  style =
                      Style.transitionKey(context, "test", Transition.TransitionKeyType.GLOBAL)))
          child(
              Row(
                  style =
                      Style.transitionKey(context, "test2", Transition.TransitionKeyType.GLOBAL)))
        }
      }
    }

    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) { HasUniqueTransitionKeys() }
    testLithoView.componentTree.mainThreadLayoutState!!.transitionIdMapping
  }

  @Test
  fun testThrowIfSameTransitionKeyAppearsMultipleTimes() {
    expectedException.expect(RuntimeException::class.java)
    expectedException.expectMessage(
        "The transitionId 'TransitionId{\"test\", GLOBAL}' is defined multiple times in the same layout.")
    class HasNonUniqueTransitionKeys : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row {
          child(
              Row(
                  style =
                      Style.transitionKey(context, "test", Transition.TransitionKeyType.GLOBAL)))
          child(
              Row(
                  style =
                      Style.transitionKey(context, "test", Transition.TransitionKeyType.GLOBAL)))
        }
      }
    }

    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) { HasNonUniqueTransitionKeys() }
    Assertions.assertThat(testLithoView.componentTree.mainThreadLayoutState!!.transitionIdMapping)
        .isNotNull
  }
}
