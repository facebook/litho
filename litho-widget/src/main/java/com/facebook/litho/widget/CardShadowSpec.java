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

package com.facebook.litho.widget;

import android.content.Context;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;

/**
 * A component that is able to render the card's shadow. Used in the implementation of {@link
 * CardSpec}.
 *
 * @prop shadowStartColor Start color for the shadow.
 * @prop shadowEndColor End color for the shadow.
 * @prop cornerRadius Corner radius for the card that shows the shadow.
 * @prop shadowSize Size of the shadow.
 * @prop shadowDx The x offset of the shadow.
 * @prop shadowDy The y offset of the shadow.
 */
@MountSpec(isPureRender = true)
class CardShadowSpec {

  @PropDefault static final float shadowDx = CardShadowDrawable.UNDEFINED;
  @PropDefault static final float shadowDy = CardShadowDrawable.UNDEFINED;

  @OnCreateMountContent
  static CardShadowDrawable onCreateMountContent(Context c) {
    return new CardShadowDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      CardShadowDrawable cardShadowDrawable,
      @Prop(optional = true, resType = ResType.COLOR) int shadowStartColor,
      @Prop(optional = true, resType = ResType.COLOR) int shadowEndColor,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float cornerRadius,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) float shadowSize,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDx,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDy,
      @Prop(optional = true) boolean hideTopShadow,
      @Prop(optional = true) boolean hideBottomShadow) {

    cardShadowDrawable.setShadowStartColor(shadowStartColor);
    cardShadowDrawable.setShadowEndColor(shadowEndColor);
    cardShadowDrawable.setCornerRadius(cornerRadius);
    cardShadowDrawable.setShadowSize(shadowSize);
    cardShadowDrawable.setHideTopShadow(hideTopShadow);
    cardShadowDrawable.setHideBottomShadow(hideBottomShadow);
    cardShadowDrawable.setShadowDx(shadowDx);
    cardShadowDrawable.setShadowDy(shadowDy);
  }
}
