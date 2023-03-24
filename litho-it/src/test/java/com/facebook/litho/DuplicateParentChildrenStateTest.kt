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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import junit.framework.Assert
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class DuplicateParentChildrenStateTest {
  @JvmField
  @Rule
  val lithoViewRule: LithoViewRule =
      LithoViewRule(
          componentsConfiguration =
              ComponentsConfiguration.create().shouldAddHostViewForRootComponent(true).build())

  @Test
  fun duplicateParentState_avoidedIfRedundant() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c) // (0 = root host)
                .duplicateParentState(true) // (1 = generated host)
                .clickHandler(c.newEventHandler(1) as EventHandler<ClickEvent>)
                .child(
                    Column.create(c)
                        .duplicateParentState(false) // (2 = simpleMountTester)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    Column.create(c)
                        .duplicateParentState(true) // (3 = simpleMountTester)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    Column.create(c) // (4 = generated host)
                        .clickHandler(
                            c.newEventHandler(2)
                                as EventHandler<ClickEvent>) // (5 = simpleMountTester)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    Column.create(c) // (6 = generated host)
                        .clickHandler(
                            c.newEventHandler(3)
                                as EventHandler<ClickEvent>) // (7 = simpleMountTester)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(false)))
                .child(
                    Column.create(c) // (8 = generated host)
                        .clickHandler(c.newEventHandler(3) as EventHandler<ClickEvent>)
                        .backgroundColor(Color.RED)
                        .foregroundColor(Color.RED))
                .child(
                    Column.create(c)
                        .backgroundColor(Color.BLUE)
                        .foregroundColor(Color.BLUE)) // (9 = generated host)
                .build()
          }
        }

    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 100) { component }
    val layoutState = testLithoView.componentTree.mainThreadLayoutState!!

    Assertions.assertThat(layoutState.mountableOutputCount).isEqualTo(10)
    Assert.assertFalse(
        "Root output doesn't have duplicate state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).flags))
    Assert.assertTrue(
        "Clickable generated root host output has duplicate state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).flags))
    Assert.assertFalse(
        "Drawable doesn't duplicate host state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(2)).flags))
    Assert.assertTrue(
        "Drawable does duplicate parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(3)).flags))
    Assert.assertFalse(
        "Drawable host doesn't duplicate clickable parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(4)).flags))
    Assert.assertTrue(
        "Drawable duplicates parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(5)).flags))
    Assert.assertFalse(
        "Drawable host doesn't duplicate clickable parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(6)).flags))
    Assert.assertFalse(
        "Drawable doesn't duplicate clickable parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(7)).flags))
    Assert.assertFalse(
        "Clickable host doesn't duplicate parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(8)).flags))
    Assert.assertFalse(
        "Host with bg doesn't duplicate parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(9)).flags))
  }

  @Test
  fun duplicateChildrenStates_passedToView() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Row.create(c)
                .child(
                    Row.create(c)
                        .duplicateChildrenStates(true)
                        .child(SimpleMountSpecTester.create(c).focusable(true))
                        .child(SimpleMountSpecTester.create(c).clickable(true)))
                .build()
          }
        }
    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 100) { component }
    val secondMountedItem =
        testLithoView.lithoView.mountDelegateTarget!!.getMountItemAt(1)!!.content
    Assert.assertTrue(secondMountedItem is ComponentHost)
    Assert.assertTrue((secondMountedItem as ComponentHost).addStatesFromChildren())
  }
}
