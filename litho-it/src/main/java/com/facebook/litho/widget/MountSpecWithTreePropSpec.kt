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
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.annotations.TreeProp
import com.facebook.litho.widget.treeprops.SimpleTreeProp

@MountSpec(isPureRender = true)
internal object MountSpecWithTreePropSpec {

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
  @UiThread
  @OnMount
  fun onMount(
      c: ComponentContext,
      view: TextView,
      @State count: Int,
      @TreeProp treeProp: SimpleTreeProp
  ) {
    view.text = treeProp.name
  }
}
