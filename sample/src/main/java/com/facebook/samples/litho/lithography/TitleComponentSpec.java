/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

import static android.graphics.Typeface.BOLD;
import static com.facebook.litho.annotations.ResType.STRING;

@LayoutSpec
public class TitleComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop(resType = STRING) String title) {
    return Text.create(c)
        .text(title)
        .textStyle(BOLD)
        .textSizeDip(24)
        .withLayout()
        .backgroundColor(0xDDFFFFFF)
        .positionType(YogaPositionType.ABSOLUTE)
        .positionDip(YogaEdge.BOTTOM, 4)
        .positionDip(YogaEdge.LEFT, 4)
        .paddingDip(YogaEdge.HORIZONTAL, 6)
        .build();
  }
}
