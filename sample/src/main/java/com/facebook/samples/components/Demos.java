// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import java.util.Arrays;
import java.util.List;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentInfo;
import com.facebook.components.widget.RecyclerBinder;
import com.facebook.samples.components.kittens.KittensRootComponent;
import com.facebook.samples.components.playground.PlaygroundComponent;

/**
 * The list of Components demos -- Add your demos below!
 */
public final class Demos {

  private Demos() {
  }

  private static List<DemoModel> getAll() {
    return Arrays.asList(
        new DemoModel("Kittens App", KittensRootComponent.class),
        new DemoModel("Playground", PlaygroundComponent.class));
  }

  public static void addAllToBinder(RecyclerBinder recyclerBinder, ComponentContext c) {
    for (DemoModel demoModel : getAll()) {
      ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
      componentInfoBuilder.component(
          DemoListItemComponent.create(c)
                  .item(demoModel)
                  .build());
      recyclerBinder.insertItemAt(recyclerBinder.getItemCount(), componentInfoBuilder.build());
    }
  }
}
