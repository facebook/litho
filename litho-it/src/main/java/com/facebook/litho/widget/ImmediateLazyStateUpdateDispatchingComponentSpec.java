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

import androidx.annotation.Nullable;
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
import java.util.List;

@LayoutSpec
class ImmediateLazyStateUpdateDispatchingComponentSpec {

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Integer> count) {
    count.set(0);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Nullable @Prop(optional = true) List<LifecycleStep.StepInfo> steps,
      @State(canUpdateLazily = true) int count) {
    if (steps != null) {
      steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_CREATE_LAYOUT));
    }
    ImmediateLazyStateUpdateDispatchingComponent.lazyUpdateCount(c, count + 1);
    return Column.create(c).child(Text.create(c).text("count:" + count)).build();
  }
}
