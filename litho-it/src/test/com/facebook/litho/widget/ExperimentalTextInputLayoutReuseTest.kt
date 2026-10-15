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

package com.facebook.litho.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.Spanned
import androidx.core.graphics.drawable.toDrawable
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.KComponent
import com.facebook.litho.Size
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.unspecified
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * A re-layout with equal props reuses [ExperimentalTextInput]'s measurement, like the legacy
 * TextInput's @ShouldUpdate. Measuring sets the text on an EditText, which runs its input filters,
 * so a counting filter counts the measurements.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
class ExperimentalTextInputLayoutReuseTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `a background built on each render that draws the same keeps the measurement`() {
    val counter = CountingFilter()
    val tree = ComponentTree.create(lithoTestRule.context).build()

    layout(tree, Input(counter, Color.RED.toDrawable()))
    val measurements = counter.calls
    assertThat(measurements).isPositive()

    layout(tree, Input(counter, Color.RED.toDrawable()))
    assertThat(counter.calls).isEqualTo(measurements)
  }

  @Test
  fun `a background that draws differently measures again`() {
    val counter = CountingFilter()
    val tree = ComponentTree.create(lithoTestRule.context).build()

    layout(tree, Input(counter, Color.RED.toDrawable()))
    val measurements = counter.calls

    layout(tree, Input(counter, Color.BLUE.toDrawable()))
    assertThat(counter.calls).isGreaterThan(measurements)
  }

  @Test
  fun `a length filter built on each render with the same max keeps the measurement`() {
    val counter = CountingFilter()
    val tree = ComponentTree.create(lithoTestRule.context).build()

    layout(tree, Input(counter, background = null, maxLength = 10))
    val measurements = counter.calls

    layout(tree, Input(counter, background = null, maxLength = 10))
    assertThat(counter.calls).isEqualTo(measurements)

    layout(tree, Input(counter, background = null, maxLength = 20))
    assertThat(counter.calls).isGreaterThan(measurements)
  }

  @Test
  fun `replacing the default background with a transparent one measures without its padding`() {
    val counter = CountingFilter()
    val tree = ComponentTree.create(lithoTestRule.context).build()
    val transparent = Color.TRANSPARENT.toDrawable()
    val heightWithoutPadding = measuredHeight(transparent)

    assertThat(layout(tree, counter, UNSET_DRAWABLE)).isNotEqualTo(heightWithoutPadding)

    assertThat(layout(tree, counter, transparent)).isEqualTo(heightWithoutPadding)
  }

  @Test
  fun `backgrounds without a constant state are measured with their own padding`() {
    val counter = CountingFilter()
    val tree = ComponentTree.create(lithoTestRule.context).build()
    val padded = PaddedDrawable(padding = 40)
    val heightWithPadding = measuredHeight(padded)

    assertThat(layout(tree, counter, PaddedDrawable(padding = 0))).isNotEqualTo(heightWithPadding)

    assertThat(layout(tree, counter, padded)).isEqualTo(heightWithPadding)
  }

  private fun layout(tree: ComponentTree, root: Component) {
    tree.setRootAndSizeSpecSync(root, exactly(500), unspecified())
  }

  private fun layout(tree: ComponentTree, counter: CountingFilter, background: Drawable): Int {
    val size = Size()
    tree.setRootAndSizeSpecSync(Input(counter, background), exactly(500), unspecified(), size)
    return size.height
  }

  private fun measuredHeight(background: Drawable): Int =
      layout(ComponentTree.create(lithoTestRule.context).build(), CountingFilter(), background)

  /** A drawable without a ConstantState, like many custom drawables. */
  private class PaddedDrawable(private val padding: Int) : Drawable() {
    override fun draw(canvas: Canvas): Unit = Unit

    override fun setAlpha(alpha: Int): Unit = Unit

    override fun setColorFilter(colorFilter: ColorFilter?): Unit = Unit

    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    override fun getPadding(padding: Rect): Boolean {
      padding.set(0, this.padding, 0, this.padding)
      return true
    }
  }

  private class CountingFilter : InputFilter {
    var calls = 0

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int,
    ): CharSequence? {
      calls++
      return null
    }
  }

  private class Input(
      private val counter: CountingFilter,
      private val background: Drawable?,
      private val maxLength: Int? = null,
  ) : KComponent() {
    // A new filter list on every render is the case under test, so it must not be cached.
    @Suppress("KComponentUncachedAllocationDetector")
    override fun ComponentScope.render(): Component = Column {
      child(
          ExperimentalTextInput(
              initialText = "hello",
              inputBackground = background,
              inputFilters =
                  if (maxLength == null) {
                    listOf(counter)
                  } else {
                    listOf(counter, InputFilter.LengthFilter(maxLength))
                  },
          ),
      )
    }
  }
}
