/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.litho.Column;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import com.facebook.litho.ComponentLayout;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

@LayoutSpec
public class FeedItemComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final Artist artist,
      @Prop final RecyclerBinder binder) {
    return Column.create(c)
        .child(
            Column.create(c)
                .child(artist.images.length == 1 ?
                    SingleImageComponent.create(c)
                        .image(artist.images[0])
                        .aspectRatio(2)
                        .withLayout() :
                    Recycler.create(c)
                        .binder(binder)
                        .withLayout()
                        .aspectRatio(2))
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
