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
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
class CounterComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int count) {
    return Row.create(c)
        .child(
            Text.create(c)
                .text("+")
                .textSizeDip(30)
                .marginDip(YogaEdge.ALL, 10)
                .clickHandler(CounterComponent.onClickIncrease(c)))
        .child(Text.create(c).text("" + count).textSizeDip(30).marginDip(YogaEdge.ALL, 10))
        .child(
            Text.create(c)
                .text("-")
                .textSizeDip(30)
                .marginDip(YogaEdge.ALL, 10)
                .clickHandler(CounterComponent.onClickDecrease(c)))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickIncrease(ComponentContext c) {
    CounterComponent.increase(c);
  }

  @OnEvent(ClickEvent.class)
  static void onClickDecrease(ComponentContext c) {
    CounterComponent.decrease(c);
  }

  @OnUpdateState
  static void increase(StateValue<Integer> count) {
    count.set(count.get() + 1);
  }

  @OnUpdateState
  static void decrease(StateValue<Integer> count) {
    count.set(count.get() - 1);
  }
}
