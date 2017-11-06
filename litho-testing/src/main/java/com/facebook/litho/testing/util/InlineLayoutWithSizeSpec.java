/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.util;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.EventHandler;

/** Like {@link InlineLayoutSpec} but uses onCreateLayoutWithSizeSpec */
public abstract class InlineLayoutWithSizeSpec extends Component {

  @Override
  protected ComponentLayout onCreateLayoutWithSizeSpec(
      ComponentContext c, int widthSpec, int heightSpec, Component<?> component) {
    return ((InlineLayoutWithSizeSpec) component)
        .onCreateLayoutWithSizeSpec(c, widthSpec, heightSpec);
  }

  @Override
  public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    // no-op
    return null;
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  public String getSimpleName() {
    // You may want to override this in your inline spec, but it's not required.
    return "InlineLayoutWithSize";
  }

  protected abstract ComponentLayout onCreateLayoutWithSizeSpec(
      ComponentContext c, int widthSpec, int heightSpec);
}
