/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho;

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
    final String demoName = (String) getIntent().getSerializableExtra("demoName");
    final Component<?> component = Demos.getComponent(demoName);

    final ComponentView componentView = new ComponentView(this);
    componentView.setComponent(
