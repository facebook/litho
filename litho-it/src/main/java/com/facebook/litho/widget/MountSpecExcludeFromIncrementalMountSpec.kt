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

package com.facebook.litho.widget

import android.content.Context
import android.widget.TextView
import androidx.annotation.UiThread
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleTracker
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.ShouldExcludeFromIncrementalMount

@MountSpec
internal object MountSpecExcludeFromIncrementalMountSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(context: ComponentContext, @Prop lifecycleTracker: LifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_INITIAL_STATE)
  }

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop lifecycleTracker: LifecycleTracker
  ) {
    size.width = SizeSpec.getSize(widthSpec)
    size.height = SizeSpec.getSize(heightSpec)
    lifecycleTracker.addStep(LifecycleStep.ON_MEASURE, size)
  }

  @JvmStatic @ShouldExcludeFromIncrementalMount fun shouldPrefetch(): Boolean = true

  @JvmStatic
  @UiThread
  @OnCreateMountContent
  fun onCreateMountContent(c: Context): TextView = TextView(c)

  @JvmStatic
  @UiThread
  @OnMount
  fun onMount(c: ComponentContext, view: TextView, @Prop lifecycleTracker: LifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_MOUNT)
  }

  @JvmStatic
  @UiThread
  @OnUnmount
  fun onUnmount(
      context: ComponentContext,
      view: TextView,
      @Prop lifecycleTracker: LifecycleTracker
  ) {
    lifecycleTracker.addStep(LifecycleStep.ON_UNMOUNT)
  }

  @JvmStatic
  @UiThread
  @OnBind
  fun onBind(c: ComponentContext, view: TextView, @Prop lifecycleTracker: LifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_BIND)
  }

  @JvmStatic
  @UiThread
  @OnUnbind
  fun onUnbind(c: ComponentContext, view: TextView, @Prop lifecycleTracker: LifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_UNBIND)
  }

  @JvmStatic
  @OnAttached
  fun onAttached(c: ComponentContext, @Prop lifecycleTracker: LifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_ATTACHED)
  }

  @JvmStatic
  @OnDetached
  fun onDetached(c: ComponentContext, @Prop lifecycleTracker: LifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_DETACHED)
  }
}
