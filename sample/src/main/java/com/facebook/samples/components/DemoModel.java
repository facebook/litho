// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import com.facebook.components.ComponentLifecycle;

public class DemoModel {

  public final String name;
  public final Class<? extends ComponentLifecycle> componentClass;

  public DemoModel(String name, Class<? extends ComponentLifecycle> componentClass) {
    this.name = name;
    this.componentClass = componentClass;
  }
}
