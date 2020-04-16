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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.LifecycleStep.StepInfo;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import java.util.List;

@MountSpec
public class MountSpecLifecycleTesterSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new StepInfo(LifecycleStep.ON_CREATE_INITIAL_STATE));
  }

  @OnPrepare
  static void onPrepare(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new StepInfo(LifecycleStep.ON_PREPARE));
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop List<LifecycleStep.StepInfo> steps) {

    steps.add(new StepInfo(LifecycleStep.ON_MEASURE));
    size.width = 600;
    size.height = 800;
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c, ComponentLayout layout, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new StepInfo(LifecycleStep.ON_BOUNDS_DEFINED));
  }

  @UiThread
  @OnCreateMountContent
  static View onCreateMountContent(Context c) {
    StaticContainer.sLastCreatedView = new View(c);
    return StaticContainer.sLastCreatedView;
  }

  @UiThread
  @OnMount
  static void onMount(
      ComponentContext context, View view, @Prop List<LifecycleStep.StepInfo> steps) {
    // TODO: (T64290961) Remove the StaticContainer hack for tracing OnCreateMountContent callback.
    if (view == StaticContainer.sLastCreatedView) {
      steps.add(new StepInfo(LifecycleStep.ON_CREATE_MOUNT_CONTENT));
      StaticContainer.sLastCreatedView = null;
    }
    steps.add(new StepInfo(LifecycleStep.ON_MOUNT));
  }

  @UiThread
  @OnUnmount
  static void onUnmount(
      ComponentContext context, View view, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new StepInfo(LifecycleStep.ON_UNMOUNT));
  }

  @UiThread
  @OnBind
  static void onBind(ComponentContext c, View view, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new StepInfo(LifecycleStep.ON_BIND));
  }

  @UiThread
  @OnUnbind
  static void onUnbind(ComponentContext c, View view, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new StepInfo(LifecycleStep.ON_UNBIND));
  }

  @OnAttached
  static void onAttached(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new StepInfo(LifecycleStep.ON_ATTACHED));
  }

  @OnDetached
  static void onDetached(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new StepInfo(LifecycleStep.ON_DETACHED));
  }

  public static class StaticContainer {
    @SuppressLint("StaticFieldLeak")
    public static @Nullable View sLastCreatedView;
  }
}
