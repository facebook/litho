// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.kittens;

import com.facebook.components.Component;
import com.facebook.components.ComponentLayout;
import com.facebook.components.InlineLayoutSpec;
import com.facebook.components.Container;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.widget.Card;
import com.facebook.debug.tracer.Tracer;

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
    Tracer.startTracer("FeedItemComponent.onCreateLayout");
    try {
      final Component content = new InlineLayoutSpec() {
        @Override
        public String getSimpleName() {
          return "FeedItemComponent";
        }

        @Override
        public ComponentLayout onCreateLayout(ComponentContext c) {
          final ComponentLayout header =
              HeaderComponent.create(c)
                  .text(item.title)
                  .withLayout()
                  .testKey(ITEMHEADER_PREFIX + index)
                  .build();

          final Component footer =
              FooterComponent.create(c)
                  .text(item.description)
                  .build();

          final ComponentLayout content;
          if (item.images.length == 1) {
            content =
                SingleImageComponent.create(c)
                    .image(item.images[0])
                    .withLayout()
                    .aspectRatio(1)
                    .build();
          } else if (item.images.length <= 4) {
            content =
                ImageGridComponent.create(c)
                    .images(item.images)
                    .withLayout()
                    .aspectRatio(1)
                    .build();
          } else {
            content =
                ImagePagerComponent.create(c)
                    .images(item.images)
                    .withLayout()
                    .aspectRatio(1)
                    .build();
          }

          return Container.create(c)
              .child(header)
              .child(content)
              .child(footer)
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
      Tracer.stopTracer();
    }
  }
}
