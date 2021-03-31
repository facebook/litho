/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.view.viewTag
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for setting a manual key with the [key] function. */
@RunWith(AndroidJUnit4::class)
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

      return Row(style = Style.viewTag(state.value))
    }
  }

  @Test
  fun useState_createNewLayoutWithSameManualKey_stateIsSame() {
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(KeyTestParentComponent(manualKey = "my_key", i = 0))
        .attachToWindow()
        .measure()
        .layout()

    val originalTag = lithoViewRule.lithoView.tag
    assertThat(originalTag).isNotNull()

    lithoViewRule.setRoot(KeyTestParentComponent("my_key", i = 1))

    assertThat(lithoViewRule.lithoView.tag).isEqualTo(originalTag)
  }

  @Test
  fun useState_createNewLayoutWithDifferentManualKey_stateIsRecreated() {
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(KeyTestParentComponent(manualKey = "my_key", i = 0))
        .attachToWindow()
        .measure()
        .layout()

    val originalTag = lithoViewRule.lithoView.tag
    assertThat(originalTag).isNotNull()

    lithoViewRule.setRoot(KeyTestParentComponent("a_different_key", i = 1))

    assertThat(lithoViewRule.lithoView.tag).isNotEqualTo(originalTag)
  }
}
