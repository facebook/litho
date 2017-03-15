// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;
import com.facebook.components.ComponentView;

public class DemoListActivity extends AppCompatActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentView componentView = new ComponentView(this);
    final ComponentContext context = new ComponentContext(this);

    Demos.initialize(context);

    componentView.setComponent(
        ComponentTree.create(context, DemoListComponent.create(context))
            .incrementalMount(false)
            .build());
    setContentView(componentView);
  }
}
