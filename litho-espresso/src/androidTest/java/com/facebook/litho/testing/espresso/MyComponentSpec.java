/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.espresso;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class MyComponentSpec {
  @OnCreateLayout
  public static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop String text,
      @Prop(optional=true) Object customViewTag) {
    return Text.create(c)
          .text(text)
          .contentDescription("foobar2")
          .viewTag(customViewTag)
          .testKey("my_test_key")
          .build();
  }
}
