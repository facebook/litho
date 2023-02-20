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
package com.facebook.litho.animation

import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TransitionTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.TransitionEndCallbackTestComponent
import com.facebook.litho.widget.TransitionEndCallbackTestComponentSpec
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class TransitionEndEventTest {

  @JvmField @Rule val lithoViewRule = LithoViewRule()
  @JvmField @Rule val transitionTestRule = TransitionTestRule()

  @Test
  fun transitionEnCallback_parallelCallback_shouldShowDifferentMessages() {
    val stateUpdater =
        TransitionEndCallbackTestComponentSpec.Caller().apply {
          testType = TransitionEndCallbackTestComponentSpec.TestType.SAME_KEY
        }
    val component =
        TransitionEndCallbackTestComponent.create(lithoViewRule.context)
            .caller(stateUpdater)
            .build()

    lithoViewRule.render { component }

    Assertions.assertThat(stateUpdater.transitionEndMessage)
        .describedAs("Before starting animations")
        .isNullOrEmpty()

    stateUpdater.toggle()
    transitionTestRule.step(10)
    Assertions.assertThat(stateUpdater.transitionEndMessage)
        .describedAs("Message changed after first transition ended")
        .isEqualTo(TransitionEndCallbackTestComponentSpec.ANIM_ALPHA)

    transitionTestRule.step(20)
    Assertions.assertThat(stateUpdater.transitionEndMessage)
        .describedAs("Message changed after second transition ended")
        .isEqualTo(TransitionEndCallbackTestComponentSpec.ANIM_X)
  }

  @Test
  fun transitionEnCallback_disappearAnimation_shouldDisappearMessage() {
    val stateUpdater =
        TransitionEndCallbackTestComponentSpec.Caller().apply {
          testType = TransitionEndCallbackTestComponentSpec.TestType.DISAPPEAR
        }
    val component =
        TransitionEndCallbackTestComponent.create(lithoViewRule.context)
            .caller(stateUpdater)
            .build()

    lithoViewRule.render { component }

    Assertions.assertThat(stateUpdater.transitionEndMessage)
        .describedAs("Before starting animations")
        .isNullOrEmpty()

    transitionTestRule.step(1000)
    stateUpdater.toggle()
    transitionTestRule.step(1000)
    Assertions.assertThat(stateUpdater.transitionEndMessage)
        .describedAs("Message changed after disappear transition ended")
        .isEqualTo(TransitionEndCallbackTestComponentSpec.ANIM_DISAPPEAR)
  }
}
