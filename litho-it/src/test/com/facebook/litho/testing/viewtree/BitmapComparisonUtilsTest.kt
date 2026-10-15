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

package com.facebook.litho.testing.viewtree

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.facebook.litho.it.R
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode.NATIVE

/** Tests [BitmapComparisonUtils] */
@GraphicsMode(NATIVE)
@RunWith(LithoTestRunner::class)
class BitmapComparisonUtilsTest {

  private lateinit var activity: Activity
  private lateinit var redDrawable: Drawable
  private lateinit var blueDrawable: Drawable
  private lateinit var lithoDrawable: Drawable

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).create().get()

    redDrawable = Color.RED.toDrawable()
    blueDrawable = Color.BLUE.toDrawable()

    lithoDrawable = checkNotNull(ContextCompat.getDrawable(activity, R.drawable.litho))
  }

  @Test
  fun testDrawablesMatch_identicalDrawables_returnsTrue() {
    val drawable1 = Color.RED.toDrawable()
    val drawable2 = Color.RED.toDrawable()

    assertThat(BitmapComparisonUtils.drawablesMatch(drawable1, drawable2)).isTrue()
  }

  @Test
  fun testDrawablesMatch_differentDrawables_returnsFalse() {
    assertThat(BitmapComparisonUtils.drawablesMatch(redDrawable, blueDrawable)).isFalse()
  }

  @Test
  fun testDrawablesMatch_sameDrawableInstance_returnsTrue() {
    assertThat(BitmapComparisonUtils.drawablesMatch(redDrawable, redDrawable)).isTrue()
  }

  @Test
  fun testDrawablesMatch_resourceDrawable_matchesItself() {
    val anotherLithoDrawable = checkNotNull(ContextCompat.getDrawable(activity, R.drawable.litho))

    assertThat(BitmapComparisonUtils.drawablesMatch(lithoDrawable, anotherLithoDrawable)).isTrue()
  }

  @Test
  fun testDrawablesMatch_resourceDrawable_doesNotMatchColorDrawable() {
    assertThat(BitmapComparisonUtils.drawablesMatch(lithoDrawable, redDrawable)).isFalse()
  }

  @Test
  fun testViewMatchesDrawable_imageViewWithSameDrawable_returnsTrue() {
    val imageView = ImageView(activity)
    val drawable = Color.GREEN.toDrawable()
    imageView.setImageDrawable(drawable)

    // Need to measure and layout the view for drawToBitmap to work
    imageView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    )
    imageView.layout(0, 0, 100, 100)

    assertThat(BitmapComparisonUtils.viewMatchesDrawable(imageView, drawable)).isTrue()
  }

  @Test
  fun testViewMatchesDrawable_imageViewWithDifferentDrawable_returnsFalse() {
    val imageView = ImageView(activity)
    imageView.setImageDrawable(Color.GREEN.toDrawable())

    // Need to measure and layout the view for drawToBitmap to work
    imageView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    )
    imageView.layout(0, 0, 100, 100)

    assertThat(BitmapComparisonUtils.viewMatchesDrawable(imageView, Color.YELLOW.toDrawable()))
        .isFalse()
  }

  @Test
  fun testViewMatchesDrawable_viewSmallerThanDefaultSize_returnsTrueForSameDrawable() {
    val imageView = ImageView(activity)
    val drawable = Color.CYAN.toDrawable()
    imageView.setImageDrawable(drawable)

    imageView.measure(
        View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY),
    )
    imageView.layout(0, 0, 50, 50)

    assertThat(BitmapComparisonUtils.viewMatchesDrawable(imageView, drawable)).isTrue()
  }
}
