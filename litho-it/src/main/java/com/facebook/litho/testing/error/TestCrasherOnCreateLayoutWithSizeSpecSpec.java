/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.testing.error;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;

@LayoutSpec
public class TestCrasherOnCreateLayoutWithSizeSpecSpec {
  @OnCreateLayoutWithSizeSpec
  protected static Component onCreateLayoutWithSizeSpec(
      ComponentContext c, final int widthSpec, final int heightSpec) {
    throw new RuntimeException("onCreateLayoutWithSizeSpec crash");
  }
}
