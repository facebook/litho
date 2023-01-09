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
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Diff
import com.facebook.litho.Size
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.ShouldUpdate

/**
 * A MountSpec that lets us test the function of ShouldUpdate/pure-render by supplying an object
 * who's identity determines the result of @ShouldUpdate.
 */
@MountSpec(isPureRender = true)
object RecordsShouldUpdateSpec {

  @JvmStatic
  @OnMeasure
  fun onMeasure(c: ComponentContext, layout: ComponentLayout, w: Int, h: Int, size: Size) {
    size.width = 100
    size.height = 100
  }

  @JvmStatic @OnCreateMountContent fun onCreateMountContent(c: Context): View = View(c)

  @JvmStatic
  @ShouldUpdate(onMount = true)
  fun shouldUpdate(
      @Prop testProp: Diff<Any?>,
      @Prop shouldUpdateCalls: Diff<MutableList<Diff<Any?>>>
  ): Boolean {
    shouldUpdateCalls.next!!.add(testProp)
    return true
  }

  @JvmStatic
  @OnMount
  fun onMount(
      c: ComponentContext,
      v: View,
      @Prop testProp: Any?,
      @Prop shouldUpdateCalls: List<Diff<Any?>>
  ) = Unit
}
