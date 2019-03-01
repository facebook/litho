/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.espresso.rules;

import androidx.appcompat.app.AppCompatActivity;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;

/**
 * An Activity that hosts a Component in a LithoView. Used for instrumentation and screenshot tests.
 */
public class ComponentActivity extends AppCompatActivity {

  /** Sets or replaces the Component being rendered in this Activity. */
  public void setComponent(Component component) {
    setContentView(LithoView.create(this, component));
  }

  /** Sets or replaces the ComponentTree being rendered in this Activity. */
  public void setComponentTree(ComponentTree componentTree) {
    final LithoView lithoView = new LithoView(this);
    lithoView.setComponentTree(componentTree);
    setContentView(lithoView);
  }
}
