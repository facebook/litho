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
import com.facebook.litho.Column as ColumnBuilder
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.State

@ExcuseMySpec(reason = Reason.LEGACY)
@SuppressLint("DeprecatedLayoutSpecApiInKotlin")
@LayoutSpec
internal object ImmediateLazyStateUpdateDispatchingComponentSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(c: ComponentContext, count: StateValue<Int>) {
    count.set(0)
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State(canUpdateLazily = true) count: Int,
  ): Component {
    ImmediateLazyStateUpdateDispatchingComponent.lazyUpdateCount(c, count + 1)
    return ColumnBuilder.create(c).child(Text.create(c).text("count:$count")).build()
  }
}
