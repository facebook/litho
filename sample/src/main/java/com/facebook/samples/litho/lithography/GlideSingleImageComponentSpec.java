package com.facebook.samples.litho.lithography;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.glide.GlideImage;

@LayoutSpec
public class GlideSingleImageComponentSpec {

  @PropDefault
  protected static final float aspectRatio = 1f;

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop String image,
      @Prop(optional = true) float aspectRatio) {
    return GlideImage.create(c)
        .imageUrl(image)
        .aspectRatio(aspectRatio)
        .centerCrop(true)
        .buildWithLayout();
  }
}
