/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;

public class Decade implements Datum {
  public int year;

  public Decade(int year) {
    this.year = year;
  }

  @Override
  public Component createComponent(ComponentContext c) {
    return DecadeSeparator.create(c)
        .decade(this)
        .build();
  }
}
