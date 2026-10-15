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

import com.facebook.litho.BoundaryWorkingRange
import com.facebook.litho.Column as ColumnBuilder
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEnteredRange
import com.facebook.litho.annotations.OnRegisterRanges
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.Reason

/**
 * The pre-`@OnCreateLayoutWithSizeSpec` shape of `FbFeedGrootVideoComponentSpec`: measures during
 * resolve behind the workaround and returns the measured component as its root.
 */
@ExcuseMySpec(reason = Reason.USES_WORKING_RANGES)
@LayoutSpec
internal object LayoutSpecMeasuringWorkingRangeTesterSpec {

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component {
    @Suppress("DEPRECATION") c.lithoWorkAroundIgnoreMeasureInResolve()
    val child = ColumnBuilder.create(c).child(Text.create(c).text("hello")).build()
    child.measure(
        c,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.UNSPECIFIED),
        Size(),
    )
    return child
  }

  @JvmStatic
  @OnRegisterRanges
  fun registerWorkingRanges(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_REGISTER_RANGES))
    LayoutSpecMeasuringWorkingRangeTester.registerBoundaryWorkingRange(c, BoundaryWorkingRange())
  }

  @JvmStatic
  @OnEnteredRange(name = "boundary")
  fun onEnteredWorkingRange(c: ComponentContext, @Prop steps: MutableList<StepInfo>) {
    steps.add(StepInfo(LifecycleStep.ON_ENTERED_RANGE))
  }
}
