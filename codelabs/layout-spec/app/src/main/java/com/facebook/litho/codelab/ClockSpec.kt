/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.codelab

import android.content.Context
import androidx.annotation.UiThread
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Size
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.Prop

@MountSpec(isPureRender = true)
object ClockSpec {

  @OnMeasure
  fun onMeasure(
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop radius: Int
  ) {
    size.height = radius * 2
    size.width = radius * 2
  }

  @UiThread
  @OnCreateMountContent
  fun onCreateMountContent(c: Context): ClockDrawable {
    return ClockDrawable()
  }

  @UiThread
  @OnMount
  fun onMount(
      context: ComponentContext,
      mountedDrawable: ClockDrawable,
      @Prop radius: Int,
      @Prop timeMillis: Long
  ) {
    mountedDrawable.radius = radius
    mountedDrawable.setTime(timeMillis)
  }
}
