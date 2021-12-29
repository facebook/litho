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

import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Handle;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Button;
import com.facebook.litho.widget.TextInput;
import com.facebook.yoga.YogaEdge;

/** Demo to show how to request/clear focus programmatically. */
@LayoutSpec
class TextInputRequestAndClearFocusSpec {

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Handle> textInputHandle) {
    textInputHandle.set(new Handle());
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State Handle textInputHandle) {
    return Column.create(c)
        .child(TextInput.create(c).handle(textInputHandle))
        .child(
            Row.create(c)
                .child(
                    Button.create(c)
                        .text("Request Focus")
                        .clickHandler(TextInputRequestAndClearFocus.onRequestFocusClick(c))
                        .marginDip(YogaEdge.HORIZONTAL, 8))
                .child(
                    Button.create(c)
                        .text("Clear Focus")
                        .clickHandler(TextInputRequestAndClearFocus.onClearFocusClick(c))))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onRequestFocusClick(
      ComponentContext c, @FromEvent View view, @State Handle textInputHandle) {
    TextInput.requestFocus(c, textInputHandle);
  }

  @OnEvent(ClickEvent.class)
  static void onClearFocusClick(
      ComponentContext c, @FromEvent View view, @State Handle textInputHandle) {
    TextInput.clearFocus(c, textInputHandle);
  }
}
