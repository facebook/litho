// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;

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
