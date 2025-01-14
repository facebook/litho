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

import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.LithoTestRuleResizeMode
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.alpha
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.view.wrapInView
import com.facebook.litho.widget.ComponentWithTreeProp
import com.facebook.litho.widget.TextDrawable
import com.facebook.litho.widget.treeprops.SimpleTreeProp
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LithoTestRuleTest {

  @JvmField @Rule val mLithoTestRule = LithoTestRule()
  @JvmField
  @Rule
  val manualResizeLithoTestRule = LithoTestRule(resizeMode = LithoTestRuleResizeMode.MANUAL)

  @Test
  fun onLithoTestRuleWithTreeProp_shouldPropagateTreeProp() {
    val component = ComponentWithTreeProp.create(mLithoTestRule.context).build()
    val testLithoView =
        mLithoTestRule.setTreeProp(SimpleTreeProp::class.java, SimpleTreeProp("test")).render {
          component
        }

    val item = testLithoView.lithoView.getMountItemAt(0).content
    assertThat(item).isInstanceOf(TextDrawable::class.java)
    assertThat((item as TextDrawable).text).isEqualTo("test")
  }

  @Test(expected = RuntimeException::class)
  fun onLithoTestRuleWithoutTreeProp_shouldThrowException() {
    val component = ComponentWithTreeProp.create(mLithoTestRule.context).build()
    mLithoTestRule.createTestLithoView().attachToWindow().setRoot(component).measure().layout()
  }

  @Test
  fun onLithoTestRuleExceptionOnBackgroundThread_shouldPropagateExceptionImmediately() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val randomState = useState { false }
        if (randomState.value) {
          throw Exception("Hi There!")
        }
        return Row(style = Style.width(100.px).height(100.px)) {
          child(Text(text = "some_other_text", style = Style.onClick { randomState.update(true) }))
        }
      }
    }

    val testLithoView = mLithoTestRule.render { TestComponent() }
    val thrown: Throwable =
        Assertions.catchThrowable {
          mLithoTestRule.act(testLithoView) { clickOnText("some_other_text") }
        }

    assertThat(thrown).isInstanceOf(RuntimeException::class.java)
    assertThat((thrown.stackTraceToString()).contains("Timed out!")).isFalse
    assertThat(thrown).hasStackTraceContaining("Hi There!")
  }

  @Test
  fun `given dynamic size component when size is updated then view has updated size on resetting root`() {
    val testLithoView =
        mLithoTestRule.render {
          ComponentWithDynamicSize(width = 100.px, height = 100.px, alpha = 1f)
        }

    testLithoView.findViewWithTag(ComponentWithDynamicSize.VIEW_TAG).run {
      assertThat(width).isEqualTo(100)
      assertThat(height).isEqualTo(100)
      assertThat(alpha).isEqualTo(1f)
    }

    val updatedWidth = 200
    val updatedHeight = 300
    val updatedAlpha = 0.5f
    testLithoView.setRoot(
        ComponentWithDynamicSize(
            width = updatedWidth.px, height = updatedHeight.px, alpha = updatedAlpha))

    with(testLithoView.findViewWithTag(ComponentWithDynamicSize.VIEW_TAG)) {
      assertThat(width).isEqualTo(updatedWidth)
      assertThat(height).isEqualTo(updatedHeight)
      assertThat(alpha).isEqualTo(updatedAlpha)
    }
  }

  @Test
  fun `given rule with manual resizing and dynamic size component when size is updated then view size not updated on resetting root`() {
    val width = 100
    val height = 200
    val alpha = 1f
    val testLithoView =
        manualResizeLithoTestRule.render { ComponentWithDynamicSize(width.px, height.px, alpha) }

    with(testLithoView.findViewWithTag(ComponentWithDynamicSize.VIEW_TAG)) {
      assertThat(this.width).isEqualTo(width)
      assertThat(this.height).isEqualTo(height)
      assertThat(this.alpha).isEqualTo(alpha)
    }

    testLithoView.setRoot(ComponentWithDynamicSize(width = 200.px, height = 300.px, alpha = .5f))

    with(testLithoView.findViewWithTag(ComponentWithDynamicSize.VIEW_TAG)) {
      assertThat(this.width).isEqualTo(width)
      assertThat(this.height).isEqualTo(height)
      assertThat(this.alpha).isEqualTo(alpha)
    }
  }

  @Test
  fun `given dynamic size component when size is updated then view has updated size on resetting root async`() {
    val testLithoView =
        mLithoTestRule.render {
          ComponentWithDynamicSize(width = 100.px, height = 100.px, alpha = 1f)
        }

    testLithoView.findViewWithTag(ComponentWithDynamicSize.VIEW_TAG).run {
      assertThat(width).isEqualTo(100)
      assertThat(height).isEqualTo(100)
      assertThat(alpha).isEqualTo(1f)
    }

    val updatedWidth = 200
    val updatedHeight = 300
    val updatedAlpha = 0.5f
    testLithoView.setRootAsync(
        ComponentWithDynamicSize(
            width = updatedWidth.px, height = updatedHeight.px, alpha = updatedAlpha))

    with(testLithoView.findViewWithTag(ComponentWithDynamicSize.VIEW_TAG)) {
      assertThat(width).isEqualTo(updatedWidth)
      assertThat(height).isEqualTo(updatedHeight)
      assertThat(alpha).isEqualTo(updatedAlpha)
    }
  }

  @Test
  fun `given rule with manual resizing and dynamic size component when size is updated then view size not updated on resetting root async`() {
    val width = 100
    val height = 200
    val alpha = 1f
    val testLithoView =
        manualResizeLithoTestRule.render { ComponentWithDynamicSize(width.px, height.px, alpha) }

    with(testLithoView.findViewWithTag(ComponentWithDynamicSize.VIEW_TAG)) {
      assertThat(this.width).isEqualTo(width)
      assertThat(this.height).isEqualTo(height)
      assertThat(this.alpha).isEqualTo(alpha)
    }

    testLithoView.setRootAsync(
        ComponentWithDynamicSize(width = 200.px, height = 300.px, alpha = .5f))

    with(testLithoView.findViewWithTag(ComponentWithDynamicSize.VIEW_TAG)) {
      assertThat(this.width).isEqualTo(width)
      assertThat(this.height).isEqualTo(height)
      assertThat(this.alpha).isEqualTo(alpha)
    }
  }
}

private class ComponentWithDynamicSize(
    private val width: Dimen,
    private val height: Dimen,
    private val alpha: Float,
) : KComponent() {
  override fun ComponentScope.render(): Component {
    val style = Style.width(width).height(height).alpha(alpha).viewTag(VIEW_TAG)

    return Row(style = Style.wrapInView()) { child(Row(style = style) { child(Text("test")) }) }
  }

  companion object {
    const val VIEW_TAG = "test_view"
  }
}
