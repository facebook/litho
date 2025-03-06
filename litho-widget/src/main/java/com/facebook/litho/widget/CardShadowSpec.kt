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

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType

/**
 * A component that is able to render the card's shadow. Used in the implementation of [CardSpec].
 *
 * @prop shadowStartColor Start color for the shadow.
 * @prop shadowEndColor End color for the shadow.
 * @prop cornerRadius Corner radius for the card that shows the shadow.
 * @prop shadowSize Size of the shadow.
 * @prop shadowDx The x offset of the shadow.
 * @prop shadowDy The y offset of the shadow.
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
internal object CardShadowSpec {

  @PropDefault val shadowDx: Float = CardShadowDrawable.UNDEFINED
  @PropDefault val shadowDy: Float = CardShadowDrawable.UNDEFINED
  @PropDefault val shadowLeftSizeOverride: Float = CardShadowDrawable.UNDEFINED
  @PropDefault val shadowRightSizeOverride: Float = CardShadowDrawable.UNDEFINED

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true, resType = ResType.COLOR) shadowStartColor: Int,
      @Prop(optional = true, resType = ResType.COLOR) shadowEndColor: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) cornerRadius: Float,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) shadowSize: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowDx: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowDy: Float,
      @Prop(optional = true) hideTopShadow: Boolean,
      @Prop(optional = true) hideBottomShadow: Boolean,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) shadowLeftSizeOverride: Float,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) shadowRightSizeOverride: Float
  ): Component {
    return ExperimentalCardShadow(
        shadowStartColor = shadowStartColor,
        shadowEndColor = shadowEndColor,
        cornerRadius = cornerRadius,
        shadowSize = shadowSize,
        shadowDx = shadowDx,
        shadowDy = shadowDy,
        hideTopShadow = hideTopShadow,
        hideBottomShadow = hideBottomShadow,
        shadowLeftSizeOverride = shadowLeftSizeOverride,
        shadowRightSizeOverride = shadowRightSizeOverride)
  }
}
