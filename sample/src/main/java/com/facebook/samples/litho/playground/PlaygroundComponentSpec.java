/*
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.playground;

import android.graphics.Color;
import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class PlaygroundComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .child(
            Text.create(c)
                .textSizeSp(20)
                .text("Playground sample"))
        .build();
  }
}
