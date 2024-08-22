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

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.config.ComponentsConfiguration

/**
 * A component that is able to display drawable resources. It takes a drawable resource ID as prop.
 *
 * @uidocs
 * @prop drawable Drawable to display.
 * @prop scaleType Scale type for the drawable within the container.
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
object ImageSpec {

  @PropDefault val useIntrinsicSize: Boolean = true

  @OptIn(ExperimentalLithoApi::class)
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(resType = ResType.DRAWABLE) drawable: Drawable?,
      @Prop(optional = true) scaleType: ImageView.ScaleType?,
      @Prop(optional = true) useIntrinsicSize: Boolean,
  ): Component {
    return if (ComponentsConfiguration.usePrimitiveImage &&
        // Currently there is a measurement bug that causes the image to be measured incorrectly
        // so we only want to use the primitive implementation when the fix is enabled.
        ComponentsConfiguration.enablePrimitiveMeasurementFix) {
      ExperimentalImage(drawable, scaleType ?: ImageView.ScaleType.FIT_CENTER, useIntrinsicSize)
    } else {
      ImageComponent.create(c)
          .drawable(drawable)
          .scaleType(scaleType ?: ImageView.ScaleType.FIT_CENTER)
          .useIntrinsicSize(useIntrinsicSize)
          .build()
    }
  }
}
