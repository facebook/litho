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
import android.graphics.Rect
import android.view.View
import androidx.annotation.UiThread
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Diff
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleTracker
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.StateValue
import com.facebook.litho.TrackingMountContentPool
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnCreateMountContentPool
import com.facebook.litho.annotations.OnCreateTreeProp
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.ShouldUpdate
import com.facebook.litho.annotations.State
import com.facebook.rendercore.MountItemsPool

@MountSpec(isPureRender = true)
object MountSpecPureRenderLifecycleTesterSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      context: ComponentContext,
      dummyState: StateValue<Any?>,
      @Prop lifecycleTracker: LifecycleTracker
  ) {
    dummyState.set(Any())
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_INITIAL_STATE)
  }

  @JvmStatic
  @OnPrepare
  fun onPrepare(
      c: ComponentContext,
      @Prop lifecycleTracker: LifecycleTracker,
      @State dummyState: Any?,
      @CachedValue expensiveValue: Int
  ) {
    lifecycleTracker.addStep(LifecycleStep.ON_PREPARE)
  }

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop lifecycleTracker: LifecycleTracker,
      @Prop(optional = true) intrinsicSize: Size?
  ) {
    val width = SizeSpec.getSize(widthSpec)
    val height = SizeSpec.getSize(heightSpec)
    if (intrinsicSize != null) {
      size.width = SizeSpec.resolveSize(widthSpec, intrinsicSize.width)
      size.height = SizeSpec.resolveSize(heightSpec, intrinsicSize.height)
    } else {
      size.width = width
      size.height = height
    }
    lifecycleTracker.addStep(LifecycleStep.ON_MEASURE, size)
  }

  @JvmStatic
  @OnBoundsDefined
  fun onBoundsDefined(
      c: ComponentContext,
      layout: ComponentLayout,
      @Prop lifecycleTracker: LifecycleTracker
  ) {
    val bounds = Rect(layout.x, layout.y, layout.x + layout.width, layout.y + layout.height)
    lifecycleTracker.addStep(LifecycleStep.ON_BOUNDS_DEFINED, bounds)
  }

  @JvmStatic
  @UiThread
  @OnCreateMountContent
  fun onCreateMountContent(c: Context): View? {
    StaticContainer.sLastCreatedView = View(c)
    return StaticContainer.sLastCreatedView
  }

  @JvmStatic
  @OnCreateMountContentPool
  fun onCreateMountContentPool(): MountItemsPool.ItemPool = TrackingMountContentPool(1, true)

  @JvmStatic
  @UiThread
  @OnMount
  fun onMount(context: ComponentContext, view: View, @Prop lifecycleTracker: LifecycleTracker) {
    // TODO: (T64290961) Remove the StaticContainer hack for tracing OnCreateMountContent callback.
    if (view === StaticContainer.sLastCreatedView) {
      lifecycleTracker.addStep(LifecycleStep.ON_CREATE_MOUNT_CONTENT)
      StaticContainer.sLastCreatedView = null
    }
    lifecycleTracker.addStep(LifecycleStep.ON_MOUNT)
  }

  @JvmStatic
  @UiThread
  @OnUnmount
  fun onUnmount(context: ComponentContext, view: View, @Prop lifecycleTracker: LifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_UNMOUNT)
  }

  @JvmStatic
  @UiThread
  @OnBind
  fun onBind(c: ComponentContext, view: View, @Prop lifecycleTracker: LifecycleTracker) {
    lifecycleTracker.addStep(LifecycleStep.ON_BIND)
  }

  @JvmStatic
  @UiThread
  @OnUnbind
  fun onUnbind(c: ComponentContext, view: View, @Prop lifecycleTracker: LifecycleTracker) {
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

  @JvmStatic
  @OnCalculateCachedValue(name = "expensiveValue")
  fun onCalculateExpensiveValue(@Prop lifecycleTracker: LifecycleTracker): Int {
    lifecycleTracker.addStep(LifecycleStep.ON_CALCULATE_CACHED_VALUE)
    return 0
  }

  @JvmStatic
  @OnCreateTreeProp
  fun onCreateTreePropRect(c: ComponentContext, @Prop lifecycleTracker: LifecycleTracker): Rect {
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_TREE_PROP)
    return Rect()
  }

  @JvmStatic
  @ShouldUpdate
  fun shouldUpdate(
      @Prop(optional = true) shouldUpdate: Diff<Boolean>?,
      @Prop lifecycleTracker: Diff<LifecycleTracker>
  ): Boolean {
    lifecycleTracker.next!!.addStep(LifecycleStep.SHOULD_UPDATE)
    return shouldUpdate?.next ?: true
  }

  object StaticContainer {
    @SuppressLint("StaticFieldLeak") var sLastCreatedView: View? = null
  }
}
