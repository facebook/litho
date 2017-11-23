/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.specmodels;

import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;

@LayoutSpec
public class MyGenericSpec<E extends HasEventDispatcher> {
  @OnCreateLayout
  protected ComponentLayout onCreateLayout(ComponentContext c, @Prop E genericProp) {
    return Column.create(c).build();
  }
}
