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
import android.graphics.Rect;
import android.widget.TextView;
import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.MountContentPool;
import com.facebook.litho.Size;
import com.facebook.litho.TrackingMountContentPool;
import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnCreateMountContentPool;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;

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
    size.height = 200;
    size.width = 600;
    if (lifecycle.equals(LifecycleStep.ON_MEASURE)) {
      throw new MountPhaseException(LifecycleStep.ON_MEASURE);
    }
  }

  @UiThread
  @OnCreateMountContent
  static TextView onCreateMountContent(final Context c, final @InjectProp LifecycleStep lifecycle) {
    TextView view = new TextView(c);
    view.setText("Hello World");
    if (lifecycle.equals(LifecycleStep.ON_CREATE_MOUNT_CONTENT)) {
      throw new MountPhaseException(LifecycleStep.ON_CREATE_MOUNT_CONTENT);
    }
    return view;
  }

  @UiThread
  @OnMount
  static void onMount(
      final ComponentContext context,
      final TextView textView,
      @Prop String someStringProp,
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

  @OnPrepare
  static void onPrepare(
      ComponentContext c, @Prop LifecycleStep lifecycle, @State Object dummyState) {
    if (lifecycle.equals(LifecycleStep.ON_PREPARE)) {
      throw new MountPhaseException(LifecycleStep.ON_PREPARE);
    }
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c, ComponentLayout layout, @Prop LifecycleStep lifecycle) {
    final Rect bounds =
        new Rect(
            layout.getX(),
            layout.getY(),
            layout.getX() + layout.getWidth(),
            layout.getY() + layout.getHeight());
    if (lifecycle.equals(LifecycleStep.ON_BOUNDS_DEFINED)) {
      throw new MountPhaseException(LifecycleStep.ON_BOUNDS_DEFINED);
    }
  }

  @OnCreateMountContentPool
  static MountContentPool onCreateMountContentPool(@InjectProp LifecycleStep lifecycle) {
    if (lifecycle.equals(LifecycleStep.ON_CREATE_MOUNT_CONTENT_POOL)) {
      throw new MountPhaseException(LifecycleStep.ON_CREATE_MOUNT_CONTENT_POOL);
    }
    return new TrackingMountContentPool("MountSpecLifecycleTester", 1, true);
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
      @Prop Diff<String> someStringProp, @InjectProp LifecycleStep lifecycle) {
    if (lifecycle.equals(LifecycleStep.SHOULD_UPDATE)) {
      throw new MountPhaseException(LifecycleStep.SHOULD_UPDATE);
    }
    return !someStringProp.getPrevious().equals(someStringProp.getNext());
  }

  public static final class MountPhaseException extends RuntimeException {

    public MountPhaseException(LifecycleStep lifecycle) {
      super("Crashed on " + lifecycle.name());
    }
  }
}
