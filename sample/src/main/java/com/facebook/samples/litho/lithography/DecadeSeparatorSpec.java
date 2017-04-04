/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.litho.Row;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;

@LayoutSpec
public class DecadeSeparatorSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final Decade decade) {
    return Row.create(c)
        .alignItems(YogaAlign.CENTER)
        .paddingDip(YogaEdge.ALL, 16)
        .child(
            Row.create(c)
                .heightPx(1)
                .backgroundColor(0xFFAAAAAA)
                .flex(1))
        .child(
            Text.create(c)
                .text(String.valueOf(decade.year))
                .textSizeDip(14)
                .textColor(0xFFAAAAAA)
                .withLayout()
                .marginDip(YogaEdge.HORIZONTAL, 10)
                .flex(0))
        .child(
            Row.create(c)
                .heightPx(1)
                .backgroundColor(0xFFAAAAAA)
                .flex(1))
        .build();
  }
}
