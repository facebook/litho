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

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.it.R
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.ViewGroupWithLithoViewChildren
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.SolidColor
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextInput
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class MountStateViewTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = legacyLithoViewRule.context
  }

  @Test
  fun testViewPaddingAndBackground() {
    val color = 0xFFFF0000.toInt()
    val component: InlineLayoutSpec =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(
                      TextInput.create(c)
                          .paddingPx(YogaEdge.LEFT, 5)
                          .paddingPx(YogaEdge.TOP, 6)
                          .paddingPx(YogaEdge.RIGHT, 7)
                          .paddingPx(YogaEdge.BOTTOM, 8)
                          .backgroundColor(color))
                  .build()
        }
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val lithoView = legacyLithoViewRule.lithoView
    val child = lithoView.getChildAt(0)
    val background = child.background
    assertThat(child.paddingLeft).isEqualTo(5)
    assertThat(child.paddingTop).isEqualTo(6)
    assertThat(child.paddingRight).isEqualTo(7)
    assertThat(child.paddingBottom).isEqualTo(8)
    assertThat(background).isInstanceOf(ColorDrawable::class.java)
    assertThat((background as ColorDrawable).color).isEqualTo(color)
  }

  @Test
  fun testSettingCustomPaddingOverridesDefaultBackgroundPadding() {
    val c =
        ComponentContext(
            ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.TestTheme_BackgroundWithPadding))
    val component = TextInput.create(c).paddingPx(YogaEdge.ALL, 9).build()
    legacyLithoViewRule.useContext(c)
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    val lithoView = legacyLithoViewRule.lithoView
    val child = lithoView.getChildAt(0)
    assertThat(child.paddingLeft).isEqualTo(9)
    assertThat(child.paddingTop).isEqualTo(9)
    assertThat(child.paddingRight).isEqualTo(9)
    assertThat(child.paddingBottom).isEqualTo(9)
  }

  @Test
  fun testSettingOneSidePaddingClearsTheRest() {
    val c =
        ComponentContext(
            ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.TestTheme_BackgroundWithPadding))
    val component = TextInput.create(c).paddingPx(YogaEdge.LEFT, 12).build()
    legacyLithoViewRule.useContext(c)
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    val lithoView = legacyLithoViewRule.lithoView
    val child = lithoView.getChildAt(0)
    assertThat(child.paddingLeft).isEqualTo(12)
    assertThat(child.paddingTop).isZero
    assertThat(child.paddingRight).isZero
    assertThat(child.paddingBottom).isZero
  }

  @Test
  fun testComponentDeepUnmount() {
    val lifecycleTracker = LifecycleTracker()
    val testComponent =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker).build()
    val mountedTestComponent: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(Wrapper.create(c).delegate(testComponent).widthPx(10).heightPx(10))
                  .build()
        }
    legacyLithoViewRule.setRoot(mountedTestComponent).attachToWindow().measure().layout()
    assertThat(lifecycleTracker.isMounted).isTrue
    val viewGroup = ViewGroupWithLithoViewChildren(context.androidContext)
    val child = legacyLithoViewRule.lithoView
    viewGroup.addView(child)
    legacyLithoViewRule.setRoot(TestViewComponent.create(context).testView(viewGroup).build())
    assertThat(lifecycleTracker.isMounted).isTrue
    legacyLithoViewRule.lithoView.unmountAllItems()
    assertThat(lifecycleTracker.isMounted).isFalse
  }

  @Test
  fun onMountedContentSize_shouldBeEqualToLayoutOutputSize() {
    val component =
        Column.create(context)
            .child(TextInput.create(context).widthPx(100).heightPx(100))
            .child(SolidColor.create(context).color(Color.BLACK).widthPx(100).heightPx(100))
            .child(
                Text.create(context)
                    .text("hello world")
                    .widthPx(80)
                    .heightPx(80)
                    .paddingPx(YogaEdge.ALL, 10)
                    .marginPx(YogaEdge.ALL, 10))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val root = legacyLithoViewRule.lithoView
    val view = root.getChildAt(0)
    val viewBounds = root.getMountItemAt(0).renderTreeNode.getAbsoluteBounds(Rect())
    assertThat(view.width).isEqualTo(viewBounds.width())
    assertThat(view.height).isEqualTo(viewBounds.height())
    val item1 = root.getMountItemAt(1)
    val drawableOutputBounds = item1.renderTreeNode.getAbsoluteBounds(Rect())
    val drawablesActualBounds = (item1.content as Drawable).bounds
    assertThat(drawablesActualBounds.width()).isEqualTo(drawableOutputBounds.width())
    assertThat(drawablesActualBounds.height()).isEqualTo(drawableOutputBounds.height())
    val item2 = root.getMountItemAt(2)
    val textOutputBounds = item2.renderTreeNode.getAbsoluteBounds(Rect())
    val textActualBounds = (item2.content as Drawable).bounds
    assertThat(textActualBounds.width()).isEqualTo(textOutputBounds.width())
    assertThat(textActualBounds.height()).isEqualTo(textOutputBounds.height())
  }

  @Test
  fun onMountContentWithPadded9PatchDrawable_shouldNotSetPaddingOnHost() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true)
    val component =
        Column.create(context)
            .backgroundRes(R.drawable.background_with_padding)
            .child(Text.create(context).text("hello world").textSizeSp(20f))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    assertThat(legacyLithoViewRule.lithoView.paddingTop).isEqualTo(0)
    assertThat(legacyLithoViewRule.lithoView.paddingRight).isEqualTo(0)
    assertThat(legacyLithoViewRule.lithoView.paddingBottom).isEqualTo(0)
    assertThat(legacyLithoViewRule.lithoView.paddingLeft).isEqualTo(0)
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }
}
