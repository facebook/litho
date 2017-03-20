// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.PropDefault;
import com.facebook.components.fresco.FrescoComponent;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;

@LayoutSpec
public class SingleImageComponentSpec {
  @PropDefault
  protected static final float aspectRatio = 1f;

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop String image,
      @Prop(optional = true) float aspectRatio) {
    final DraweeController controller = Fresco.newDraweeControllerBuilder()
        .setUri(image)
        .build();
    return FrescoComponent.create(c)
        .controller(controller)
        .aspectRatio(aspectRatio)
        .buildWithLayout();
  }
}
