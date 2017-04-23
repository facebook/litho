/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentInfo;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;

public class Artist implements Datum {

  public final String name;
  public final String biography;
  public final int year;
  public boolean useGlide;
  public final String[] images;

  public Artist(String name, String biography, int year, boolean useGlide, String... images) {
    this.name = name;
    this.biography = biography;
    this.year = year;
    this.useGlide = useGlide;
    this.images = images;
  }

  @Override
  public Component createComponent(ComponentContext c) {
    final RecyclerBinder imageRecyclerBinder =
        new RecyclerBinder(c, 4.0f, new LinearLayoutInfo(c, OrientationHelper.HORIZONTAL, false));

    for (String image : images) {
      ComponentInfo.Builder imageComponentInfoBuilder = ComponentInfo.create();
      imageComponentInfoBuilder.component(getImageComponent(c, image, useGlide));
      imageRecyclerBinder.insertItemAt(imageRecyclerBinder.getItemCount(),
          imageComponentInfoBuilder.build());
    }
    return FeedItemCard.create(c).artist(this).binder(imageRecyclerBinder).build();
  }

  private Component getImageComponent(ComponentContext c, String imageUrl, boolean useGlide) {
    return useGlide ? GlideSingleImageComponent.create(c).image(imageUrl).aspectRatio(2).build()
        : SingleImageComponent.create(c).image(imageUrl).aspectRatio(2).build();
  }
}
