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

import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.CardHeaderComponent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class MountStateMountTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = legacyLithoViewRule.context
  }

  @Test
  fun unmountAll_mountStateNeedsRemount() {
    val root =
        Column.create(context).child(CardHeaderComponent.create(context).title("Title")).build()
    legacyLithoViewRule.setRoot(root).attachToWindow().measure().layout()
    val mountDelegateTarget = legacyLithoViewRule.lithoView.mountDelegateTarget
    assertThat(mountDelegateTarget.needsRemount()).isFalse
    legacyLithoViewRule.lithoView.unmountAllItems()
    assertThat(mountDelegateTarget.needsRemount()).isTrue
  }
}
