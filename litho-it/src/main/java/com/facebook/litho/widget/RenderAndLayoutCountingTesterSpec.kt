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
import com.facebook.litho.RenderAndMeasureCounter
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.ShouldUpdate

@MountSpec(isPureRender = true)
object RenderAndLayoutCountingTesterSpec {

  @JvmStatic
  @OnPrepare
  fun onPrepare(
      c: ComponentContext,
      @Prop renderAndMeasureCounter: RenderAndMeasureCounter,
      @Prop(optional = true) listener: Listener?
  ) {
    listener?.onPrepare()
    renderAndMeasureCounter.incrementRenderCount()
  }

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop renderAndMeasureCounter: RenderAndMeasureCounter,
      @Prop(optional = true) listener: Listener?
  ) {
    listener?.onMeasure()
    size.width = SizeSpec.getSize(widthSpec)
    size.height = SizeSpec.getSize(heightSpec)
    renderAndMeasureCounter.incrementMeasureCount()
  }

  @JvmStatic @UiThread @OnCreateMountContent fun onCreateMountContent(c: Context): View = View(c)

  @JvmStatic @ShouldUpdate fun shouldUpdate(): Boolean = true

  interface Listener {
    fun onPrepare()

    fun onMeasure()
  }
}
