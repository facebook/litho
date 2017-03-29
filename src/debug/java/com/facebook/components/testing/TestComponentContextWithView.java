/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import android.content.Context;
import android.view.View;

import com.facebook.litho.ComponentContext;

public class TestComponentContextWithView extends ComponentContext {

  private final View mTestView;

  public TestComponentContextWithView(Context c) {
    super(c);
    if (c instanceof TestComponentContextWithView) {
      mTestView = ((TestComponentContextWithView) c).getTestView();
    } else {
      mTestView = new View(c);
    }
  }

