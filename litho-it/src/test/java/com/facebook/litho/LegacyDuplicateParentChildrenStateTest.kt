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
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import java.lang.Exception
import junit.framework.Assert
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LegacyDuplicateParentChildrenStateTest {
  @JvmField @Rule val lithoViewRule: LithoViewRule = LithoViewRule()

  @Before
  @Throws(Exception::class)
  fun setUp() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false)
  }

  @Test
  fun duplicateParentState_avoidedIfRedundant() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .duplicateParentState(true)
                .clickHandler(c.newEventHandler(1) as EventHandler<ClickEvent>)
                .child(
                    Column.create(c)
                        .duplicateParentState(false)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    Column.create(c)
                        .duplicateParentState(true)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    Column.create(c)
                        .clickHandler(c.newEventHandler(2) as EventHandler<ClickEvent>)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(true)))
                .child(
                    Column.create(c)
                        .clickHandler(c.newEventHandler(3) as EventHandler<ClickEvent>)
                        .child(SimpleMountSpecTester.create(c).duplicateParentState(false)))
                .child(
                    Column.create(c)
                        .clickHandler(c.newEventHandler(3) as EventHandler<ClickEvent>)
                        .backgroundColor(Color.RED)
                        .foregroundColor(Color.RED))
                .child(Column.create(c).backgroundColor(Color.BLUE).foregroundColor(Color.BLUE))
                .build()
          }
        }
    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 100) { component }
    val layoutState = testLithoView.componentTree.mainThreadLayoutState!!
    Assertions.assertThat(layoutState.mountableOutputCount).isEqualTo(12)
    Assert.assertTrue(
        "Clickable root output has duplicate state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).flags))
    Assert.assertFalse(
        "Parent doesn't duplicate host state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).flags))
    Assert.assertTrue(
        "Parent does duplicate host state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(2)).flags))
    Assert.assertTrue(
        "Drawable duplicates clickable parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(4)).flags))
    Assert.assertFalse(
        "Drawable doesn't duplicate clickable parent state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(6)).flags))
    Assert.assertTrue(
        "Background should duplicate clickable node state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(8)).flags))
    Assert.assertTrue(
        "Foreground should duplicate clickable node state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(9)).flags))
    Assert.assertFalse(
        "Background should duplicate non-clickable node state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(10)).flags))
    Assert.assertFalse(
        "Foreground should duplicate non-clickable node state",
        LithoRenderUnit.isDuplicateParentState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(11)).flags))
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }
}
