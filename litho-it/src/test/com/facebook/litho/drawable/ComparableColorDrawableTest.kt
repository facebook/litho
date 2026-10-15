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

package com.facebook.litho.drawable

import android.graphics.Color
import com.facebook.litho.Style
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.background
import com.facebook.litho.view.backgroundColor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComparableColorDrawableTest {

  @Test
  fun create_notNull() {
    val drawable = ComparableColorDrawable.create(Color.RED)
    assertThat(drawable).isNotNull
  }

  @Test
  fun equals_sameColor_returnsTrue() {
    val drawable1 = ComparableColorDrawable.create(Color.RED)
    val drawable2 = ComparableColorDrawable.create(Color.RED)
    assertThat(drawable1 == drawable2).isTrue
    assertThat(drawable2 == drawable1).isTrue
    assertThat(drawable1.equals(drawable2)).isTrue
  }

  @Test
  fun equals_differentColor_returnsFalse() {
    val drawable1 = ComparableColorDrawable.create(Color.RED)
    val drawable2 = ComparableColorDrawable.create(Color.BLUE)
    assertThat(drawable1 == drawable2).isFalse
    assertThat(drawable2 == drawable1).isFalse
  }

  @Test
  fun equals_sameInstance_returnsTrue() {
    val drawable = ComparableColorDrawable.create(Color.RED)
    assertThat(drawable == drawable).isTrue
    assertThat(drawable.equals(drawable)).isTrue
  }

  @Test
  fun equals_null_returnsFalse() {
    val drawable = ComparableColorDrawable.create(Color.RED)
    assertThat(drawable.equals(null)).isFalse
  }

  @Test
  fun equals_differentType_returnsFalse() {
    val drawable = ComparableColorDrawable.create(Color.RED)
    val other = ComparableGradientDrawable()
    assertThat(drawable.equals(other)).isFalse
    assertThat(other.equals(drawable)).isFalse
  }

  @Test
  fun hashCode_sameColor_isEqual() {
    val drawable1 = ComparableColorDrawable.create(Color.RED)
    val drawable2 = ComparableColorDrawable.create(Color.RED)
    assertThat(drawable1.hashCode()).isEqualTo(drawable2.hashCode())
    assertThat(drawable1.hashCode()).isEqualTo(Color.RED)
  }

  @Test
  fun hashCode_differentColor_isNotEqual() {
    val drawable1 = ComparableColorDrawable.create(Color.RED)
    val drawable2 = ComparableColorDrawable.create(Color.BLUE)
    assertThat(drawable1.hashCode()).isNotEqualTo(drawable2.hashCode())
  }

  @Test
  fun isEquivalentTo_sameColor_returnsTrue() {
    val drawable1 = ComparableColorDrawable.create(Color.GREEN)
    val drawable2 = ComparableColorDrawable.create(Color.GREEN)
    assertThat(drawable1.isEquivalentTo(drawable2)).isTrue
    assertThat(drawable2.isEquivalentTo(drawable1)).isTrue
  }

  @Test
  fun isEquivalentTo_differentColor_returnsFalse() {
    val drawable1 = ComparableColorDrawable.create(Color.GREEN)
    val drawable2 = ComparableColorDrawable.create(Color.YELLOW)
    assertThat(drawable1.isEquivalentTo(drawable2)).isFalse
  }

  @Test
  fun isEquivalentTo_differentType_returnsFalse() {
    val drawable1 = ComparableColorDrawable.create(Color.BLACK)
    val drawable2 = ComparableGradientDrawable()
    assertThat(drawable1.isEquivalentTo(drawable2)).isFalse
  }

  // Style equivalence tests - these would fail before equals was implemented

  @Test
  fun style_background_equality_sameColor_returnsTrue() {
    val style1 = Style.background(ComparableColorDrawable.create(Color.RED))
    val style2 = Style.background(ComparableColorDrawable.create(Color.RED))
    assertThat(style1 == style2).isTrue
    assertThat(style1.hashCode()).isEqualTo(style2.hashCode())
  }

  @Test
  fun style_background_equality_differentColor_returnsFalse() {
    val style1 = Style.background(ComparableColorDrawable.create(Color.RED))
    val style2 = Style.background(ComparableColorDrawable.create(Color.BLUE))
    assertThat(style1 == style2).isFalse
  }

  @Test
  fun style_backgroundColor_equality_sameColor_returnsTrue() {
    // backgroundColor internally creates ComparableColorDrawable
    val style1 = Style.backgroundColor(Color.CYAN)
    val style2 = Style.backgroundColor(Color.CYAN)
    assertThat(style1 == style2).isTrue
    assertThat(style1.hashCode()).isEqualTo(style2.hashCode())
  }

  @Test
  fun style_backgroundColor_equality_differentColor_returnsFalse() {
    val style1 = Style.backgroundColor(Color.CYAN)
    val style2 = Style.backgroundColor(Color.MAGENTA)
    assertThat(style1 == style2).isFalse
  }

  @Test
  fun style_combined_equality_sameColor_returnsTrue() {
    val style1 = Style.backgroundColor(Color.RED) + Style.backgroundColor(Color.BLUE)
    val style2 = Style.backgroundColor(Color.RED) + Style.backgroundColor(Color.BLUE)
    assertThat(style1 == style2).isTrue
  }

  @Test
  fun style_plusOperator_equality_sameBackground_returnsTrue() {
    val style1 = Style.background(ComparableColorDrawable.create(Color.RED))
    val style2 = Style.background(ComparableColorDrawable.create(Color.RED))
    val combined1 = Style.backgroundColor(Color.GREEN) + style1
    val combined2 = Style.backgroundColor(Color.GREEN) + style2
    assertThat(combined1 == combined2).isTrue
  }
}
