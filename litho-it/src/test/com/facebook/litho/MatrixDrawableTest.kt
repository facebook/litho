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

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView.ScaleType
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class MatrixDrawableTest {

  @Test
  fun getVisualBounds_centerInside_returnsPaintedDrawableBounds() {
    val drawable = GradientDrawable().apply { setSize(20, 10) }
    val subject = createMatrixDrawable(drawable, ScaleType.CENTER_INSIDE)

    assertThat(subject.getVisualBounds()).isEqualTo(Rect(50, 65, 70, 75))
  }

  @Test
  fun getVisualBounds_fitCenter_returnsScaledDrawableBounds() {
    val drawable = GradientDrawable().apply { setSize(20, 10) }
    val subject = createMatrixDrawable(drawable, ScaleType.FIT_CENTER)

    assertThat(subject.getVisualBounds()).isEqualTo(Rect(10, 45, 110, 95))
  }

  @Test
  fun getVisualBounds_centerCrop_clipsToContainerBounds() {
    val drawable = GradientDrawable().apply { setSize(20, 10) }
    val subject = createMatrixDrawable(drawable, ScaleType.CENTER_CROP)

    assertThat(subject.getVisualBounds()).isEqualTo(Rect(10, 20, 110, 120))
  }

  @Test
  fun getVisualBounds_oversizedCenter_clipsToContainerBounds() {
    val drawable = GradientDrawable().apply { setSize(200, 200) }
    val subject = createMatrixDrawable(drawable, ScaleType.CENTER)

    assertThat(subject.getVisualBounds()).isEqualTo(Rect(10, 20, 110, 120))
  }

  private fun createMatrixDrawable(
      drawable: Drawable,
      scaleType: ScaleType,
  ): MatrixDrawable<Drawable> =
      MatrixDrawable<Drawable>().apply {
        setBounds(10, 20, 110, 120)
        mount(drawable, DrawableMatrix.create(drawable, scaleType, 100, 100))
        bind(drawable.intrinsicWidth, drawable.intrinsicHeight)
      }
}
