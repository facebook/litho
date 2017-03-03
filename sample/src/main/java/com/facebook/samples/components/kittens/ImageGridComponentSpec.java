// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.kittens;

import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;

import static com.facebook.yoga.YogaAlign.STRETCH;
import static com.facebook.yoga.YogaFlexDirection.COLUMN;
import static com.facebook.yoga.YogaFlexDirection.ROW;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;

@LayoutSpec
public class ImageGridComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop String[] images) {

    final ComponentLayout.Builder row1 =
        Container.create(c)
            .flexDirection(ROW)
            .flex(1)
            .child(
                SingleImageComponent.create(c)
                    .image(images[0])
                    .aspectRatio(images.length < 3 ? 2f : 1)
                    .withLayout()
                    .marginDip(LEFT, 5)
                    .marginDip(RIGHT, 5)
                    .marginDip(BOTTOM, 5)
                    .flex(1))
            .child(images.length < 3 ? null :
                SingleImageComponent.create(c)
                    .image(images[2])
                    .withLayout()
                    .marginDip(RIGHT, 5)
                    .marginDip(BOTTOM, 5)
                    .flex(1));

    final ComponentLayout.Builder row2 =
        Container.create(c)
            .flexDirection(ROW)
            .flex(1)
            .child(
                SingleImageComponent.create(c)
                    .image(images[1])
                    .aspectRatio(images.length < 4 ? 2f : 1)
                    .withLayout()
                    .marginDip(LEFT, 5)
                    .marginDip(RIGHT, 5)
                    .marginDip(BOTTOM, 5)
                    .flex(1))
            .child(images.length < 4 ? null :
                SingleImageComponent.create(c)
                    .image(images[3])
                    .withLayout()
                    .marginDip(RIGHT, 5)
                    .marginDip(BOTTOM, 5)
                    .flex(1));

    return Container.create(c)
        .flexDirection(COLUMN)
        .alignItems(STRETCH)
        .child(row1)
        .child(row2)
        .build();
  }
}
