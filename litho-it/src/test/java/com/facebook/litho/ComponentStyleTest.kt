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

import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.it.R
import com.facebook.litho.it.R.attr.testAttrLargePadding
import com.facebook.litho.it.R.attr.testAttrLargeText
import com.facebook.litho.it.R.style.PaddingStyle
import com.facebook.litho.it.R.style.TestTheme
import com.facebook.litho.it.R.style.TextSizeStyle
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
@Ignore("t16280359")
class ComponentStyleTest {

  private var dimen = 0
  private var largeDimen = 0
  private lateinit var context: ComponentContext

  @JvmField @Rule var legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    context =
        ComponentContext(
            ContextThemeWrapper(ApplicationProvider.getApplicationContext(), TestTheme))
    legacyLithoViewRule.useContext(context)
    dimen = context.resources.getDimensionPixelSize(R.dimen.test_dimen)
    largeDimen = context.resources.getDimensionPixelSize(R.dimen.test_large_dimen)
  }

  @Test
  fun testStyleProp() {
    val component = Text.create(context, 0, TextSizeStyle).text("text").build()
    assertThat(Whitebox.getInternalState<Any>(component, "textSize") as Int).isEqualTo(dimen)
  }

  @Test
  fun testOverrideStyleProp() {
    val component =
        Text.create(context, 0, TextSizeStyle).text("text").textSizePx(2 * dimen).build()
    assertThat(Whitebox.getInternalState<Any>(component, "textSize") as Int).isEqualTo(2 * dimen)
  }

  @Test
  fun testStyleLayout() {
    val component = Text.create(context, 0, PaddingStyle).text("text").build()
    val result =
        LegacyLithoViewRule.getRootLayout(
            legacyLithoViewRule, component, SizeSpec.UNSPECIFIED, SizeSpec.UNSPECIFIED)
    assertThat(result?.yogaNode?.getPadding(YogaEdge.LEFT)).isEqualTo(dimen)
  }

  @Test
  fun testOverrideStyleLayout() {
    val component =
        Text.create(context, 0, PaddingStyle)
            .text("text")
            .paddingPx(YogaEdge.ALL, dimen * 2)
            .build()
    val result =
        LegacyLithoViewRule.getRootLayout(
            legacyLithoViewRule, component, SizeSpec.UNSPECIFIED, SizeSpec.UNSPECIFIED)
    assertThat(result?.yogaNode?.getPadding(YogaEdge.LEFT)).isEqualTo(2 * dimen)
  }

  @Test
  fun testAttributeStyleProp() {
    val component = Text.create(context, testAttrLargeText, 0).text("text").build()
    assertThat(Whitebox.getInternalState<Any>(component, "textSize") as Int).isEqualTo(largeDimen)
  }

  @Test
  fun testOverrideAttributeStyleProp() {
    val component =
        Text.create(context, testAttrLargeText, 0).text("text").textSizePx(dimen).build()
    assertThat(Whitebox.getInternalState<Any>(component, "textSize") as Int).isEqualTo(dimen)
  }

  @Test
  fun testAttributeStyleLayout() {
    val component = Text.create(context, testAttrLargePadding, 0).text("text").build()
    val result =
        LegacyLithoViewRule.getRootLayout(
            legacyLithoViewRule, component, SizeSpec.UNSPECIFIED, SizeSpec.UNSPECIFIED)
    assertThat(result?.yogaNode?.getPadding(YogaEdge.LEFT)).isEqualTo(largeDimen)
  }

  @Test
  fun testOverrideAttributeStyleLayout() {
    val component =
        Text.create(context, testAttrLargePadding, 0)
            .text("text")
            .paddingPx(YogaEdge.ALL, dimen * 2)
            .build()
    val result =
        LegacyLithoViewRule.getRootLayout(
            legacyLithoViewRule, component, SizeSpec.UNSPECIFIED, SizeSpec.UNSPECIFIED)
    assertThat(result?.yogaNode?.getPadding(YogaEdge.LEFT)).isEqualTo(2 * dimen)
  }

  @Test
  fun testStyleResOverridenByAttrResForProp() {
    val component = Text.create(context, testAttrLargeText, TextSizeStyle).text("text").build()
    assertThat(Whitebox.getInternalState<Any>(component, "textSize") as Int).isEqualTo(largeDimen)
  }

  @Test
  fun testStyleResOverridenByAttrResForLayout() {
    val component = Text.create(context, testAttrLargePadding, PaddingStyle).text("text").build()
    val result =
        LegacyLithoViewRule.getRootLayout(
            legacyLithoViewRule, component, SizeSpec.UNSPECIFIED, SizeSpec.UNSPECIFIED)
    assertThat(result?.yogaNode?.getPadding(YogaEdge.LEFT)).isEqualTo(largeDimen)
  }
}
