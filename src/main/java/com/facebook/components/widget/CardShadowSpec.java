// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.content.Context;

import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.OnCreateMountContent;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.MountSpec;
import com.facebook.components.annotations.OnMount;
import com.facebook.components.annotations.ResType;

/**
 * A component that is able to render the card's shadow. Used in the
 * implementation of {@link CardSpec}.
 */
@MountSpec(isPublic = false, isPureRender = true)
class CardShadowSpec {

  @OnCreateMountContent
  static CardShadowDrawable onCreateMountContent(ComponentContext c) {
    return new CardShadowDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      CardShadowDrawable cardShadowDrawable,
      @Prop(optional = true, resType = ResType.COLOR) int shadowStartColor,
      @Prop(optional = true, resType = ResType.COLOR) int shadowEndColor,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float cornerRadius,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) float shadowSize) {

    cardShadowDrawable.setShadowStartColor(shadowStartColor);
    cardShadowDrawable.setShadowEndColor(shadowEndColor);
    cardShadowDrawable.setCornerRadius(cornerRadius);
    cardShadowDrawable.setShadowSize(shadowSize);
  }
}
