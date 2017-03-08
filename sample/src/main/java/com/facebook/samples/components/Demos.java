// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import java.util.Arrays;
import java.util.List;

import com.facebook.samples.components.kittens.KittensRootComponent;
import com.facebook.samples.components.playground.PlaygroundComponent;

/**
 * The list of Components demos -- Add your demos below!
 */
public final class Demos {

  private Demos() {
  }

  public static List<DemoModel> getAll() {
    return Arrays.asList(
        new DemoModel("Kittens App", KittensRootComponent.class),
        new DemoModel("Playground", PlaygroundComponent.class));
  }
}
