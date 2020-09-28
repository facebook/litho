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
import android.widget.TextView;
import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;

@MountSpec(isPureRender = true)
public class CrashingMountableSpec {
  @OnMeasure
  static void onMeasure(
      final ComponentContext context,
      final ComponentLayout layout,
      final int widthSpec,
      final int heightSpec,
      final Size size,
      @Prop LifecycleStep lifecycle) {
    if (lifecycle.equals(LifecycleStep.ON_MEASURE)) {
      throw new MountPhaseException(LifecycleStep.ON_MEASURE);
    }
    size.height = 200;
    size.width = 600;
  }

  @UiThread
  @OnCreateMountContent
  static TextView onCreateMountContent(final Context c) {
    TextView view = new TextView(c);
    view.setText("Hello World");
    return view;
  }

  @UiThread
  @OnMount
  static void onMount(
      final ComponentContext context,
      final TextView textView,
      final @Prop LifecycleStep lifecycle) {
    if (lifecycle.equals(LifecycleStep.ON_MOUNT)) {
      throw new MountPhaseException(LifecycleStep.ON_MOUNT);
    }
  }

  @UiThread
  @OnUnmount
  static void onUnmount(
      final ComponentContext context,
      final TextView textView,
      final @Prop LifecycleStep lifecycle) {
    if (lifecycle.equals(LifecycleStep.ON_UNMOUNT)) {
      throw new MountPhaseException(LifecycleStep.ON_UNMOUNT);
    }
  }

  @OnBind
  static void onBind(
      final ComponentContext context,
      final TextView textView,
      final @Prop LifecycleStep lifecycle) {
    if (lifecycle.equals(LifecycleStep.ON_BIND)) {
      throw new MountPhaseException(LifecycleStep.ON_BIND);
    }
  }

  @OnUnbind
  static void onUnbind(
      final ComponentContext context,
      final TextView textView,
      final @Prop LifecycleStep lifecycle) {
    if (lifecycle.equals(LifecycleStep.ON_UNBIND)) {
      throw new MountPhaseException(LifecycleStep.ON_UNBIND);
    }
  }

  public static final class MountPhaseException extends RuntimeException {

    public MountPhaseException(LifecycleStep lifecycle) {
      super("Crashed on " + lifecycle.name());
    }
  }
}
