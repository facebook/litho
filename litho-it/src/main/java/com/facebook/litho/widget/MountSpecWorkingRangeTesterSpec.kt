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
import com.facebook.litho.BoundaryWorkingRange
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.Size
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnEnteredRange
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.OnRegisterRanges
import com.facebook.litho.annotations.Prop

@MountSpec
internal object MountSpecWorkingRangeTesterSpec {

  @JvmStatic @OnPrepare fun onPrepare(c: ComponentContext) = Unit

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size
  ) {
    size.width = 600
    size.height = 800
  }

  @JvmStatic @UiThread @OnCreateMountContent fun onCreateMountContent(c: Context): View = View(c)

  @JvmStatic
  @OnRegisterRanges
  fun registerWorkingRanges(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_REGISTER_RANGES))
    MountSpecWorkingRangeTester.registerBoundaryWorkingRange(c, BoundaryWorkingRange())
  }

  @JvmStatic
  @OnEnteredRange(name = "boundary")
  fun onEnteredWorkingRange(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_ENTERED_RANGE))
  }
}
