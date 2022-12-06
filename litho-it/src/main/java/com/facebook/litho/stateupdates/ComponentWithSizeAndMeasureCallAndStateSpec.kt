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

package com.facebook.litho.stateupdates

import android.graphics.Rect
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec
import com.facebook.litho.annotations.OnCreateTreeProp
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text

@LayoutSpec
object ComponentWithSizeAndMeasureCallAndStateSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      count: StateValue<Int?>,
      @Prop steps: MutableList<StepInfo?>
  ) {
    steps.add(StepInfo(LifecycleStep.ON_CREATE_INITIAL_STATE))
    count.set(0)
  }

  @JvmStatic
  @OnCreateLayoutWithSizeSpec
  fun onCreateLayout(
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      @Prop shouldCacheResult: Boolean,
      @Prop steps: MutableList<StepInfo?>,
      @Prop(optional = true) component: Component?,
      @Prop(optional = true) mountSpec: Component?,
      @Prop(optional = true) prefix: String?,
      @CachedValue expensiveValue: Int,
      @State count: Int
  ): Component {
    steps.add(StepInfo(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC))
    component?.measure(
        c,
        if (widthSpec != 0) widthSpec else SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        if (heightSpec != 0) heightSpec else SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        Size(),
        shouldCacheResult)
    val textPrefix = prefix ?: "Count:"
    return Column.create(c)
        .child(Text.create(c).text("$textPrefix $count"))
        .child(component ?: mountSpec)
        .build()
  }

  @JvmStatic
  @OnAttached
  fun onAttached(c: ComponentContext, @Prop steps: MutableList<StepInfo?>) {
    steps.add(StepInfo(LifecycleStep.ON_ATTACHED))
  }

  @JvmStatic
  @OnDetached
  fun onDetached(c: ComponentContext, @Prop steps: MutableList<StepInfo?>) {
    steps.add(StepInfo(LifecycleStep.ON_DETACHED))
  }

  @JvmStatic
  @OnCreateTreeProp
  fun onCreateTreePropRect(c: ComponentContext, @Prop steps: MutableList<StepInfo?>): Rect {
    steps.add(StepInfo(LifecycleStep.ON_CREATE_TREE_PROP))
    return Rect()
  }

  @JvmStatic
  @OnCalculateCachedValue(name = "expensiveValue")
  fun onCalculateExpensiveValue(@Prop steps: MutableList<StepInfo?>): Int {
    steps.add(StepInfo(LifecycleStep.ON_CALCULATE_CACHED_VALUE))
    return 0
  }

  @JvmStatic
  @OnUpdateState
  fun onIncrementCount(count: StateValue<Int?>) {
    val counter = count.get() ?: throw RuntimeException("state value is null.")
    count.set(counter + 1)
  }

  class Caller : BaseIncrementStateCaller {
    override fun increment(c: ComponentContext?) {
      ComponentWithSizeAndMeasureCallAndState.onIncrementCountSync(c)
    }
  }
}
