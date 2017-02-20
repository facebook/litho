// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.MountSpec;
import com.facebook.components.annotations.OnCreateMountContent;
import com.facebook.components.annotations.OnMount;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.ResType;

/**
 * A component that is paints rounded edges to mimic a clipping operation on the
 * component being rendered below it. Used in {@link CardSpec}.
 */
@MountSpec(isPureRender = true)
class CardClipSpec {

  @OnCreateMountContent
  static CardClipDrawable onCreateMountContent(ComponentContext c) {
    return new CardClipDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      CardClipDrawable cardClipDrawable,
      @Prop(optional = true, resType = ResType.COLOR) int clippingColor,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float cornerRadius) {

    cardClipDrawable.setClippingColor(clippingColor);
    cardClipDrawable.setCornerRadius(cornerRadius);
  }
}
