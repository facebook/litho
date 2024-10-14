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
import com.facebook.litho.it.R.dimen.test_dimen
import com.facebook.litho.it.R.dimen.test_dimen_float
import com.facebook.litho.it.R.style.TestTheme
import com.facebook.litho.testing.LegacyLithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.TextInput
import com.facebook.rendercore.utils.MeasureSpecUtils.unspecified
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ResolveResTest {

  @JvmField @Rule val legacyLithoTestRule: LegacyLithoTestRule = LegacyLithoTestRule()

  @Before
  fun setup() {
    legacyLithoTestRule.useContext(
        ComponentContext(
            ContextThemeWrapper(ApplicationProvider.getApplicationContext(), TestTheme)))
  }

  @Test
  fun testDefaultDimenWidthRes() {
    val c = legacyLithoTestRule.context
    val column = Column.create(c).widthRes(test_dimen).build()
    legacyLithoTestRule
        .setRootAndSizeSpecSync(column, unspecified(), unspecified())
        .measure()
        .layout()
    val dimen = c.resources.getDimensionPixelSize(test_dimen)
    assertThat(legacyLithoTestRule.lithoView.width).isEqualTo(dimen)
  }

  @Test
  fun testDefaultDimenPaddingRes() {
    val c = legacyLithoTestRule.context
    val column = Column.create(c).paddingRes(YogaEdge.LEFT, test_dimen).build()
    legacyLithoTestRule
        .setRootAndSizeSpecSync(column, unspecified(), unspecified())
        .measure()
        .layout()
    val dimen = c.resources.getDimensionPixelSize(test_dimen)
    assertThat(legacyLithoTestRule.lithoView.width).isEqualTo(dimen)
  }

  @Test
  fun testFloatDimenWidthRes() {
    val c = legacyLithoTestRule.context
    val column = Column.create(c).widthRes(test_dimen_float).build()
    legacyLithoTestRule
        .setRootAndSizeSpecSync(column, unspecified(), unspecified())
        .measure()
        .layout()
    val dimen = c.resources.getDimensionPixelSize(test_dimen_float)
    assertThat(legacyLithoTestRule.lithoView.width).isEqualTo(dimen)
  }

  @Test
  fun testFloatDimenPaddingRes() {
    val c = legacyLithoTestRule.context
    val row =
        Row.create(c).child(TextInput.create(c).paddingRes(YogaEdge.LEFT, test_dimen_float)).build()
    legacyLithoTestRule.attachToWindow().setSizePx(100, 100).setRoot(row).measure().layout()
    val dimen = c.resources.getDimensionPixelSize(test_dimen_float)
    assertThat(legacyLithoTestRule.lithoView.getChildAt(0).paddingLeft).isEqualTo(dimen)
  }
}
