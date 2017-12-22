/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;

public class TestNullLayoutComponent extends Component {

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  protected Component onCreateLayoutWithSizeSpec(
      ComponentContext c, int widthSpec, int heightSpec) {
    return null;
  }

  @Override
  public String getSimpleName() {
    return "TestNullLayoutComponent";
  }
}
