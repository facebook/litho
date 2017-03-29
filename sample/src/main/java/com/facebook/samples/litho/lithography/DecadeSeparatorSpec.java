/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Container;
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
    return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .flexDirection(YogaFlexDirection.ROW)
        .alignItems(YogaAlign.CENTER)
        .paddingDip(YogaEdge.HORIZONTAL, 16)
        .paddingDip(YogaEdge.VERTICAL, 16)
        .child(
            Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                .heightPx(1)
                .backgroundColor(0xFFAAAAAA)
                .flex(1).flexBasisDip(0))
        .child(
            Text.create(c)
            .text(String.valueOf(decade.year))
            .textSizeDip(14)
            .textColor(0xFFAAAAAA)
            .withLayout().flexShrink(0)
            .marginDip(YogaEdge.HORIZONTAL, 10)
            .flex(0).flexBasisDip(0))
        .child(
            Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                .heightPx(1)
                .backgroundColor(0xFFAAAAAA)
                .flex(1).flexBasisDip(0))
        .build();
  }
}
