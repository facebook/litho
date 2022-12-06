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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text

@LayoutSpec
object ComponentWithStateCrashOnUpdateStateSpec {

  @JvmStatic
  @OnCreateInitialState
  fun OnCreateInitialState(c: ComponentContext, count: StateValue<Int?>) {
    count.set(0)
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @State count: Int): Component =
      Column.create(c).child(Text.create(c).text("Count: $count")).build()

  @JvmStatic
  @OnUpdateState
  fun incrementCount(count: StateValue<Int?>?) {
    throw RuntimeException("crash in OnUpdateState")
  }
}
