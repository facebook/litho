// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentLifecycle;

public class TestNullLayoutComponent extends Component {

  private static class Lifecycle extends ComponentLifecycle {

    @Override
    protected boolean canMeasure() {
      return true;
    }

    @Override
    protected ComponentLayout onCreateLayoutWithSizeSpec(
        ComponentContext c,
        int widthSpec,
        int heightSpec,
        Component object) {
      return null;
    }
  }

  private static final ComponentLifecycle sLifecycle = new Lifecycle();

  public TestNullLayoutComponent() {
    super(sLifecycle);
  }

  @Override
  public String getSimpleName() {
    return "TestNullLayoutComponent";
  }
}
