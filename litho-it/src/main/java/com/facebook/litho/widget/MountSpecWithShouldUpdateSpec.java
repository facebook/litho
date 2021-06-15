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
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ShouldUpdate;
import java.util.List;

/**
 * A MountSpec that lets us test the function of ShouldUpdate/pure-render by supplying an object
 * who's identity determines the result of @ShouldUpdate.
 */
@MountSpec(isPureRender = true)
public class MountSpecWithShouldUpdateSpec {

  @OnCreateMountContent
  static View onCreateMountContent(Context c) {
    return new View(c);
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(@Prop Diff<Object> objectForShouldUpdate) {
    return !objectForShouldUpdate.getPrevious().equals(objectForShouldUpdate.getNext());
  }

  @OnMeasure
  static void onMeasure(
      final ComponentContext c,
      final ComponentLayout layout,
      final int widthSpec,
      final int heightSpec,
      final Size size,
      @Prop List<LifecycleStep> operationsOutput) {
    size.height = 100;
    size.width = 100;
    operationsOutput.add(LifecycleStep.ON_MEASURE);
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      View v,
      @Prop List<LifecycleStep> operationsOutput,
      @Prop Object objectForShouldUpdate) {
    operationsOutput.add(LifecycleStep.ON_MOUNT);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      View v,
      @Prop List<LifecycleStep> operationsOutput,
      @Prop Object objectForShouldUpdate) {
    operationsOutput.add(LifecycleStep.ON_UNMOUNT);
  }
}
