/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.espresso.rules;

import android.support.v7.app.AppCompatActivity;
import com.facebook.litho.Component;
import com.facebook.litho.LithoView;

/**
 * An Activity that hosts a Component in a LithoView. Used for instrumentation and screenshot tests.
 */
public class ComponentActivity extends AppCompatActivity {

  /** Sets or replaces the Component being rendered in this Activity. */
  public void setComponent(Component component) {
    setContentView(LithoView.create(this, component));
  }
}
