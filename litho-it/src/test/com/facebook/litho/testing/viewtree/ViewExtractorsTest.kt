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
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.facebook.litho.it.R
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.LooperMode

/** Tests [ViewExtractors] */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ViewExtractorsTest {

  private lateinit var view: View
  private lateinit var textView: TextView
  private lateinit var goneTextView: TextView
  private lateinit var imageView: ImageView
  private lateinit var goneImageView: ImageView
  private lateinit var lithoDrawable: Drawable

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()

    @Suppress("DEPRECATION")
    lithoDrawable = activity.resources.getDrawable(R.drawable.litho)

    view = View(activity)

    textView = TextView(activity)
    textView.text = "example"

    goneTextView = TextView(activity)
    goneTextView.text = "gone"
    goneTextView.visibility = View.GONE

    imageView = ImageView(activity)
    imageView.setImageDrawable(lithoDrawable)

    goneImageView = ImageView(activity)
    goneImageView.setImageDrawable(lithoDrawable)
    goneImageView.visibility = View.GONE
  }

  @Test
  fun testGetTextFrotextViewHasTextContent() {
    assertThat(ViewExtractors.GET_TEXT_FUNCTION.apply(textView)).contains("example")
  }

  @Test
  fun testGetTextPrintsVisibity() {
    assertThat(ViewExtractors.GET_TEXT_FUNCTION.apply(textView)).contains("view is visible")
    assertThat(ViewExtractors.GET_TEXT_FUNCTION.apply(goneTextView)).contains("view is not visible")
  }

  @Test
  fun testViewWithoutText() {
    assertThat(ViewExtractors.GET_TEXT_FUNCTION.apply(view)).contains("No text found")
  }

  @Test
  fun testGetDrawableOutOfImageView() {
    assertThat(ViewExtractors.GET_DRAWABLE_FUNCTION.apply(imageView))
        .contains(lithoDrawable.toString())
  }

  @Test
  fun testGetDrawablePrintsVisibity() {
    assertThat(ViewExtractors.GET_DRAWABLE_FUNCTION.apply(imageView)).contains("view is visible")
    assertThat(ViewExtractors.GET_DRAWABLE_FUNCTION.apply(goneImageView))
        .contains("view is not visible")
  }
}
