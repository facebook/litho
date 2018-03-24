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
import com.facebook.litho.EventHandler;
import com.facebook.litho.Transition;

public abstract class InlineLayoutSpec extends Component {

  protected InlineLayoutSpec() {
    super("InlineLayout");
  }

  @Override
  public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    // no-op
    return null;
  }

  @Override
  protected Transition onCreateTransition(ComponentContext c) {
    return null;
  }
}
