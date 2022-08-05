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

import android.graphics.Color
import com.facebook.litho.ClickEvent
import com.facebook.litho.ComponentScope
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s children */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class CollectionChildEquivalenceTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private lateinit var lazyList: LazyCollection

  @Before
  fun before() {
    with(ComponentScope(lithoViewRule.context)) { lazyList = LazyList {} as LazyCollection }
  }

  @Test
  fun `test children with null fields are equivalent`() {
    val next = CollectionChild("", null)
    val previous = CollectionChild("", null)

    assertThat(lazyList.isChildEquivalent(previous, next)).isTrue
  }

  @Test
  fun `test children with equal deps are equivalent`() {
    val next = CollectionChild("", null, deps = arrayOf("A"))
    val previous = CollectionChild("", null, deps = arrayOf("A"))

    assertThat(lazyList.isChildEquivalent(previous, next)).isTrue
  }

  @Test
  fun `test children with unequal deps are not equivalent`() {
    val next = CollectionChild("", null, deps = arrayOf("A"))
    val previous = CollectionChild("", null, deps = arrayOf("B"))

    assertThat(lazyList.isChildEquivalent(previous, next)).isFalse
  }

  @Test
  fun `test isChildEquivalent checks props`() {
    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Text("Test")),
                  CollectionChild("", Text("Test")),
              ))
          .isTrue

      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Text("Test")),
                  CollectionChild("", Text("Production")),
              ))
          .isFalse
    }
  }

  @Test
  fun `test isChildEquivalent checks nested props`() {
    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Row { child(Text("Test")) }),
                  CollectionChild("", Row { child(Text("Test")) }),
              ))
          .isTrue

      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Row { child(Text("Test")) }),
                  CollectionChild("", Row { child(Text("Production")) }),
              ))
          .isFalse
    }
  }

  @Test
  fun `test isChildEquivalent checks lambdas`() {
    with(ComponentScope(lithoViewRule.context)) {
      val onClick = { _: ClickEvent -> }
      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Text("Test", style = Style.onClick(action = onClick))),
                  CollectionChild("", Text("Test", style = Style.onClick(action = onClick))),
              ))
          .isTrue

      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Text("", style = Style.onClick {})),
                  CollectionChild("", Text("", style = Style.onClick {})),
              ))
          .isFalse
    }
  }

  @Test
  fun `test isChildEquivalent checks nested lambdas`() {
    with(ComponentScope(lithoViewRule.context)) {
      val onClick = { _: ClickEvent -> }
      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild(
                      "", Row { child(Text("Test", style = Style.onClick(action = onClick))) }),
                  CollectionChild(
                      "", Row { child(Text("Test", style = Style.onClick(action = onClick))) }),
              ))
          .isTrue

      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Row { child(Text("Test", style = Style.onClick {})) }),
                  CollectionChild("", Row { child(Text("Test", style = Style.onClick {})) }),
              ))
          .isFalse
    }
  }

  @Test
  fun `test isChildEquivalent checks common props`() {
    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Text("Test", style = Style.backgroundColor(Color.RED))),
                  CollectionChild("", Text("Test", style = Style.backgroundColor(Color.RED))),
              ))
          .isTrue

      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild("", Text("Test", style = Style.backgroundColor(Color.RED))),
                  CollectionChild("", Text("Test", style = Style.backgroundColor(Color.BLUE))),
              ))
          .isFalse
    }
  }

  @Test
  fun `test isChildEquivalent checks nested common props`() {
    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild(
                      "", Row { child(Text("Test", style = Style.backgroundColor(Color.RED))) }),
                  CollectionChild(
                      "", Row { child(Text("Test", style = Style.backgroundColor(Color.RED))) }),
              ))
          .isTrue

      assertThat(
              lazyList.isChildEquivalent(
                  CollectionChild(
                      "", Row { child(Text("Test", style = Style.backgroundColor(Color.RED))) }),
                  CollectionChild(
                      "", Row { child(Text("Test", style = Style.backgroundColor(Color.BLUE))) }),
              ))
          .isFalse
    }
  }
}
