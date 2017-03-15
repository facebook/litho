// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import android.support.v7.widget.OrientationHelper;

import java.util.Map;
import java.util.LinkedHashMap;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentInfo;
import com.facebook.components.widget.LinearLayoutInfo;
import com.facebook.components.widget.RecyclerBinder;
import com.facebook.samples.components.kittens.DataModel;
import com.facebook.samples.components.kittens.KittensRootComponent;
import com.facebook.samples.components.playground.PlaygroundComponent;

/**
 * The list of Components demos -- Add your demos below!
 */
public final class Demos {

  private static Map<String, Component<?>> demoModels;

  private Demos() {
  }

  public static void initialize(ComponentContext c) {
    final RecyclerBinder recyclerBinder = new RecyclerBinder(
            c,
            4.0f,
            new LinearLayoutInfo(c, OrientationHelper.VERTICAL, false));
    DataModel.populateBinderWithSampleData(recyclerBinder, c);
    demoModels = new LinkedHashMap<>();
    demoModels.put(
        "Kittens App",
        KittensRootComponent.create(c)
            .recyclerBinder(recyclerBinder)
            .build());
    demoModels.put("Playground", PlaygroundComponent.create(c).build());
  }

  public static Component<?> getComponent(String name) {
    return demoModels.get(name);
  }

  public static void addAllToBinder(RecyclerBinder recyclerBinder, ComponentContext c) {
    for (String name : demoModels.keySet()) {
      ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
      componentInfoBuilder.component(
          DemoListItemComponent.create(c)
                  .name(name)
                  .build());
      recyclerBinder.insertItemAt(recyclerBinder.getItemCount(), componentInfoBuilder.build());
    }
  }
}
