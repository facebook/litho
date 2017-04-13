/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;

public class DemoActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Demos.initialize(this);

    final String demoName = (String) getIntent().getSerializableExtra("demoName");

    setContentView(
        LithoView.create(
            this,
            Demos.getComponent(demoName)));
  }
}
