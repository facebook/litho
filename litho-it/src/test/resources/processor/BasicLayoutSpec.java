/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor.integration.resources;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;

@LayoutSpec
public class BasicLayoutSpec {
  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext context,
      @Prop String myStringProp,
      @Prop(resType = ResType.COLOR) int myRequiredColorProp) {
    return null;
  }
}
