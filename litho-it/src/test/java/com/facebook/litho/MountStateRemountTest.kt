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
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.drawable.ComparableDrawable
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.EditText
import com.facebook.litho.widget.Text
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class MountStateRemountTest {

  private val config =
      ComponentsConfiguration.create().shouldAddHostViewForRootComponent(true).build()
  private lateinit var context: ComponentContext

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule(config)

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testMountItemsHaveMountData() {
    val component1 = TestDrawableComponent.create(context).build()
    val component2 = TestDrawableComponent.create(context).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(component1).child(component2).build())
    assertThat(component1.isMounted).isTrue
    assertThat(component2.isMounted).isTrue
  }

  @Test
  fun testRemountSameLayoutState() {
    val component1 = TestDrawableComponent.create(context).build()
    val component2 = TestDrawableComponent.create(context).build()
    val component3 = TestDrawableComponent.create(context).build()
    val component4 = TestDrawableComponent.create(context).build()
    legacyLithoViewRule
        .setRoot(
            Column.create(context)
                .widthPx(100)
                .heightPx(100)
                .child(component1)
                .child(component2)
                .build())
        .measure()
        .layout()
        .attachToWindow()
    assertThat(component1.isMounted).isTrue
    assertThat(component2.isMounted).isTrue
    legacyLithoViewRule.setRoot(
        Column.create(context)
            .widthPx(100)
            .heightPx(100)
            .child(component3)
            .child(component4)
            .build())
    assertThat(component1.isMounted).isTrue
    assertThat(component2.isMounted).isTrue
    assertThat(component3.isMounted).isFalse
    assertThat(component4.isMounted).isFalse
    val mountDelegateTarget = legacyLithoViewRule.lithoView.mountDelegateTarget
    val components: MutableList<Component> = ArrayList()
    for (i in 0 until mountDelegateTarget.mountItemCount) {
      mountDelegateTarget.getMountItemAt(i)?.let { mountItem ->
        components.add(LithoRenderUnit.getRenderUnit(mountItem).component)
      }
    }
    assertThat(containsRef(components, component1)).isFalse
    assertThat(containsRef(components, component2)).isFalse
    assertThat(containsRef(components, component3)).isTrue
    assertThat(containsRef(components, component4)).isTrue
  }

  /**
   * There was a crash when mounting a drawing in place of a view. This test is here to make sure
   * this does not regress. To reproduce this crash the pools needed to be in a specific state as
   * view layout outputs and mount items were being re-used for drawables.
   */
  @Test
  fun testRemountDifferentMountType() {
    legacyLithoViewRule
        .setRoot(TestViewComponent.create(context).build())
        .setSizeSpecs(exactly(10), exactly(5))
    legacyLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10)
    assertThat(legacyLithoViewRule.lithoView.childCount).isEqualTo(1)
    legacyLithoViewRule
        .setRoot(TestDrawableComponent.create(context).build())
        .setSizeSpecs(exactly(10), exactly(5))
    assertThat(legacyLithoViewRule.lithoView.drawables[0]).isNotNull
  }

  @Test
  fun testRemountNewLayoutState() {
    val component1 = TestDrawableComponent.create(context).color(Color.RED).build()
    val component2 = TestDrawableComponent.create(context).color(Color.BLUE).build()
    val component3 = TestDrawableComponent.create(context).color(Color.GREEN).build()
    val component4 = TestDrawableComponent.create(context).color(Color.YELLOW).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(component1).child(component2).build())
    assertThat(component1.isMounted).isTrue
    assertThat(component2.isMounted).isTrue
    ComponentTestHelper.mountComponent(
        context, lithoView, Column.create(context).child(component3).child(component4).build())
    assertThat(component1.isMounted).isFalse
    assertThat(component2.isMounted).isFalse
    assertThat(component3.isMounted).isTrue
    assertThat(component4.isMounted).isTrue
  }

  @Test
  fun testRemountAfterSettingNewRootTwice() {
    val component1 =
        TestDrawableComponent.create(context).color(Color.RED).returnSelfInMakeShallowCopy().build()
    val component2 =
        TestDrawableComponent.create(context)
            .returnSelfInMakeShallowCopy()
            .color(Color.BLUE)
            .build()
    val lithoView = LithoView(context)
    val componentTree =
        ComponentTree.create(context, Column.create(context).child(component1).build()).build()
    ComponentTestHelper.mountComponent(lithoView, componentTree, exactly(100), exactly(100))
    assertThat(component1.isMounted).isTrue
    componentTree.setRootAndSizeSpecSync(
        Column.create(context).child(component2).build(), exactly(50), exactly(50))
    componentTree.setSizeSpec(exactly(100), exactly(100))
    assertThat(component2.isMounted).isTrue
  }

  @Test
  fun testRemountPartiallyDifferentLayoutState() {
    val component1 = TestDrawableComponent.create(context).build()
    val component2 = TestDrawableComponent.create(context).build()
    val component3 = TestDrawableComponent.create(context).build()
    val component4 = TestDrawableComponent.create(context).build()
    legacyLithoViewRule
        .setRoot(
            Column.create(context)
                .widthPx(100)
                .heightPx(100)
                .child(component1)
                .child(component2)
                .build())
        .measure()
        .layout()
        .attachToWindow()
    assertThat(component1.isMounted).isTrue
    assertThat(component2.isMounted).isTrue
    legacyLithoViewRule.setRoot(
        Column.create(context)
            .widthPx(100)
            .heightPx(100)
            .child(component3)
            .child(Column.create(context).wrapInView().child(component4))
            .build())
    assertThat(component1.isMounted).isTrue
    assertThat(component2.isMounted).isFalse
    assertThat(component3.isMounted).isFalse
    assertThat(component4.isMounted).isTrue
  }

  @Test
  fun testRemountOnNoLayoutChanges() {
    val oldComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(
                EditText.create(context)
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.CYAN)
                    .text("Hello World")
                    .viewTag("Alpha")
                    .contentDescription("some description"))
            .build()
    val lithoView = LithoView(context)
    val componentTree =
        ComponentTree.create(context, oldComponent)
            .componentsConfiguration(config)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build()
    ComponentTestHelper.mountComponent(lithoView, componentTree, exactly(400), exactly(400))
    val oldHost = lithoView.getChildAt(0) as ViewGroup
    val oldView = oldHost.getChildAt(0)
    val oldTag = oldView.tag
    val oldContentDescription = oldView.contentDescription.toString()
    val oldBackground = oldView.background
    val newComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(
                EditText.create(context)
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.CYAN)
                    .text("Hello World")
                    .viewTag("Alpha")
                    .contentDescription("some description"))
            .build()
    componentTree.setRootAndSizeSpecSync(newComponent, exactly(400), exactly(400))
    componentTree.setSizeSpec(exactly(400), exactly(400))
    val newHost = lithoView.getChildAt(0) as ViewGroup
    val newView = newHost.getChildAt(0)
    assertThat(newView).isSameAs(oldView)
    val newTag = newView.tag
    val newContentDescription = newView.contentDescription.toString()
    val newBackground = newView.background

    // Check that props were not set again
    assertThat(newTag).isSameAs(oldTag)
    assertThat(newContentDescription).isSameAs(oldContentDescription)
    assertThat(oldBackground).isSameAs(newBackground)
  }

  @Test
  fun testRemountOnNodeInfoLayoutChanges() {
    val oldComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(Text.create(context).textSizeSp(12f).text("label:"))
            .child(
                EditText.create(context)
                    .text("Hello World")
                    .textSizeSp(12f)
                    .viewTag("Alpha")
                    .enabled(true))
            .build()
    val lithoView = LithoView(context)
    val componentTree =
        ComponentTree.create(context, oldComponent)
            .componentsConfiguration(config)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build()
    ComponentTestHelper.mountComponent(lithoView, componentTree, exactly(400), exactly(400))
    val oldHost = lithoView.getChildAt(0) as ViewGroup
    val oldView = oldHost.getChildAt(0)
    val oldTag = oldView.tag
    val oldIsEnabled = oldView.isEnabled
    val newComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(Text.create(context).textSizeSp(12f).text("label:"))
            .child(
                EditText.create(context)
                    .text("Hello World")
                    .textSizeSp(12f)
                    .viewTag("Beta")
                    .enabled(false))
            .build()
    componentTree.setRootAndSizeSpecSync(newComponent, exactly(400), exactly(400))
    componentTree.setSizeSpec(exactly(400), exactly(400))
    val newHost = lithoView.getChildAt(0) as ViewGroup
    val newView = newHost.getChildAt(0)
    assertThat(newView).isSameAs(oldView)
    val newTag = newView.tag
    val newIsEnabled = newView.isEnabled
    assertThat(newTag).isNotEqualTo(oldTag)
    assertThat(newIsEnabled).isNotEqualTo(oldIsEnabled)
  }

  @Test
  fun testRemountOnViewNodeInfoLayoutChanges() {
    val oldComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(Text.create(context).textSizeSp(12f).text("label:"))
            .child(
                EditText.create(context)
                    .text("Hello World")
                    .textSizeSp(12f)
                    .backgroundColor(Color.RED))
            .build()
    val lithoView = LithoView(context)
    val componentTree =
        ComponentTree.create(context, oldComponent)
            .componentsConfiguration(config)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build()
    ComponentTestHelper.mountComponent(lithoView, componentTree, exactly(400), exactly(400))
    val oldHost = lithoView.getChildAt(0) as ViewGroup
    val oldView = oldHost.getChildAt(0)
    val oldDrawable = oldView.background as ComparableDrawable
    val newComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(Text.create(context).textSizeSp(12f).text("label:"))
            .child(
                EditText.create(context)
                    .text("Hello World")
                    .textSizeSp(12f)
                    .backgroundColor(Color.CYAN))
            .build()
    componentTree.setRootAndSizeSpecSync(newComponent, exactly(400), exactly(400))
    componentTree.setSizeSpec(exactly(400), exactly(400))
    val newHost = lithoView.getChildAt(0) as ViewGroup
    val newView = newHost.getChildAt(0)
    assertThat(newView).isSameAs(oldView)
    val newDrawable = newView.background as ComparableDrawable
    assertThat(oldDrawable.isEquivalentTo(newDrawable)).isFalse
  }

  private fun containsRef(list: List<*>, obj: Any): Boolean {
    return list.any { o -> o === obj }
  }
}
