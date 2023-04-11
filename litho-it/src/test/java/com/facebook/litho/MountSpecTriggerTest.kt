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

import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.SingleComponentSection
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecTriggerTester
import com.facebook.litho.widget.Text
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class MountSpecTriggerTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @Test
  fun mountSpec_setRootAndTriggerEvent_eventIsTriggered() {
    val componentContext = legacyLithoViewRule.context
    val triggerObjectRef = AtomicReference<Any>()
    val triggerHandle = Handle()
    val info: List<StepInfo> = ArrayList<StepInfo>()
    val component =
        MountSpecTriggerTester.create(componentContext)
            .steps(info)
            .triggerObjectRef(triggerObjectRef)
            .handle(triggerHandle)
            .build()
    legacyLithoViewRule.setRoot(component).idle()
    legacyLithoViewRule.attachToWindow().measure().layout()
    val bazObject = Any()
    MountSpecTriggerTester.triggerTestEvent(
        legacyLithoViewRule.componentTree.context, triggerHandle, bazObject)
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call @OnTrigger method")
        .containsExactly(LifecycleStep.ON_TRIGGER)
    assertThat(triggerObjectRef.get())
        .describedAs("Event object is correctly passed")
        .isEqualTo(bazObject)
  }

  @Test
  fun mountSpec_setRootAndTriggerEvent_eventIsTriggered_handle_used_in_child() {
    val componentContext = legacyLithoViewRule.context
    val triggerObjectRef = AtomicReference<Any>()
    val triggerHandle = Handle()
    val info: List<StepInfo> = ArrayList<StepInfo>()
    val component =
        Column.create(componentContext)
            .child(Text.create(componentContext).text("Sample"))
            .child(
                MountSpecTriggerTester.create(componentContext)
                    .steps(info)
                    .triggerObjectRef(triggerObjectRef)
                    .handle(triggerHandle)
                    .build())
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val bazObject = Any()
    MountSpecTriggerTester.triggerTestEvent(
        legacyLithoViewRule.componentTree.context, triggerHandle, bazObject)
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call @OnTrigger method")
        .containsExactly(LifecycleStep.ON_TRIGGER)
    assertThat(triggerObjectRef.get())
        .describedAs("Event object is correctly passed")
        .isEqualTo(bazObject)
  }

  @Test
  fun mountSpec_setRootAndTriggerEvent_eventIsTriggered_handle_used_in_nested_tree_root() {
    val componentContext = legacyLithoViewRule.context
    val triggerObjectRef = AtomicReference<Any>()
    val triggerHandle = Handle()
    val info: List<StepInfo> = ArrayList<StepInfo>()
    val component =
        Column.create(componentContext)
            .child(Text.create(componentContext).text("Sample"))
            .child(
                RecyclerCollectionComponent.create(componentContext)
                    .heightPx(150)
                    .section(
                        SingleComponentSection.create(SectionContext(componentContext))
                            .component(
                                MountSpecTriggerTester.create(componentContext)
                                    .steps(info)
                                    .triggerObjectRef(triggerObjectRef)
                                    .handle(triggerHandle)
                                    .build())))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val bazObject = Any()
    MountSpecTriggerTester.triggerTestEvent(
        legacyLithoViewRule.componentTree.context, triggerHandle, bazObject)
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call @OnTrigger method")
        .containsExactly(LifecycleStep.ON_TRIGGER)
    assertThat(triggerObjectRef.get())
        .describedAs("Event object is correctly passed")
        .isEqualTo(bazObject)
  }

  @Test
  fun mountSpec_setRootAndTriggerEvent_eventIsTriggered_handle_used_in_nested_tree_deeper_in_hierarchy() {
    val componentContext = legacyLithoViewRule.context
    val triggerObjectRef = AtomicReference<Any>()
    val triggerHandle = Handle()
    val info: List<StepInfo> = ArrayList<StepInfo>()
    val component =
        Column.create(componentContext)
            .child(Text.create(componentContext).text("Sample"))
            .child(
                RecyclerCollectionComponent.create(componentContext)
                    .heightPx(150)
                    .section(
                        SingleComponentSection.create(SectionContext(componentContext))
                            .component(
                                Column.create(componentContext)
                                    .child(Text.create(componentContext).text("Nested tree child"))
                                    .child(
                                        MountSpecTriggerTester.create(componentContext)
                                            .steps(info)
                                            .triggerObjectRef(triggerObjectRef)
                                            .handle(triggerHandle)
                                            .build()))))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val bazObject = Any()
    MountSpecTriggerTester.triggerTestEvent(
        legacyLithoViewRule.componentTree.context, triggerHandle, bazObject)
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call @OnTrigger method")
        .containsExactly(LifecycleStep.ON_TRIGGER)
    assertThat(triggerObjectRef.get())
        .describedAs("Event object is correctly passed")
        .isEqualTo(bazObject)
  }
}
