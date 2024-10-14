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
import android.graphics.drawable.GradientDrawable
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComparableGradientDrawableTest {

  @Test
  fun ComparableDrawable_create() {
    val comparable = ComparableGradientDrawable()
    assertThat(comparable).isNotNull
  }

  @Test
  fun ComparableDrawable_equal_simple() {
    val comparable1 = ComparableGradientDrawable()
    val comparable2 = ComparableGradientDrawable()
    assertThat(comparable1.isEquivalentTo(comparable1)).isTrue
    assertThat(comparable1.isEquivalentTo(comparable2)).isTrue
  }

  @Test
  fun ComparableDrawable_equal_detailed() {
    val comparable1 = ComparableGradientDrawable()
    comparable1.orientation = GradientDrawable.Orientation.BOTTOM_TOP
    comparable1.setColors(intArrayOf(0, 0))
    comparable1.setCornerRadii(floatArrayOf(0f, 1f, 2f, 4f))
    comparable1.setSize(1, 2)
    comparable1.setShape(GradientDrawable.OVAL)
    comparable1.setGradientRadius(2f)
    comparable1.setGradientType(GradientDrawable.LINEAR_GRADIENT)
    val comparable2 = ComparableGradientDrawable()
    comparable2.orientation = GradientDrawable.Orientation.BOTTOM_TOP
    comparable2.setColors(intArrayOf(0, 0))
    comparable2.setCornerRadii(floatArrayOf(0f, 1f, 2f, 4f))
    comparable2.setSize(1, 2)
    comparable2.setShape(GradientDrawable.OVAL)
    comparable2.setGradientRadius(2f)
    comparable2.setGradientType(GradientDrawable.LINEAR_GRADIENT)
    assertThat(comparable1.isEquivalentTo(comparable1)).isTrue
    assertThat(comparable1.isEquivalentTo(comparable2)).isTrue
  }

  @Test
  fun ComparableDrawable_equal_detailed_with_null() {
    val comparable1 = ComparableGradientDrawable()
    comparable1.orientation = null
    comparable1.setColors(null)
    comparable1.setColor(0xFF)
    comparable1.setCornerRadii(null)
    val comparable2 = ComparableGradientDrawable()
    comparable2.orientation = null
    comparable2.setColors(null)
    comparable2.setColor(0xFF)
    comparable2.setCornerRadii(null)
    assertThat(comparable1.isEquivalentTo(comparable1)).isTrue
    assertThat(comparable1.isEquivalentTo(comparable2)).isTrue
  }

  @Test
  fun ComparableDrawable_not_equal_simple() {
    val comparable1 = ComparableGradientDrawable()
    val comparable2 = ComparableColorDrawable.create(Color.BLACK)
    assertThat(comparable1.isEquivalentTo(comparable2)).isFalse
  }

  @Test
  fun ComparableDrawable_not_equal_detailed() {
    val comparable1 = ComparableGradientDrawable()
    comparable1.orientation = GradientDrawable.Orientation.BOTTOM_TOP
    comparable1.setColors(intArrayOf(0, 0))
    comparable1.setCornerRadii(floatArrayOf(0f, 1f, 2f, 4f))
    comparable1.setSize(1, 2)
    comparable1.setShape(GradientDrawable.OVAL)
    comparable1.setGradientRadius(2f)
    comparable1.setGradientType(GradientDrawable.LINEAR_GRADIENT)
    val comparable2 = ComparableGradientDrawable()
    comparable2.orientation = GradientDrawable.Orientation.BOTTOM_TOP
    comparable2.setColors(intArrayOf(1, 1))
    comparable2.setCornerRadii(floatArrayOf(4f, 3f, 2f, 1f))
    comparable2.setSize(2, 1)
    comparable2.setShape(GradientDrawable.RECTANGLE)
    comparable2.setGradientRadius(1f)
    comparable2.setGradientType(GradientDrawable.RADIAL_GRADIENT)
    assertThat(comparable1.isEquivalentTo(comparable2)).isFalse
  }

  @Test
  fun ComparableDrawable_not_equal_detailed_with_null() {
    val comparable1 = ComparableGradientDrawable()
    comparable1.orientation = GradientDrawable.Orientation.BOTTOM_TOP
    comparable1.setColors(intArrayOf(0, 0))
    comparable1.setCornerRadii(floatArrayOf(0f, 1f, 2f, 4f))
    val comparable2 = ComparableGradientDrawable()
    comparable2.orientation = null
    comparable2.setColors(null)
    comparable2.setCornerRadii(null)
    assertThat(comparable1.isEquivalentTo(comparable2)).isFalse
  }
}
