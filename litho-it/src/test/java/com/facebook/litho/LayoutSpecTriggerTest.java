/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import static com.facebook.litho.LifecycleStep.getSteps;
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.LayoutSpecTriggerTester;
import com.facebook.litho.widget.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutSpecTriggerTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Test
  public void layoutSpec_setRootAndTriggerEvent_eventIsTriggered() {
    final ComponentContext componentContext = mLegacyLithoViewRule.getContext();
    final AtomicReference<Object> triggerObjectRef = new AtomicReference<>();
    final Handle triggerHandle = new Handle();
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        LayoutSpecTriggerTester.create(componentContext)
            .steps(info)
            .triggerObjectRef(triggerObjectRef)
            .handle(triggerHandle)
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    final Object bazObject = new Object();

    // We need to use a ComponentContext with a ComponentTree on it
    LayoutSpecTriggerTester.triggerTestEvent(
        mLegacyLithoViewRule.getComponentTree().getContext(), triggerHandle, bazObject);

    assertThat(getSteps(info))
        .describedAs("Should call @OnTrigger method")
        .containsExactly(LifecycleStep.ON_TRIGGER);

    assertThat(triggerObjectRef.get())
        .describedAs("Event object is correctly passed")
        .isEqualTo(bazObject);
  }

  @Test
  public void layoutSpec_setRootAndTriggerEvent_eventIsTriggered_handle_used_in_child() {
    final ComponentContext componentContext = mLegacyLithoViewRule.getContext();
    final AtomicReference<Object> triggerObjectRef = new AtomicReference<>();
    final Handle triggerHandle = new Handle();
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        Column.create(componentContext)
            .child(Text.create(componentContext).text("Sample"))
            .child(
                LayoutSpecTriggerTester.create(componentContext)
                    .steps(info)
                    .triggerObjectRef(triggerObjectRef)
                    .handle(triggerHandle)
                    .build())
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    final Object bazObject = new Object();

    // We need to use a ComponentContext with a ComponentTree on it
    LayoutSpecTriggerTester.triggerTestEvent(
        mLegacyLithoViewRule.getComponentTree().getContext(), triggerHandle, bazObject);

    assertThat(getSteps(info))
        .describedAs("Should call @OnTrigger method")
        .containsExactly(LifecycleStep.ON_TRIGGER);

    assertThat(triggerObjectRef.get())
        .describedAs("Event object is correctly passed")
        .isEqualTo(bazObject);
  }

  @Test
  public void layoutSpec_setRootAndTriggerEvent_eventIsTriggered_handle_used_in_nested_tree_root() {
    final ComponentContext componentContext = mLegacyLithoViewRule.getContext();
    final AtomicReference<Object> triggerObjectRef = new AtomicReference<>();
    final Handle triggerHandle = new Handle();
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        Column.create(componentContext)
            .child(Text.create(componentContext).text("Sample"))
            .child(
                RecyclerCollectionComponent.create(componentContext)
                    .heightPx(150)
                    .section(
                        SingleComponentSection.create(new SectionContext(componentContext))
                            .component(
                                LayoutSpecTriggerTester.create(componentContext)
                                    .steps(info)
                                    .triggerObjectRef(triggerObjectRef)
                                    .handle(triggerHandle)
                                    .build())))
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    final Object bazObject = new Object();

    // We need to use a ComponentContext with a ComponentTree on it
    LayoutSpecTriggerTester.triggerTestEvent(
        mLegacyLithoViewRule.getComponentTree().getContext(), triggerHandle, bazObject);

    assertThat(getSteps(info))
        .describedAs("Should call @OnTrigger method")
        .containsExactly(LifecycleStep.ON_TRIGGER);

    assertThat(triggerObjectRef.get())
        .describedAs("Event object is correctly passed")
        .isEqualTo(bazObject);
  }

  @Test
  public void
      layoutSpec_setRootAndTriggerEvent_eventIsTriggered_handle_used_in_nested_tree_deeper_in_hierarchy() {
    final ComponentContext componentContext = mLegacyLithoViewRule.getContext();
    final AtomicReference<Object> triggerObjectRef = new AtomicReference<>();
    final Handle triggerHandle = new Handle();
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        Column.create(componentContext)
            .child(Text.create(componentContext).text("Sample"))
            .child(
                RecyclerCollectionComponent.create(componentContext)
                    .heightPx(150)
                    .section(
                        SingleComponentSection.create(new SectionContext(componentContext))
                            .component(
                                Column.create(componentContext)
                                    .child(Text.create(componentContext).text("Nested tree child"))
                                    .child(
                                        LayoutSpecTriggerTester.create(componentContext)
                                            .steps(info)
                                            .triggerObjectRef(triggerObjectRef)
                                            .handle(triggerHandle)
                                            .build()))))
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    final Object bazObject = new Object();

    // We need to use a ComponentContext with a ComponentTree on it
    LayoutSpecTriggerTester.triggerTestEvent(
        mLegacyLithoViewRule.getComponentTree().getContext(), triggerHandle, bazObject);

    assertThat(getSteps(info))
        .describedAs("Should call @OnTrigger method")
        .containsExactly(LifecycleStep.ON_TRIGGER);

    assertThat(triggerObjectRef.get())
        .describedAs("Event object is correctly passed")
        .isEqualTo(bazObject);
  }
}
