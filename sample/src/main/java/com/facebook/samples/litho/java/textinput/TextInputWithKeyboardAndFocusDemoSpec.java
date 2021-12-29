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

package com.facebook.samples.litho.java.textinput;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Handle;
import com.facebook.litho.StateValue;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.TextInput;

/** Demo to show how to request focus and show keyboard when TextInput appears. */
@LayoutSpec
class TextInputWithKeyboardAndFocusDemoSpec {

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Handle> textInputHandle) {
    textInputHandle.set(new Handle());
  }

  @OnEvent(VisibleEvent.class)
  static void onVisibleEvent(final ComponentContext c, @State final Handle textInputHandle) {
    TextInput.requestFocus(c, textInputHandle);
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State Handle textInputHandle) {
    return Column.create(c)
        .child(
            TextInput.create(c)
                .handle(textInputHandle)
                .visibleHandler(TextInputWithKeyboardAndFocusDemo.onVisibleEvent(c)))
        .build();
  }
}
