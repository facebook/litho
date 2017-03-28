// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho;

import android.support.v7.widget.OrientationHelper;

import java.util.Map;
import java.util.LinkedHashMap;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentInfo;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.samples.litho.lithography.DataModel;
import com.facebook.samples.litho.lithography.LithographyRootComponent;
import com.facebook.samples.litho.playground.PlaygroundComponent;

/**
 * The list of Litho demos -- Add your demos below!
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
        "Lithography",
        LithographyRootComponent.create(c)
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
