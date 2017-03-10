// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.lithobarebones;

import android.app.Activity;
import android.os.Bundle;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;
import com.facebook.components.ComponentView;
import com.facebook.components.widget.Recycler;
import com.facebook.components.widget.Text;

public class SampleActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentView componentView = new ComponentView(this);
    final ComponentContext context = new ComponentContext(this);

    final ComponentTree componentTree = ComponentTree.create(
        context,
        Recycler.create(context)
                    .binder(new FeedBinder(context)))
            .build();

    componentView.setComponent(componentTree);

    setContentView(componentView);
  }
}
