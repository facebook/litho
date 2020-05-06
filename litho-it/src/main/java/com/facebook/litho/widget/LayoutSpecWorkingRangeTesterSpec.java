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

import com.facebook.litho.BoundaryWorkingRange;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.Prop;
import java.util.List;

@LayoutSpec
class LayoutSpecWorkingRangeTesterSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c).child(Text.create(c).text("hello")).build();
  }

  @OnRegisterRanges
  static void registerWorkingRanges(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_REGISTER_RANGES));
    LayoutSpecWorkingRangeTester.registerBoundaryWorkingRange(c, new BoundaryWorkingRange());
  }

  @OnEnteredRange(name = "boundary")
  static void onEnteredWorkingRange(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_ENTERED_RANGE));
  }
}
