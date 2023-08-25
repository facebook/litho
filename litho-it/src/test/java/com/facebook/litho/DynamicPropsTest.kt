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

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.SizeSpec.EXACTLY
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.animated.alpha
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.DynamicPropsResetValueTester
import com.facebook.litho.widget.DynamicPropsResetValueTesterSpec
import com.facebook.rendercore.MountItem
import com.facebook.rendercore.primitives.ExactSizeConstraintsLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class DynamicPropsTest {

  private lateinit var context: ComponentContext

  val config = ComponentsConfiguration.create().shouldAddHostViewForRootComponent(true).build()

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule(config)

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testDynamicAlphaApplied() {
    val startValue = 0.8f
    val alphaDV = DynamicValue(startValue)
    val lithoView =
        legacyLithoViewRule
            .attachToWindow()
            .setRoot(Column.create(context).widthPx(80).heightPx(80).alpha(alphaDV).build())
            .measure()
            .layout()
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    val hostView = lithoView.getChildAt(0)
    assertThat(hostView.alpha).isEqualTo(startValue)
    alphaDV.set(0.5f)
    assertThat(hostView.alpha).isEqualTo(0.5f)
    alphaDV.set(0f)
    assertThat(hostView.alpha).isEqualTo(0f)
    alphaDV.set(1f)
    assertThat(hostView.alpha).isEqualTo(1f)
  }

  @Test
  fun testAttributesAndDynamicPropDuringUpdate() {
    val startValue = 0.8f
    val alphaDV = DynamicValue(startValue)
    val component1 =
        Column.create(context)
            .widthPx(80)
            .heightPx(80)
            .backgroundColor(Color.GREEN)
            .alpha(alphaDV)
            .build()
    val component2 =
        Column.create(context)
            .widthPx(80)
            .heightPx(80)
            .backgroundColor(Color.MAGENTA)
            .alpha(alphaDV)
            .build()
    legacyLithoViewRule
        .setRoot(component1)
        .setSizeSpecs(makeSizeSpec(80, EXACTLY), makeSizeSpec(80, EXACTLY))
        .attachToWindow()
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView

    // Ensure we have one view.
    assertThat(lithoView.childCount).isEqualTo(1)
    var hostView = lithoView.getChildAt(0)

    // Ensure alpha DV is correct
    assertThat(hostView.alpha).isEqualTo(startValue)

    // Ensure background attribute is present and has the correct value.
    assertThat(hostView.background).isNotNull
    assertThat((hostView.background as ColorDrawable).color).isEqualTo(Color.GREEN)

    // Mount component2, which is identical to component1, except with a different bg, invoking
    // an update sequence.
    legacyLithoViewRule.setRoot(component2)

    // Grab the host again
    hostView = lithoView.getChildAt(0)

    // Alter the alpha DV
    alphaDV.set(0.5f)

    // Ensure the DV is properly applied on the view
    assertThat(hostView.alpha).isEqualTo(0.5f)

    // Ensure background attribute is present and has the correct value.
    assertThat(hostView.background).isNotNull
    assertThat((hostView.background as ColorDrawable).color).isEqualTo(Color.MAGENTA)
  }

  @Test
  fun testDynamicTranslationApplied() {
    val startValueX = 100f
    val startValueY = -100f
    val translationXDV = DynamicValue(startValueX)
    val translationYDV = DynamicValue(startValueY)
    val lithoView =
        legacyLithoViewRule
            .attachToWindow()
            .setRoot(
                Column.create(context)
                    .widthPx(80)
                    .heightPx(80)
                    .translationX(translationXDV)
                    .translationY(translationYDV)
                    .build())
            .measure()
            .layout()
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    val hostView = lithoView.getChildAt(0)
    assertThat(hostView.translationX).isEqualTo(startValueX)
    assertThat(hostView.translationY).isEqualTo(startValueY)
    translationXDV.set(50f)
    translationYDV.set(20f)
    assertThat(hostView.translationX).isEqualTo(50f)
    assertThat(hostView.translationY).isEqualTo(20f)
    translationXDV.set(-50f)
    translationYDV.set(-20f)
    assertThat(hostView.translationX).isEqualTo(-50f)
    assertThat(hostView.translationY).isEqualTo(-20f)
    translationXDV.set(0f)
    translationYDV.set(0f)
    assertThat(hostView.translationX).isEqualTo(0f)
    assertThat(hostView.translationY).isEqualTo(0f)
  }

  @Test
  fun testDynamicScaleApplied() {
    val startValueX = 1.5f
    val startValueY = -1.5f
    val scaleXDV = DynamicValue(startValueX)
    val scaleYDV = DynamicValue(startValueY)
    val lithoView =
        legacyLithoViewRule
            .attachToWindow()
            .setRoot(
                Column.create(context)
                    .widthPx(80)
                    .heightPx(80)
                    .scaleX(scaleXDV)
                    .scaleY(scaleYDV)
                    .build())
            .measure()
            .layout()
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    val hostView = lithoView.getChildAt(0)
    assertThat(hostView.scaleX).isEqualTo(startValueX)
    assertThat(hostView.scaleY).isEqualTo(startValueY)
    scaleXDV.set(0.5f)
    scaleYDV.set(2f)
    assertThat(hostView.scaleX).isEqualTo(0.5f)
    assertThat(hostView.scaleY).isEqualTo(2f)
    scaleXDV.set(2f)
    scaleYDV.set(0.5f)
    assertThat(hostView.scaleX).isEqualTo(2f)
    assertThat(hostView.scaleY).isEqualTo(.5f)
    scaleXDV.set(0f)
    scaleYDV.set(0f)
    assertThat(hostView.scaleX).isEqualTo(0f)
    assertThat(hostView.scaleY).isEqualTo(0f)
  }

  @Test
  fun testDynamicBackgroundColorApplied() {
    val startValue = Color.RED
    val backgroundColorDV = DynamicValue(startValue)
    val lithoView =
        legacyLithoViewRule
            .attachToWindow()
            .setRoot(
                Column.create(context)
                    .widthPx(80)
                    .heightPx(80)
                    .backgroundColor(backgroundColorDV)
                    .build())
            .measure()
            .layout()
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    val hostView = lithoView.getChildAt(0)
    assertThat(hostView.background).isInstanceOf(ColorDrawable::class.java)
    assertThat((hostView.background as ColorDrawable).color).isEqualTo(startValue)
    backgroundColorDV.set(Color.BLUE)
    assertThat((hostView.background as ColorDrawable).color).isEqualTo(Color.BLUE)
    backgroundColorDV.set(Color.GRAY)
    assertThat((hostView.background as ColorDrawable).color).isEqualTo(Color.GRAY)
    backgroundColorDV.set(Color.TRANSPARENT)
    assertThat((hostView.background as ColorDrawable).color).isEqualTo(Color.TRANSPARENT)
  }

  @Test
  fun testDynamicRotationApplied() {
    val startValue = 0f
    val rotationDV = DynamicValue(startValue)
    val lithoView =
        legacyLithoViewRule
            .attachToWindow()
            .setRoot(Column.create(context).widthPx(80).heightPx(80).rotation(rotationDV).build())
            .measure()
            .layout()
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    val hostView = lithoView.getChildAt(0)
    assertThat(hostView.rotation).isEqualTo(startValue)
    rotationDV.set(364f)
    assertThat(hostView.rotation).isEqualTo(364f)
    rotationDV.set(520f)
    assertThat(hostView.rotation).isEqualTo(520f)
    rotationDV.set(-1f)
    assertThat(hostView.rotation).isEqualTo(-1f)
  }

  @Test
  fun testNullDynamicValue() {
    val nullIntegerValue: DynamicValue<Int>? = null
    val nullFloatValue: DynamicValue<Float>? = null
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            Column.create(context)
                .widthPx(80)
                .heightPx(80)
                .backgroundColor(nullIntegerValue)
                .rotation(nullFloatValue)
                .build())
    assertThat(lithoView.background).isEqualTo(null)
    assertThat(lithoView.rotation).isEqualTo(0.0f)
  }

  private class DynamicElevationBuilder(
      c: ComponentContext,
      defStyleAttr: Int,
      defStyleRes: Int,
      private var _component: SpecGeneratedComponent
  ) : Component.Builder<DynamicElevationBuilder?>(c, defStyleAttr, defStyleRes, _component) {
    override fun build(): SpecGeneratedComponent = _component

    override fun getThis(): DynamicElevationBuilder = this

    override fun setComponent(component: Component) {
      this._component = component as SpecGeneratedComponent
    }
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  fun testDynamicElevationApplied() {
    val lithoView = LithoView(context)
    val startValue = 1f
    val elevationDV = DynamicValue(startValue)
    val component =
        DynamicElevationBuilder(
                context,
                -1,
                -1,
                object : SpecGeneratedComponent("DynamicElevationTestComponent") {
                  override fun getMountType(): MountType = MountType.VIEW
                })
            .shadowElevation(elevationDV)
            .build()
    val dynamicPropsManager = DynamicPropsManager()
    dynamicPropsManager.onBindComponentToContent(
        component, context, component.commonDynamicProps, lithoView)
    assertThat(lithoView.elevation).isEqualTo(startValue)
    elevationDV.set(50f)
    assertThat(lithoView.elevation).isEqualTo(50f)
    elevationDV.set(-50f)
    assertThat(lithoView.elevation).isEqualTo(-50f)
  }

  @Test
  fun commonDynamicProps_unbindAndRebindContent_resetValues() {
    val stateUpdateCaller = DynamicPropsResetValueTesterSpec.Caller()
    val component = DynamicPropsResetValueTester.create(context).caller(stateUpdateCaller).build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    val mountDelegateTarget = legacyLithoViewRule.lithoView.mountDelegateTarget
    var text1HostId: Long = -1
    var text2HostId: Long = -1
    for (i in 0 until mountDelegateTarget.mountItemCount) {
      val mountItem = mountDelegateTarget.getMountItemAt(i)
      if (mountItem != null) {
        val unit = LithoRenderUnit.getRenderUnit(mountItem)
        if (unit.component.simpleName == "Text") {
          val hostMarker = if (i != 0) mountItem.renderTreeNode.parent?.renderUnit?.id ?: -1 else -1
          if (text1HostId == -1L) {
            text1HostId = hostMarker
          } else if (text2HostId == -1L) {
            text2HostId = hostMarker
          }
        }
      }
    }
    lateinit var text1HostComponent: HostComponent
    lateinit var text2HostComponent: HostComponent
    lateinit var text1Host: ComponentHost
    lateinit var text2Host: ComponentHost
    for (i in 0 until mountDelegateTarget.mountItemCount) {
      val mountItem = mountDelegateTarget.getMountItemAt(i)
      if (mountItem != null) {
        val unit = LithoRenderUnit.getRenderUnit(mountItem)
        if (text1HostId == MountItem.getId(mountItem)) {
          text1HostComponent = unit.component as HostComponent
          text1Host = mountItem.content as ComponentHost
        }
        if (text2HostId == MountItem.getId(mountItem)) {
          text2HostComponent = unit.component as HostComponent
          text2Host = mountItem.content as ComponentHost
        }
      }
    }
    assertThat(text1HostComponent.hasCommonDynamicProps()).isTrue
    assertThat(text1Host.alpha).isEqualTo(DynamicPropsResetValueTesterSpec.ALPHA_TRANSPARENT)
    assertThat(text2HostComponent.hasCommonDynamicProps()).isFalse
    assertThat(text2Host.alpha).isEqualTo(DynamicPropsResetValueTesterSpec.ALPHA_OPAQUE)
    stateUpdateCaller.toggleShowChild()
    lateinit var stateUpdateText1HostComponent: HostComponent
    lateinit var stateUpdateText2HostComponent: HostComponent
    lateinit var stateUpdateText1Host: ComponentHost
    lateinit var stateUpdateText2Host: ComponentHost
    for (i in 0 until mountDelegateTarget.mountItemCount) {
      val mountItem = mountDelegateTarget.getMountItemAt(i)
      if (mountItem != null) {
        val unit = LithoRenderUnit.getRenderUnit(mountItem)
        if (text1HostId == MountItem.getId(mountItem)) {
          stateUpdateText1HostComponent = unit.component as HostComponent
          stateUpdateText1Host = mountItem.content as ComponentHost
        }
        if (text2HostId == MountItem.getId(mountItem)) {
          stateUpdateText2HostComponent = unit.component as HostComponent
          stateUpdateText2Host = mountItem.content as ComponentHost
        }
      }
    }
    assertThat(stateUpdateText1Host).isEqualTo(text1Host)
    assertThat(stateUpdateText2Host).isEqualTo(text2Host)
    assertThat(stateUpdateText1HostComponent.hasCommonDynamicProps()).isFalse
    assertThat(stateUpdateText1Host.alpha).isEqualTo(DynamicPropsResetValueTesterSpec.ALPHA_OPAQUE)
    assertThat(stateUpdateText2HostComponent.hasCommonDynamicProps()).isFalse
    assertThat(stateUpdateText2Host.alpha).isEqualTo(DynamicPropsResetValueTesterSpec.ALPHA_OPAQUE)
  }

  @Test
  fun testDynamicPropsAddedToSpecGeneratedComponentUsingWrapper() {
    val alphaDV = DynamicValue(0.5f)
    val lithoView =
        legacyLithoViewRule
            .attachToWindow()
            .setRoot(
                Wrapper.create(context)
                    .delegate(Row.create(context).widthPx(80).heightPx(80).build())
                    .kotlinStyle(Style.alpha(alphaDV))
                    .build())
            .measure()
            .layout()
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    val hostView = lithoView.getChildAt(0)
    assertThat(hostView.alpha).isEqualTo(0.5f)
    alphaDV.set(1f)
    assertThat(hostView.alpha).isEqualTo(1f)
  }

  @Test
  fun testDynamicPropsAddedToPrimitiveComponentUsingWrapper() {
    val alphaDV = DynamicValue(0.5f)
    val lithoView =
        legacyLithoViewRule
            .attachToWindow()
            .setRoot(
                Wrapper.create(context)
                    .delegate(
                        SimpleTestPrimitiveComponent(style = Style.width(80.px).height(80.px)))
                    .kotlinStyle(Style.alpha(alphaDV))
                    .build())
            .measure()
            .layout()
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    val hostView = lithoView.getChildAt(0)
    assertThat(hostView.alpha).isEqualTo(0.5f)
    alphaDV.set(1f)
    assertThat(hostView.alpha).isEqualTo(1f)
  }
}

private class SimpleTestPrimitiveComponent(
    private val style: Style? = null,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ExactSizeConstraintsLayoutBehavior,
        mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
        style)
  }
}
