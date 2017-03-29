/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Container;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

import static android.graphics.Color.GRAY;
import static android.graphics.Typeface.ITALIC;
import static com.facebook.litho.annotations.ResType.STRING;

@LayoutSpec
public class FooterComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop(resType = STRING) String text) {
    return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .paddingDip(YogaEdge.ALL, 8)
        .child(
            Text.create(c)
                .text(text)
                .textSizeDip(14)
                .textColor(GRAY)
                .textStyle(ITALIC))
