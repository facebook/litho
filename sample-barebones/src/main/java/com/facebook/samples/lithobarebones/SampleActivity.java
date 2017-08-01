/*
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.lithobarebones;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentInfo;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

public class SampleActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext context = new ComponentContext(this);

    final RecyclerBinder recyclerBinder = new RecyclerBinder.Builder()
        .layoutInfo(new LinearLayoutInfo(this, OrientationHelper.VERTICAL, false))
        .build(context);

    final Component component = Recycler.create(context)
            .binder(recyclerBinder)
            .build();

    addContent(recyclerBinder, context);

    setContentView(LithoView.create(context, component));
  }

  private static void addContent(RecyclerBinder recyclerBinder, ComponentContext context) {
    for (int i = 0; i < 32; i++) {
      recyclerBinder.insertItemAt(
          i,
          ComponentInfo.create()
              .component(
                  ListItem.create(context)
                      .color(i % 2 == 0 ? Color.WHITE : Color.LTGRAY)
                      .title("Hello, world!")
                      .subtitle("Litho tutorial")
                      .build())
              .build());
    }
  }
}
