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
import android.view.View
import androidx.annotation.UiThread
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleTracker
import com.facebook.litho.Output
import com.facebook.litho.Size
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.FromBoundsDefined
import com.facebook.litho.annotations.FromMeasure
import com.facebook.litho.annotations.FromPrepare
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import java.util.concurrent.atomic.AtomicReference

@MountSpec
object MountSpecInterStagePropsTesterSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      context: ComponentContext,
      @Prop lifecycleTracker: LifecycleTracker,
      addOnUnbindLifecycleStep: StateValue<AtomicReference<Boolean>>
  ) {
    lifecycleTracker.addStep(LifecycleStep.ON_CREATE_INITIAL_STATE)
    addOnUnbindLifecycleStep.set(AtomicReference(null))
  }

  @JvmStatic
  @OnPrepare
  fun onPrepare(
      c: ComponentContext,
      @Prop lifecycleTracker: LifecycleTracker,
      addOnBindLifecycleStep: Output<Boolean>
  ) {
    addOnBindLifecycleStep.set(true)
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
      addOnMountLifecycleStep: Output<Boolean>
  ) {
    size.width = 600
    size.height = 800
    lifecycleTracker.addStep(LifecycleStep.ON_MEASURE)
    addOnMountLifecycleStep.set(true)
  }

  @JvmStatic
  @UiThread
  @OnMount
  fun onMount(
      context: ComponentContext,
      view: View?,
      @Prop lifecycleTracker: LifecycleTracker,
      @FromMeasure addOnMountLifecycleStep: Boolean
  ) {
    if (addOnMountLifecycleStep) {
      lifecycleTracker.addStep(LifecycleStep.ON_MOUNT)
    }
  }

  @JvmStatic @UiThread @OnCreateMountContent fun onCreateMountContent(c: Context): View = View(c)

  @JvmStatic
  @UiThread
  @OnBind
  fun onBind(
      c: ComponentContext,
      view: View,
      @Prop lifecycleTracker: LifecycleTracker,
      @FromPrepare addOnBindLifecycleStep: Boolean,
      @State addOnUnbindLifecycleStep: AtomicReference<Boolean>,
      @FromMeasure addOnMountLifecycleStep: Boolean
  ) {
    if (addOnBindLifecycleStep) {
      lifecycleTracker.addStep(LifecycleStep.ON_BIND)
    }
    addOnUnbindLifecycleStep.set(true)
  }

  @JvmStatic
  @UiThread
  @OnUnbind
  fun onUnbind(
      c: ComponentContext,
      view: View,
      @Prop lifecycleTracker: LifecycleTracker,
      @State addOnUnbindLifecycleStep: AtomicReference<Boolean>
  ) {
    if (addOnUnbindLifecycleStep.get()) {
      lifecycleTracker.addStep(LifecycleStep.ON_UNBIND)
    }
  }

  @JvmStatic
  @OnBoundsDefined
  fun onBoundsDefined(
      c: ComponentContext,
      layout: ComponentLayout,
      @Prop lifecycleTracker: LifecycleTracker,
      addOnUnMountLifecycleStep: Output<Boolean>
  ) {
    addOnUnMountLifecycleStep.set(true)
  }

  @JvmStatic
  @UiThread
  @OnUnmount
  fun onUnmount(
      context: ComponentContext,
      view: View,
      @Prop lifecycleTracker: LifecycleTracker,
      @FromBoundsDefined addOnUnMountLifecycleStep: Boolean
  ) {
    if (addOnUnMountLifecycleStep) {
      lifecycleTracker.addStep(LifecycleStep.ON_UNMOUNT)
    }
  }
}
