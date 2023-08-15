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

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.annotation.UiThread
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.Size
import com.facebook.litho.TrackingMountContentPool
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnCreateMountContentPool
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.rendercore.MountItemsPool

@MountSpec(canPreallocate = true)
object PreallocatedMountSpecLifecycleTesterSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(context: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_CREATE_INITIAL_STATE))
  }

  @JvmStatic
  @OnPrepare
  fun onPrepare(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_PREPARE))
  }

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop steps: MutableList<StepInfo>
  ) {
    steps.add(StepInfo(LifecycleStep.ON_MEASURE))
    size.width = 600
    size.height = 800
  }

  @JvmStatic
  @OnBoundsDefined
  fun onBoundsDefined(
      c: ComponentContext,
      layout: ComponentLayout,
      @Prop steps: MutableList<StepInfo>
  ) {
    steps.add(StepInfo(LifecycleStep.ON_BOUNDS_DEFINED))
  }

  @JvmStatic
  @UiThread
  @OnCreateMountContentPool
  fun onCreateMountContentPool(): MountItemsPool.ItemPool = TrackingMountContentPool(1, true)

  @JvmStatic
  @UiThread
  @OnCreateMountContent
  fun onCreateMountContent(c: Context): View? {
    StaticContainer.sLastCreatedView = View(c)
    return StaticContainer.sLastCreatedView
  }

  @JvmStatic
  @UiThread
  @OnMount
  fun onMount(context: ComponentContext, view: View, @Prop steps: MutableList<StepInfo>) {
    // TODO: (T64290961) Remove the StaticContainer hack for tracing OnCreateMountContent callback.
    if (view === StaticContainer.sLastCreatedView) {
      steps.add(StepInfo(LifecycleStep.ON_CREATE_MOUNT_CONTENT))
      StaticContainer.sLastCreatedView = null
    }
    steps.add(StepInfo(LifecycleStep.ON_MOUNT))
  }

  @JvmStatic
  @UiThread
  @OnUnmount
  fun onUnmount(context: ComponentContext, view: View, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_UNMOUNT))
  }

  @JvmStatic
  @UiThread
  @OnBind
  fun onBind(c: ComponentContext, view: View, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_BIND))
  }

  @JvmStatic
  @UiThread
  @OnUnbind
  fun onUnbind(c: ComponentContext, view: View, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_UNBIND))
  }

  @JvmStatic
  @OnAttached
  fun onAttached(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_ATTACHED))
  }

  @JvmStatic
  @OnDetached
  fun onDetached(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_DETACHED))
  }

  object StaticContainer {
    @SuppressLint("StaticFieldLeak") var sLastCreatedView: View? = null
  }
}
