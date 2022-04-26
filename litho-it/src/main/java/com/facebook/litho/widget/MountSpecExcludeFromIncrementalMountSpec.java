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

package com.facebook.litho.widget;

import android.content.Context;
import android.widget.TextView;
import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.LifecycleTracker;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;

@MountSpec(excludeFromIncrementalMount = true)
class MountSpecExcludeFromIncrementalMountSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_INITIAL_STATE);
  }

  @OnMeasure
  static void onMeasure(
      final ComponentContext c,
      final ComponentLayout layout,
      final int widthSpec,
      final int heightSpec,
      final Size size,
      @Prop LifecycleTracker lifecycleTracker) {
    size.width = SizeSpec.getSize(widthSpec);
    size.height = SizeSpec.getSize(heightSpec);
    lifecycleTracker.addStep(LifecycleStep.ON_MEASURE, size);
  }

  @UiThread
  @OnCreateMountContent
  static TextView onCreateMountContent(Context c) {
    return new TextView(c);
  }

  @UiThread
  @OnMount
  static void onMount(
      final ComponentContext c, final TextView view, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_MOUNT);
  }

  @UiThread
  @OnUnmount
  static void onUnmount(
      final ComponentContext context,
      final TextView view,
      @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_UNMOUNT);
  }

  @UiThread
  @OnBind
  static void onBind(ComponentContext c, TextView view, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_BIND);
  }

  @UiThread
  @OnUnbind
  static void onUnbind(
      final ComponentContext c, final TextView view, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_UNBIND);
  }

  @OnAttached
  static void onAttached(ComponentContext c, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_ATTACHED);
  }

  @OnDetached
  static void onDetached(ComponentContext c, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_DETACHED);
  }
}
