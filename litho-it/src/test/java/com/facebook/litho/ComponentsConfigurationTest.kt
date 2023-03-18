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
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.SingleComponentSection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentsConfigurationTest {

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()

  private val componentContext =
      ComponentContext(ApplicationProvider.getApplicationContext<Context>())

  private val defaultBuilder = ComponentsConfiguration.getDefaultComponentsConfigurationBuilder()

  @Test
  fun testSetFlagThroughComponentConfigToComponentTree() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().useCancelableLayoutFutures(true))
    val componentTree =
        ComponentTree.create(componentContext)
            .componentsConfiguration(ComponentsConfiguration.getDefaultComponentsConfiguration())
            .build()
    val componentsConfiguration = componentTree.context.mLithoConfiguration.mComponentsConfiguration

    assertThat(componentsConfiguration.useCancelableLayoutFutures).isTrue
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(defaultBuilder)
  }

  @Test
  fun testSetFlagThroughComponentConfigToComponentTreeWithRecyclerCollectionComponent() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().useCancelableLayoutFutures(true))
    val recyclerBinderConfiguration =
        RecyclerBinderConfiguration.create()
            .componentsConfiguration(ComponentsConfiguration.getDefaultComponentsConfiguration())
            .build()
    legacyLithoViewRule
        .setRoot(
            RecyclerCollectionComponent.create(componentContext)
                .recyclerConfiguration(
                    ListRecyclerConfiguration.create()
                        .recyclerBinderConfiguration(recyclerBinderConfiguration)
                        .build())
                .section(
                    SingleComponentSection.create(SectionContext(componentContext))
                        .component(
                            Row.create(componentContext)
                                .viewTag("rv_row")
                                .heightDip(100f)
                                .widthDip(100f))
                        .build())
                .build())
        .setSizeSpecs(makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(5, SizeSpec.EXACTLY))
    legacyLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10)
    val childView = legacyLithoViewRule.lithoView.findViewWithTag("rv_row") as LithoView?
    assertThat(childView).isNotNull
    val componentsConfiguration =
        childView?.componentTree?.context?.mLithoConfiguration?.mComponentsConfiguration
    assertThat(componentsConfiguration?.useCancelableLayoutFutures).isTrue
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(defaultBuilder)
  }

  @Test
  fun testOverrideDefaultBuilder() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().useCancelableLayoutFutures(true))
    assertThat(
            ComponentsConfiguration.getDefaultComponentsConfiguration().useCancelableLayoutFutures)
        .isTrue
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().useCancelableLayoutFutures(false))
    assertThat(
            ComponentsConfiguration.getDefaultComponentsConfiguration().useCancelableLayoutFutures)
        .isFalse
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(defaultBuilder)
  }
}
