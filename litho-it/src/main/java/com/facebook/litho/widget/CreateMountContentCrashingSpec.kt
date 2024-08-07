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
import com.facebook.litho.LifecycleStep
import com.facebook.litho.Size
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.Reason

@ExcuseMySpec(reason = Reason.LEGACY)
@MountSpec(isPureRender = true)
object CreateMountContentCrashingSpec {

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
  ) {
    size.height = 200
    size.width = 600
  }

  @JvmStatic
  @UiThread
  @OnCreateMountContent
  fun onCreateMountContent(c: Context): TextView {
    val view = TextView(c)
    view.text = "Hello World"
    throw CrashingMountableSpec.MountPhaseException(LifecycleStep.ON_CREATE_MOUNT_CONTENT)
    return view
  }
}
