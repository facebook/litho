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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.layoutstate.withoutdrawableoutput.RootComponent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LayoutStateCalculationWithoutDrawableOutputsTest {

  private lateinit var context: ComponentContext
  private lateinit var componentTree: ComponentTree
  private lateinit var lithoView: LithoView

  @Before
  fun before() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    lithoView = LithoView(context)
    componentTree = ComponentTree.create(context).build()
    lithoView.componentTree = componentTree
  }

  @Test
  fun whenDrawableOutputsEnabledAndChildrenNotWrappedInView_shouldHaveExplicitDrawableOutputsForBackgroundAndForeground() {
    var output: LithoRenderUnit? = null
    componentTree =
        ComponentTree.create(context)
            .componentsConfiguration(
                ComponentsConfiguration.create().shouldAddHostViewForRootComponent(false).build())
            .build()
    lithoView!!.componentTree = componentTree
    attach()
    componentTree.setRootSync(RootComponent.create(context).shouldWrapInView(false).build())
    val state: LayoutState =
        requireNotNull(lithoView.componentTree?.mainThreadLayoutState) { "empty layout state" }
    assertThat(state?.mountableOutputCount).isEqualTo(7)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(0)) // root host view
    assertThat(output?.component).isOfAnyClassIn(HostComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(1)) // background 1
    assertThat(output?.component).isOfAnyClassIn(DrawableComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(2)) // text 1
    assertThat(output?.component).isOfAnyClassIn(Text::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(3)) // foreground 1
    assertThat(output?.component).isOfAnyClassIn(DrawableComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(4)) // background 2
    assertThat(output?.component).isOfAnyClassIn(DrawableComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(5)) // text 2
    assertThat(output?.component).isOfAnyClassIn(Text::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(6)) // foreground 2
    assertThat(output?.component).isOfAnyClassIn(DrawableComponent::class.java)
  }

  @Test
  fun whenDrawableOutputsEnabledAndChildrenWrappedInView_shouldHaveExplicitDrawableOutputsForBackgroundAndForeground() {
    var output: LithoRenderUnit? = null
    componentTree =
        ComponentTree.create(context)
            .componentsConfiguration(
                ComponentsConfiguration.create().shouldAddHostViewForRootComponent(false).build())
            .build()
    lithoView!!.componentTree = componentTree
    componentTree.setRootSync(RootComponent.create(context).shouldWrapInView(true).build())
    attach()
    val state: LayoutState =
        requireNotNull(lithoView.componentTree?.mainThreadLayoutState) { "empty layout state" }
    assertThat(state.mountableOutputCount).isEqualTo(9)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(0)) // root host view
    assertThat(output?.component).isOfAnyClassIn(HostComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(1)) // host view 1
    assertThat(output?.component).isOfAnyClassIn(HostComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(2)) // background 1
    assertThat(output?.component).isOfAnyClassIn(DrawableComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(3)) // text 1
    assertThat(output?.component).isOfAnyClassIn(Text::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(4)) // foreground 1
    assertThat(output?.component).isOfAnyClassIn(DrawableComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(5)) // host view 2
    assertThat(output?.component).isOfAnyClassIn(HostComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(6)) // background 2
    assertThat(output?.component).isOfAnyClassIn(DrawableComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(7)) // text 2
    assertThat(output?.component).isOfAnyClassIn(Text::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(8)) // foreground 2
    assertThat(output?.component).isOfAnyClassIn(DrawableComponent::class.java)
  }

  @Test
  fun whenDrawableOutputsDisabledAndChildrenNotWrappedInView_shouldNotHaveDrawableOutputsForBackgroundAndForeground() {
    var output: LithoRenderUnit? = null
    // disable layout outputs for drawables
    componentTree =
        ComponentTree.create(context)
            .componentsConfiguration(
                ComponentsConfiguration.create().shouldAddHostViewForRootComponent(true).build())
            .build()
    lithoView!!.componentTree = componentTree
    componentTree.setRootSync(RootComponent.create(context).shouldWrapInView(false).build())
    attach()
    val state: LayoutState =
        requireNotNull(lithoView.componentTree?.mainThreadLayoutState) { "empty layout state" }
    assertThat(state.mountableOutputCount).isEqualTo(5) // 2 bg and fg lesser.
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(1))
    assertThat(output?.component).isOfAnyClassIn(HostComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(2))
    assertThat(output?.component).isOfAnyClassIn(Text::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(3))
    assertThat(output?.component).isOfAnyClassIn(HostComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(4))
    assertThat(output?.component).isOfAnyClassIn(Text::class.java)
  }

  @Test
  fun whenDrawableOutputsDisabledAndChildrenWrappedInView_shouldNotHaveDrawableOutputsForBackgroundAndForeground() {
    var output: LithoRenderUnit? = null
    // disable layout outputs for drawables
    componentTree =
        ComponentTree.create(context)
            .componentsConfiguration(
                ComponentsConfiguration.create().shouldAddHostViewForRootComponent(true).build())
            .build()
    lithoView!!.componentTree = componentTree
    componentTree.setRootSync(RootComponent.create(context).shouldWrapInView(true).build())
    attach()
    val state: LayoutState =
        requireNotNull(lithoView.componentTree?.mainThreadLayoutState) { "empty layout state" }
    assertThat(state.mountableOutputCount).isEqualTo(5) // 2 bg and fg lesser.
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(1))
    assertThat(output?.component).isOfAnyClassIn(HostComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(2))
    assertThat(output?.component).isOfAnyClassIn(Text::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(3))
    assertThat(output?.component).isOfAnyClassIn(HostComponent::class.java)
    output = LithoRenderUnit.getRenderUnit(state.getMountableOutputAt(4))
    assertThat(output?.component).isOfAnyClassIn(Text::class.java)
  }

  private fun attach() {
    lithoView.onAttachedToWindow()
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(1_080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1_920, View.MeasureSpec.UNSPECIFIED))
    lithoView.layout(0, 0, 1_080, 1_920)
    lithoView.notifyVisibleBoundsChanged()
  }
}
