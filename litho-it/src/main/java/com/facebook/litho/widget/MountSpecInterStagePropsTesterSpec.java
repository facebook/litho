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
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.LifecycleTracker;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.Prop;

@MountSpec
public class MountSpecInterStagePropsTesterSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_INITIAL_STATE);
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop LifecycleTracker lifecycleTracker,
      final Output<Boolean> addOnBindLifecycleStep) {
    addOnBindLifecycleStep.set(true);
    lifecycleTracker.addStep(LifecycleStep.ON_PREPARE);
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop LifecycleTracker lifecycleTracker) {
    size.width = 600;
    size.height = 800;
    lifecycleTracker.addStep(LifecycleStep.ON_MEASURE);
  }

  @UiThread
  @OnCreateMountContent
  static View onCreateMountContent(Context c) {
    return new View(c);
  }

  @UiThread
  @OnBind
  static void onBind(
      ComponentContext c,
      View view,
      @Prop LifecycleTracker lifecycleTracker,
      @FromPrepare Boolean addOnBindLifecycleStep) {
    if (addOnBindLifecycleStep) {
      lifecycleTracker.addStep(LifecycleStep.ON_BIND);
    }
  }
}
