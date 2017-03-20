// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.Component;
import com.facebook.components.ComponentLayout;
import com.facebook.components.InlineLayoutSpec;
import com.facebook.components.Container;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.widget.Card;

import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.VERTICAL;

@LayoutSpec
public class FeedItemComponentSpec {

  private static final String ITEMHEADER_PREFIX = "itemheader-";

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final DataModel item,
      @Prop final int index) {
    try {
      final Component content = new InlineLayoutSpec() {
        @Override
        public String getSimpleName() {
          return "FeedItemComponent";
        }

        @Override
        public ComponentLayout onCreateLayout(ComponentContext c) {
          return Container.create(c)
              .child(
                  Container.create(c)
                      .child(
                          SingleImageComponent.create(c)
                              .image(item.images[0])
                              .aspectRatio(2f))
                      .child(
                          TitleComponent.create(c)
                              .title(item.title))
                      .child(
                          ActionsComponent.create(c)))
              .child(
                  FooterComponent.create(c)
                      .text(item.description))
              .build();
        }
      };

      return Container.create(c)
          .paddingDip(VERTICAL, 8)
          .paddingDip(HORIZONTAL, 16)
          .child(
              Card.create(c)
                  .content(content))
          .build();
    } finally {
    }
  }
}
