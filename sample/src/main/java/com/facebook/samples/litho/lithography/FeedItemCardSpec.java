/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.yoga.YogaAlign;

import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.VERTICAL;

@LayoutSpec
public class FeedItemCardSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c, @Prop final ArtistDatum artist, @Prop final RecyclerBinder binder) {
    return Column.create(c)
        .flexShrink(0)
        .alignContent(YogaAlign.FLEX_START)
        .paddingDip(VERTICAL, 8)
        .paddingDip(HORIZONTAL, 16)
        .child(Card.create(c)
            .content(FeedItemComponent.create(c).artist(artist).binder(binder)))
        .build();
  }
}
