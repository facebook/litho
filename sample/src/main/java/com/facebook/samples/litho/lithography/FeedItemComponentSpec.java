// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;

@LayoutSpec
public class FeedItemComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final Artist artist) {
    return Container.create(c)
        .child(
            Container.create(c)
                .child(
                    SingleImageComponent.create(c)
                        .image(artist.images[0])
                        .aspectRatio(2f))
                .child(
                    TitleComponent.create(c)
                        .title(artist.name))
                .child(
                    ActionsComponent.create(c)))
        .child(
            FooterComponent.create(c)
                .text(artist.biography))
        .build();
  }
}
