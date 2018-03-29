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
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class MyInjectPropSpec {
  @OnCreateLayout
  public static Component onCreateLayout(
      ComponentContext c,
      @Prop String normalString,
      @InjectProp String injectedString,
      @InjectProp Kettle injectedKettle,
      @InjectProp Text injectedComponent) {
    return Column.create(c).child(injectedComponent).build();
  }

  public static class Kettle {
    public float temperatureCelsius;

    public Kettle(float v) {
      temperatureCelsius = v;
    }
  }
}
