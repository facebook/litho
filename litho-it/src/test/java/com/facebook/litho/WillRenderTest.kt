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
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ComponentWithState
import com.facebook.litho.widget.LayoutSpecWillRenderReuseTester
import com.facebook.litho.widget.LayoutSpecWillRenderTester
import java.lang.IllegalArgumentException
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class WillRenderTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()
  @JvmField @Rule val expectedException = ExpectedException.none()

  private val nonNullSpec: InlineLayoutSpec =
      object : InlineLayoutSpec() {
        override fun onCreateLayout(c: ComponentContext): Component = Row.create(c).build()
      }

  private val layoutWithSizeSpec: InlineLayoutSpec =
      object : InlineLayoutSpec() {
        override fun onCreateLayoutWithSizeSpec(
            c: ComponentContext,
            widthSpec: Int,
            heightSpec: Int
        ): Component =
            Row.create(c)
                .widthDip(View.MeasureSpec.getSize(widthSpec).toFloat())
                .heightDip(View.MeasureSpec.getSize(heightSpec).toFloat())
                .build()

        override fun canMeasure(): Boolean = true
      }

  @Test
  fun testWillRenderForComponentThatReturnsNull() {
    val c = legacyLithoViewRule.context
    legacyLithoViewRule
        .attachToWindow()
        .setRoot(Wrapper.create(c).delegate(null))
        .layout()
        .measure()
    assertThat(legacyLithoViewRule.committedLayoutState?.layoutRoot).isNull()
  }

  @Test
  fun testWillRenderForComponentThatReturnsNonNull() {
    val c = legacyLithoViewRule.context
    c.setRenderStateContextForTests()
    legacyLithoViewRule.setRoot(Wrapper.create(c).delegate(nonNullSpec).build())
    LithoAssertions.assertThat(legacyLithoViewRule.lithoView).willRenderContent()
  }

  @Test
  fun testWillRenderForComponentWithSizeSpecThrowsException() {
    expectedException.expect(IllegalArgumentException::class.java)
    expectedException.expectMessage("@OnCreateLayoutWithSizeSpec")
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    c.setRenderStateContextForTests()
    Component.willRender(c, Wrapper.create(c).delegate(layoutWithSizeSpec).build())
  }

  @Test
  fun testWillRender_withComponentContextWithoutStateHandler_doesntCrash() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    c.setRenderStateContextForTests()
    assertThat(Component.willRender(c, ComponentWithState.create(c).build())).isTrue
  }

  @Test
  fun testWillRender_cachedLayoutUsedInDifferentComponentHierarchy() {
    val info: List<StepInfo> = ArrayList<StepInfo>()
    val component =
        LayoutSpecWillRenderTester.create(legacyLithoViewRule.context).steps(info).build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED)
  }

  @Test
  fun testWillRender_cachedLayoutUsedInSameComponentHierarchy() {
    val info: List<StepInfo> = ArrayList<StepInfo>()
    val component =
        LayoutSpecWillRenderReuseTester.create(legacyLithoViewRule.context).steps(info).build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED)
  }
}
