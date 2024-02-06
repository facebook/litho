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

import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.viewTag
import com.facebook.litho.view.wrapInView
import com.facebook.rendercore.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for setting a manual key with the [key] function. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ManualKeyTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private class KeyTestParentComponent(val manualKey: String, val i: Int) : KComponent() {
    override fun ComponentScope.render(): Component? {
      return key(manualKey) { KeyTestChildComponent() }
    }
  }

  private class KeyTestChildComponent() : KComponent() {
    override fun ComponentScope.render(): Component? {
      val state = useState { Object() }

      return Row(style = Style.viewTag(state.value).height(100.dp).width(200.dp).wrapInView())
    }
  }

  @Test
  fun useState_createNewLayoutWithSameManualKey_stateIsSame() {
    val testLithoView =
        lithoViewRule.render(widthPx = exactly(100), heightPx = exactly(100)) {
          KeyTestParentComponent(manualKey = "my_key", i = 0)
        }

    val originalTag = testLithoView.lithoView.tag
    assertThat(originalTag).isNotNull()

    testLithoView.setRoot(KeyTestParentComponent("my_key", i = 1))

    assertThat(testLithoView.lithoView.tag).isEqualTo(originalTag)
  }

  @Test
  fun useState_createNewLayoutWithDifferentManualKey_stateIsRecreated() {
    val testLithoView =
        lithoViewRule.render(widthPx = exactly(100), heightPx = exactly(100)) {
          KeyTestParentComponent(manualKey = "my_key", i = 0)
        }

    val originalTag = testLithoView.lithoView.tag
    assertThat(originalTag).isNotNull()

    testLithoView.setRoot(KeyTestParentComponent("a_different_key", i = 1))

    assertThat(testLithoView.lithoView.tag).isNotEqualTo(originalTag)
  }
}
