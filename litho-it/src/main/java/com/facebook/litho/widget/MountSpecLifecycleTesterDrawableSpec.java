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
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.LifecycleTracker;
import com.facebook.litho.MountContentPool;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateValue;
import com.facebook.litho.TrackingMountContentPool;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnCreateMountContentPool;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;

@MountSpec
public class MountSpecLifecycleTesterDrawableSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context,
      StateValue<Object> dummyState,
      @Prop LifecycleTracker lifecycleTracker) {
    dummyState.set(new Object());
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_INITIAL_STATE);
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop LifecycleTracker lifecycleTracker,
      @State Object dummyState,
      @CachedValue int expensiveValue) {
    lifecycleTracker.addStep(LifecycleStep.ON_PREPARE);
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop LifecycleTracker lifecycleTracker,
      @Prop(optional = true) Size intrinsicSize) {

    int width = SizeSpec.getSize(widthSpec);
    int height = SizeSpec.getSize(heightSpec);

    if (intrinsicSize != null) {
      size.width = SizeSpec.resolveSize(widthSpec, intrinsicSize.width);
      size.height = SizeSpec.resolveSize(heightSpec, intrinsicSize.height);
    } else {
      size.width = width;
      size.height = height;
    }

    lifecycleTracker.addStep(LifecycleStep.ON_MEASURE, size);
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c, ComponentLayout layout, @Prop LifecycleTracker lifecycleTracker) {
    final Rect bounds =
        new Rect(
            layout.getX(),
            layout.getY(),
            layout.getX() + layout.getWidth(),
            layout.getY() + layout.getHeight());
    lifecycleTracker.addStep(LifecycleStep.ON_BOUNDS_DEFINED, bounds);
  }

  @UiThread
  @OnCreateMountContent
  static ColorDrawable onCreateMountContent(Context c) {
    return new ColorDrawable();
  }

  @OnCreateMountContentPool
  static MountContentPool onCreateMountContentPool() {
    return new TrackingMountContentPool("MountSpecLifecycleTester", 1, true);
  }

  @UiThread
  @OnMount
  static void onMount(
      ComponentContext context, ColorDrawable drawable, @Prop LifecycleTracker lifecycleTracker) {
    // TODO: (T64290961) Remove the StaticContainer hack for tracing OnCreateMountContent callback.
    if (drawable == StaticContainer.sLastCreatedDrawable) {
      lifecycleTracker.addStep(LifecycleStep.ON_CREATE_MOUNT_CONTENT);
      StaticContainer.sLastCreatedDrawable = null;
    }
    lifecycleTracker.addStep(LifecycleStep.ON_MOUNT);
  }

  @UiThread
  @OnUnmount
  static void onUnmount(
      ComponentContext context, ColorDrawable drawable, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_UNMOUNT);
  }

  @UiThread
  @OnBind
  static void onBind(
      ComponentContext c, ColorDrawable drawable, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_BIND);
  }

  @UiThread
  @OnUnbind
  static void onUnbind(
      ComponentContext c, ColorDrawable drawable, @Prop LifecycleTracker lifecycleTracker) {
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

  @OnCalculateCachedValue(name = "expensiveValue")
  static int onCalculateExpensiveValue(@Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_CALCULATE_CACHED_VALUE);
    return 0;
  }

  @OnCreateTreeProp
  static Rect onCreateTreePropRect(ComponentContext c, @Prop LifecycleTracker lifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_TREE_PROP);
    return new Rect();
  }

  public static class StaticContainer {
    @SuppressLint("StaticFieldLeak")
    public static @Nullable ColorDrawable sLastCreatedDrawable;
  }
}
