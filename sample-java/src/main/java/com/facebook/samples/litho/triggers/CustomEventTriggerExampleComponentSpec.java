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

package com.facebook.samples.litho.java.triggers;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Handle;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class CustomEventTriggerExampleComponentSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      final StateValue<Handle> textInputHandle,
      final StateValue<Handle> handleInNestedTree) {
    textInputHandle.set(new Handle());
    handleInNestedTree.set(new Handle());
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      final @State Handle textInputHandle,
      final @State Handle handleInNestedTree) {
    return Column.create(c)
        .child(
            Text.create(c, android.R.attr.buttonStyle, 0)
                .text("Trigger custom event")
                .clickHandler(CustomEventTriggerExampleComponent.onClick(c, textInputHandle)))
        .child(
            Text.create(c, android.R.attr.buttonStyle, 0)
                .text("Trigger custom event in nested tree")
                .clickHandler(CustomEventTriggerExampleComponent.onClick(c, handleInNestedTree)))
        .child(
            ComponentWithCustomEventTriggerComponent.create(c)
                .titleText("State: ")
                .handle(textInputHandle))
        .child(
            RecyclerCollectionComponent.create(c)
                .heightPx(150)
                .section(
                    SingleComponentSection.create(new SectionContext(c))
                        .component(
                            Column.create(c)
                                .child(
                                    ComponentWithCustomEventTriggerComponent.create(c)
                                        .handle(handleInNestedTree)
                                        .titleText("State in nested tree: ")
                                        .build()))
                        .build()))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @Param Handle handle) {
    ComponentWithCustomEventTriggerComponent.triggerCustomEvent(c, handle, 2);
  }
}
