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

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LegacyDynamicPropsTest {

  private lateinit var context: ComponentContext

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false)
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testDynamicAlphaApplied() {
    val startValue = 0.8f
    val alphaDV = DynamicValue(startValue)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).widthPx(80).heightPx(80).alpha(alphaDV).build())
    assertThat(lithoView.alpha).isEqualTo(startValue)
    alphaDV.set(0.5f)
    assertThat(lithoView.alpha).isEqualTo(0.5f)
    alphaDV.set(0f)
    assertThat(lithoView.alpha).isEqualTo(0f)
    alphaDV.set(1f)
    assertThat(lithoView.alpha).isEqualTo(1f)
  }

  @Test
  fun testDynamicTranslationApplied() {
    val startValueX = 100f
    val startValueY = -100f
    val translationXDV = DynamicValue(startValueX)
    val translationYDV = DynamicValue(startValueY)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            Column.create(context)
                .widthPx(80)
                .heightPx(80)
                .translationX(translationXDV)
                .translationY(translationYDV)
                .build())
    assertThat(lithoView.translationX).isEqualTo(startValueX)
    assertThat(lithoView.translationY).isEqualTo(startValueY)
    translationXDV.set(50f)
    translationYDV.set(20f)
    assertThat(lithoView.translationX).isEqualTo(50f)
    assertThat(lithoView.translationY).isEqualTo(20f)
    translationXDV.set(-50f)
    translationYDV.set(-20f)
    assertThat(lithoView.translationX).isEqualTo(-50f)
    assertThat(lithoView.translationY).isEqualTo(-20f)
    translationXDV.set(0f)
    translationYDV.set(0f)
    assertThat(lithoView.translationX).isEqualTo(0f)
    assertThat(lithoView.translationY).isEqualTo(0f)
  }

  @Test
  fun testDynamicBackgroundColorApplied() {
    val startValue = Color.RED
    val backgroundColorDV = DynamicValue(startValue)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            Column.create(context)
                .widthPx(80)
                .heightPx(80)
                .backgroundColor(backgroundColorDV)
                .build())
    assertThat(lithoView.background).isInstanceOf(ColorDrawable::class.java)
    assertThat((lithoView.background as ColorDrawable).color).isEqualTo(startValue)
    backgroundColorDV.set(Color.BLUE)
    assertThat((lithoView.background as ColorDrawable).color).isEqualTo(Color.BLUE)
    backgroundColorDV.set(-0x77777778)
    assertThat((lithoView.background as ColorDrawable).color).isEqualTo(-0x77777778)
    backgroundColorDV.set(Color.TRANSPARENT)
    assertThat((lithoView.background as ColorDrawable).color).isEqualTo(Color.TRANSPARENT)
  }

  @Test
  fun testDynamicRotationApplied() {
    val startValue = 0f
    val rotationDV = DynamicValue(startValue)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).widthPx(80).heightPx(80).rotation(rotationDV).build())
    assertThat(lithoView.rotation).isEqualTo(startValue)
    rotationDV.set(364f)
    assertThat(lithoView.rotation).isEqualTo(364f)
    rotationDV.set(520f)
    assertThat(lithoView.rotation).isEqualTo(520f)
    rotationDV.set(-1f)
    assertThat(lithoView.rotation).isEqualTo(-1f)
  }

  @Test
  fun testDynamicScaleApplied() {
    val startValueX = 1.5f
    val startValueY = -1.5f
    val scaleXDV = DynamicValue(startValueX)
    val scaleYDV = DynamicValue(startValueY)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            Column.create(context)
                .widthPx(80)
                .heightPx(80)
                .scaleX(scaleXDV)
                .scaleY(scaleYDV)
                .build())
    assertThat(lithoView.scaleX).isEqualTo(startValueX)
    assertThat(lithoView.scaleY).isEqualTo(startValueY)
    scaleXDV.set(0.5f)
    scaleYDV.set(2f)
    assertThat(lithoView.scaleX).isEqualTo(0.5f)
    assertThat(lithoView.scaleY).isEqualTo(2f)
    scaleXDV.set(2f)
    scaleYDV.set(0.5f)
    assertThat(lithoView.scaleX).isEqualTo(2f)
    assertThat(lithoView.scaleY).isEqualTo(.5f)
    scaleXDV.set(0f)
    scaleYDV.set(0f)
    assertThat(lithoView.scaleX).isEqualTo(0f)
    assertThat(lithoView.scaleY).isEqualTo(0f)
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }
}
