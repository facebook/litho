// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.lithobarebones;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.OrientationHelper;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentInfo;
import com.facebook.components.ComponentTree;
import com.facebook.components.ComponentView;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

public class SampleActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentView componentView = new ComponentView(this);
    final ComponentContext context = new ComponentContext(this);

    final RecyclerBinder recyclerBinder = new RecyclerBinder(
        context,
        4.0f,
        new LinearLayoutInfo(this, OrientationHelper.VERTICAL, false));

    final ComponentTree componentTree = ComponentTree.create(
        context,
        Recycler.create(context)
                    .binder(recyclerBinder))
            .build();

    componentView.setComponent(componentTree);

    addContent(recyclerBinder, context);

    setContentView(componentView);
  }

  private static void addContent(RecyclerBinder recyclerBinder, ComponentContext context) {
    for (int i = 0; i < 32; i++) {
      ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
      componentInfoBuilder.component(
              FeedItem.create(context)
                      .color(i % 2 == 0 ? Color.WHITE : Color.LTGRAY)
                      .message("Hello, world!")
                      .build());
      recyclerBinder.insertItemAt(i, componentInfoBuilder.build());
    }
  }
}
