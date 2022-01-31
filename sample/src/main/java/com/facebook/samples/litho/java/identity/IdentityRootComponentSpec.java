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

package com.facebook.samples.litho.java.identity;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;

@LayoutSpec
class IdentityRootComponentSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<Boolean> isFirstCounterEnabled,
      StateValue<Boolean> isSecondCounterEnabled) {
    isFirstCounterEnabled.set(true);
    isSecondCounterEnabled.set(true);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State boolean isFirstCounterEnabled,
      @State boolean isSecondCounterEnabled) {
    return Column.create(c)
        .child(
            isFirstCounterEnabled
                ? // start_manual_key
                Row.create(c)
                    .key("first_row")
                    .child(CounterComponent.create(c))
                    .child(
                        Text.create(c)
                            .text("X")
                            .clickHandler(IdentityRootComponent.onClickRemoveFirstChild(c)))
                    .build() // end_manual_key
                : null)
        .child(
            isSecondCounterEnabled
                ? Row.create(c)
                    .key("second_row")
                    .child(CounterComponent.create(c))
                    .child(
                        Text.create(c)
                            .text("X")
                            .clickHandler(IdentityRootComponent.onClickRemoveSecondChild(c)))
                    .build()
                : null)
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickRemoveFirstChild(ComponentContext c) {
    IdentityRootComponent.onRemoveFirstChild(c);
  }

  @OnEvent(ClickEvent.class)
  static void onClickRemoveSecondChild(ComponentContext c) {
    IdentityRootComponent.onRemoveSecondChild(c);
  }

  @OnUpdateState
  static void onRemoveFirstChild(StateValue<Boolean> isFirstCounterEnabled) {
    isFirstCounterEnabled.set(false);
  }

  @OnUpdateState
  static void onRemoveSecondChild(StateValue<Boolean> isSecondCounterEnabled) {
    isSecondCounterEnabled.set(false);
  }

  // start_lazy_state
  @OnEvent(VisibleEvent.class)
  static void onClickEvent(ComponentContext c, @State(canUpdateLazily = true) boolean logOnce) {
    if (!logOnce) {
      // do some logging
      IdentityRootComponent.lazyUpdateLogOnce(c, true);
    }
  }
  // end_lazy_state
}
