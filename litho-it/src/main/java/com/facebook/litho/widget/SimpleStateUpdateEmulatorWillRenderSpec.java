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
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import java.util.ArrayList;
import java.util.List;

@LayoutSpec
public class SimpleStateUpdateEmulatorWillRenderSpec {

  public static final int INITIAL_COUNT = 1;

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Integer> count) {
    count.set(1);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Caller caller,
      @Prop(optional = true) String prefix,
      @State int count) {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    caller.set(c);
    return Column.create(c)
        .child(Text.create(c).text((prefix != null ? prefix : "Text: ") + count))
        .child(LayoutSpecWillRenderReuseTester.create(c).steps(info))
        .build();
  }

  @OnUpdateState
  static void onIncrementCount(StateValue<Integer> count) {
    Integer counter = count.get();
    if (counter == null) {
      throw new RuntimeException("state value is null.");
    }
    count.set(counter + 1);
  }

  public static class Caller {

    ComponentContext c;

    void set(ComponentContext c) {
      this.c = c;
    }

    public void increment() {
      SimpleStateUpdateEmulator.onIncrementCountSync(c);
    }

    public void incrementAsync() {
      SimpleStateUpdateEmulator.onIncrementCount(c);
    }
  }
}
