/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;

public class TestNullLayoutComponent extends Component {

  private static class Lifecycle extends ComponentLifecycle {

    @Override
    protected boolean canMeasure() {
      return true;
    }

    @Override
    protected ComponentLayout onCreateLayoutWithSizeSpec(
        ComponentContext c,
        int widthSpec,
        int heightSpec,
        Component object) {
      return null;
    }
  }

  private static final ComponentLifecycle sLifecycle = new Lifecycle();

  public TestNullLayoutComponent() {
    super(sLifecycle);
  }

  @Override
  public String getSimpleName() {
    return "TestNullLayoutComponent";
  }
}
