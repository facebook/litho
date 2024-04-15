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

package com.facebook.litho

import android.graphics.drawable.Drawable
import androidx.annotation.Px
import com.facebook.infer.annotation.ThreadConfined
import com.facebook.yoga.YogaDirection

/**
 * Represents a [Component]'s computed layout state. The computed bounds will be used by the
 * framework to define the size and position of the component's mounted [android.view.View]s and
 * [android.graphics.drawable.Drawable]s returned by [com.facebook.litho.annotations.OnMeasure].
 */
@ThreadConfined(ThreadConfined.ANY)
interface ComponentLayout {

  @get:Px val x: Int

  @get:Px val y: Int

  @get:Px val width: Int

  @get:Px val height: Int

  @get:Px val paddingTop: Int

  @get:Px val paddingRight: Int

  @get:Px val paddingBottom: Int

  @get:Px val paddingLeft: Int

  @get:Deprecated("A non-zero value for padding indicates that padding is set. ")
  val isPaddingSet: Boolean

  val background: Drawable?

  val resolvedLayoutDirection: YogaDirection
}
