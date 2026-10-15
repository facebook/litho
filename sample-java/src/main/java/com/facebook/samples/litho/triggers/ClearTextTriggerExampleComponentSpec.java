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
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TextInput;

@LayoutSpec
public class ClearTextTriggerExampleComponentSpec {

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, final StateValue<Handle> textInputHandle) {
    textInputHandle.set(new Handle());
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State Handle textInputHandle) {
    return Column.create(c)
        .child(
            Text.create(c, android.R.attr.buttonStyle, 0)
                .text("Clear")
                .clickHandler(ClearTextTriggerExampleComponent.onClearClick(c, textInputHandle)))
        .child(TextInputContainerComponent.create(c).textInputHandle(textInputHandle))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClearClick(ComponentContext c, @Param Handle textInputHandle) {
    // Clear the TextInput inside TextInputContainerComponent
    TextInput.setText(c, textInputHandle, "");
  }
}
