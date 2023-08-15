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
import com.facebook.litho.Diff
import com.facebook.litho.LifecycleStep
import com.facebook.litho.Size
import com.facebook.litho.TrackingMountContentPool
import com.facebook.litho.annotations.InjectProp
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnCreateMountContentPool
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
object CrashingMountableSpec {

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop lifecycle: LifecycleStep
  ) {
    size.height = 200
    size.width = 600
    if (lifecycle == LifecycleStep.ON_MEASURE) {
      throw MountPhaseException(LifecycleStep.ON_MEASURE)
    }
  }

  @JvmStatic
  @UiThread
  @OnCreateMountContent
  fun onCreateMountContent(c: Context, @InjectProp lifecycle: LifecycleStep): TextView {
    val view = TextView(c)
    view.text = "Hello World"
    if (lifecycle == LifecycleStep.ON_CREATE_MOUNT_CONTENT) {
      throw MountPhaseException(LifecycleStep.ON_CREATE_MOUNT_CONTENT)
    }
    return view
  }

  @JvmStatic
  @UiThread
  @OnMount
  fun onMount(
      context: ComponentContext,
      textView: TextView,
      @Prop someStringProp: String?,
      @Prop lifecycle: LifecycleStep
  ) {
    if (lifecycle == LifecycleStep.ON_MOUNT) {
      throw MountPhaseException(LifecycleStep.ON_MOUNT)
    }
  }

  @JvmStatic
  @UiThread
  @OnUnmount
  fun onUnmount(context: ComponentContext, textView: TextView, @Prop lifecycle: LifecycleStep) {
    if (lifecycle == LifecycleStep.ON_UNMOUNT) {
      throw MountPhaseException(LifecycleStep.ON_UNMOUNT)
    }
  }

  @JvmStatic
  @OnBind
  fun onBind(context: ComponentContext, textView: TextView, @Prop lifecycle: LifecycleStep) {
    if (lifecycle == LifecycleStep.ON_BIND) {
      throw MountPhaseException(LifecycleStep.ON_BIND)
    }
  }

  @JvmStatic
  @OnUnbind
  fun onUnbind(context: ComponentContext, textView: TextView, @Prop lifecycle: LifecycleStep) {
    if (lifecycle == LifecycleStep.ON_UNBIND) {
      throw MountPhaseException(LifecycleStep.ON_UNBIND)
    }
  }

  @JvmStatic
  @OnPrepare
  fun onPrepare(c: ComponentContext, @Prop lifecycle: LifecycleStep, @State dummyState: Any?) {
    if (lifecycle == LifecycleStep.ON_PREPARE) {
      throw MountPhaseException(LifecycleStep.ON_PREPARE)
    }
  }

  @JvmStatic
  @OnBoundsDefined
  fun onBoundsDefined(
      c: ComponentContext,
      layout: ComponentLayout,
      @Prop lifecycle: LifecycleStep
  ) {
    if (lifecycle == LifecycleStep.ON_BOUNDS_DEFINED) {
      throw MountPhaseException(LifecycleStep.ON_BOUNDS_DEFINED)
    }
  }

  @JvmStatic
  @OnCreateMountContentPool
  fun onCreateMountContentPool(@InjectProp lifecycle: LifecycleStep): MountItemsPool.ItemPool {
    if (lifecycle == LifecycleStep.ON_CREATE_MOUNT_CONTENT_POOL) {
      throw MountPhaseException(LifecycleStep.ON_CREATE_MOUNT_CONTENT_POOL)
    }
    return TrackingMountContentPool(1, true)
  }

  @JvmStatic
  @ShouldUpdate(onMount = true)
  fun shouldUpdate(
      @Prop someStringProp: Diff<String>,
      @InjectProp lifecycle: LifecycleStep
  ): Boolean {
    if (lifecycle == LifecycleStep.SHOULD_UPDATE) {
      throw MountPhaseException(LifecycleStep.SHOULD_UPDATE)
    }
    return someStringProp.previous != someStringProp.next
  }

  class MountPhaseException(lifecycle: LifecycleStep) :
      RuntimeException("Crashed on ${lifecycle.name}")
}
