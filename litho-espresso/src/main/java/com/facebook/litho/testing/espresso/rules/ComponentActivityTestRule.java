/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.espresso.rules;

import android.app.Instrumentation;
import android.view.ViewGroup;
import androidx.test.InstrumentationRegistry;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.espresso.LithoActivityTestRule;

/**
 * A test rule for instrumentation and screenshot tests that want to render a Component in an
 * Activity. Developers wishing to use this rule can use {@link #setComponent} (along with {@link
 * #getComponentContext}) to show the Component they want rendered.
 */
public class ComponentActivityTestRule extends LithoActivityTestRule<ComponentActivity> {

  private volatile ComponentContext mComponentContext;

  public ComponentActivityTestRule() {
    super(ComponentActivity.class);
  }

  @Override
  protected synchronized void afterActivityLaunched() {
    super.afterActivityLaunched();
    mComponentContext = new ComponentContext(getActivity());
  }

  @Override
  protected synchronized void afterActivityFinished() {
    super.afterActivityFinished();
    mComponentContext = null;
  }

  /** @return a ComponentContext associated with this Activity. */
  public synchronized ComponentContext getComponentContext() {
    if (mComponentContext == null) {
      throw new RuntimeException("Tried to access ComponentContext before Activity was created");
    }
    return mComponentContext;
  }

  /** Set the Component for the Activity to display. */
  public void setComponent(final Component component) {
    final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    instrumentation.runOnMainSync(
        new Runnable() {
          @Override
          public void run() {
            getActivity().setComponent(component);
          }
        });
    instrumentation.waitForIdleSync();
  }

  /** Set ComponentTree for the Activity to display. */
  public void setComponentTree(final ComponentTree componentTree) {
    final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    instrumentation.runOnMainSync(
        new Runnable() {
          @Override
          public void run() {
            getActivity().setComponentTree(componentTree);
          }
        });
    instrumentation.waitForIdleSync();
  }

  /** @return the LithoView associated with the Activity. */
  public LithoView getLithoView() {
    return (LithoView) ((ViewGroup) getActivity().findViewById(android.R.id.content)).getChildAt(0);
  }
}
