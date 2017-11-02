/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.espresso.rules;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.view.ViewGroup;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
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

  /** @return the LithoView associated with the Activity. */
  public LithoView getLithoView() {
    return (LithoView) ((ViewGroup) getActivity().findViewById(android.R.id.content)).getChildAt(0);
  }
}
