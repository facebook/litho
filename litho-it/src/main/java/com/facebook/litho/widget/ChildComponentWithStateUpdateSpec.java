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

package com.facebook.litho.widget;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
class ChildComponentWithStateUpdateSpec {

  @OnCreateInitialState
  static void onCreateInitialState(final ComponentContext c, StateValue<Integer> count) {
    count.set(0);
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int count) {
    if (count == 0) {
      ChildComponentWithStateUpdate.onUpdateStateSync(c);
    }
    return Column.create(c)
        .child(Text.create(c).text("click to dispatch").textSizeSp(18).paddingDip(YogaEdge.ALL, 16))
        .child(Text.create(c).textSizeSp(18).text(String.valueOf(count)))
        .build();
  }

  @OnUpdateState
  public static void onUpdateState(StateValue<Integer> count) {

    count.set(count.get() + 1);
  }
}
