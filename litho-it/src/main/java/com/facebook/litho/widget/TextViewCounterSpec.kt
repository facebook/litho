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
import com.facebook.litho.Size
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State

@MountSpec(isPureRender = true)
internal object TextViewCounterSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(c: ComponentContext, count: StateValue<Int>) {
    count.set(0)
  }

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop viewWidth: Int,
      @Prop viewHeight: Int
  ) {
    size.width = viewWidth
    size.height = viewHeight
  }

  @JvmStatic
  @UiThread
  @OnCreateMountContent
  fun onCreateMountContent(c: Context): TextView = TextView(c)

  @JvmStatic
  @OnMount
  fun onMount(c: ComponentContext, view: TextView, @State count: Int) {
    view.text = count.toString()
    view.setOnClickListener { TextViewCounter.incrementSync(c) }
  }

  @JvmStatic
  @OnUnmount
  fun onUnmount(c: ComponentContext, view: TextView) {
    view.text = 0.toString()
    view.setOnClickListener(null)
  }

  @JvmStatic
  @OnUpdateState
  fun increment(count: StateValue<Int>) {
    count.set(count.get()!! + 1)
  }
}
