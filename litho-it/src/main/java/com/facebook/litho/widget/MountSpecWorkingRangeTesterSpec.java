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

import android.content.Context;
import android.view.View;
import androidx.annotation.UiThread;
import com.facebook.litho.BoundaryWorkingRange;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.Prop;
import java.util.List;

@MountSpec
class MountSpecWorkingRangeTesterSpec {

  @OnPrepare
  static void onPrepare(ComponentContext c) {}

  @OnMeasure
  static void onMeasure(
      ComponentContext context, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    size.width = 600;
    size.height = 800;
  }

  @UiThread
  @OnCreateMountContent
  static View onCreateMountContent(Context c) {
    return new View(c);
  }

  @OnRegisterRanges
  static void registerWorkingRanges(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_REGISTER_RANGES));
    MountSpecWorkingRangeTester.registerBoundaryWorkingRange(c, new BoundaryWorkingRange());
  }

  @OnEnteredRange(name = "boundary")
  static void onEnteredWorkingRange(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_ENTERED_RANGE));
  }
}
