// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLifecycle;
import com.facebook.components.ComponentTree;
import com.facebook.components.ComponentView;

public class DemoActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext context = new ComponentContext(this);
    final Class<? extends ComponentLifecycle> cls =
        (Class<? extends ComponentLifecycle>) getIntent().getSerializableExtra("demoClass");
    Component.Builder builder = null;
    try {
      final Method createMethod = cls.getMethod("create", ComponentContext.class);
      builder = (Component.Builder) createMethod.invoke(null, context);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }

    final ComponentView componentView = new ComponentView(this);
    componentView.setComponent(
        ComponentTree.create(context, builder)
            .build());
    setContentView(componentView);
  }
}
