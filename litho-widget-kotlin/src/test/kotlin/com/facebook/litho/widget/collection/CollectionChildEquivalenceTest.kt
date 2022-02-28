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

package com.facebook.litho.widget.collection

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.ClickEvent
import com.facebook.litho.ComponentScope
import com.facebook.litho.Style
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s children */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class CollectionChildEquivalenceTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `test children with null fields are equivalent`() {
    val next = CollectionChild("", null)
    val previous = CollectionChild("", null)

    assertThat(isChildEquivalent(previous, next)).isTrue
  }

  @Test
  fun `test children with equal deps are equivalent`() {
    val next = CollectionChild("", null, deps = arrayOf("A"))
    val previous = CollectionChild("", null, deps = arrayOf("A"))

    assertThat(isChildEquivalent(previous, next)).isTrue
  }

  @Test
  fun `test children with unequal deps are not equivalent`() {
    val next = CollectionChild("", null, deps = arrayOf("A"))
    val previous = CollectionChild("", null, deps = arrayOf("B"))

    assertThat(isChildEquivalent(previous, next)).isFalse
  }

  @Test
  fun `test isChildEquivalent checks props and common props`() {
    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              isChildEquivalent(
                  CollectionChild("", Text("Test")),
                  CollectionChild("", Text("Test")),
              ))
          .isTrue

      assertThat(
              isChildEquivalent(
                  CollectionChild("", Text("Test")),
                  CollectionChild("", Text("Production")),
              ))
          .isFalse

      val onClick = { _: ClickEvent -> }
      assertThat(
              isChildEquivalent(
                  CollectionChild("", Text("Test", style = Style.onClick(onClick))),
                  CollectionChild("", Text("Test", style = Style.onClick(onClick))),
              ))
          .isTrue

      assertThat(
              isChildEquivalent(
                  CollectionChild("", Text("", style = Style.onClick {})),
                  CollectionChild("", Text("", style = Style.onClick {})),
              ))
          .isFalse
    }
  }
}
