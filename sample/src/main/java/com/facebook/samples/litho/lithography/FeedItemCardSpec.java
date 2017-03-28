// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.RecyclerBinder;

import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.VERTICAL;

@LayoutSpec
public class FeedItemCardSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final Artist artist,
      @Prop final RecyclerBinder binder) {
    return Container.create(c)
        .paddingDip(VERTICAL, 8)
        .paddingDip(HORIZONTAL, 16)
        .child(
            Card.create(c)
                .content(
                    FeedItemComponent.create(c)
                        .artist(artist)
                        .binder(binder)))
        .build();
  }
}
