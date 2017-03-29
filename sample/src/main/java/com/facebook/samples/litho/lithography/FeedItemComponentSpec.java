/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.ComponentContext;
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
    return Container.create(c)
        .child(
            Container.create(c)
                .child(
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
