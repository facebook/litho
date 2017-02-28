// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

/**
 * A {@link Component} that wraps another component for testing purposes. This component has a
 * lifecycle that doesn't override any methods.
 */
class TestComponent extends Component {

  private static final ComponentLifecycle TEST_LIFECYCLE = new ComponentLifecycle() {};

  private final Component<?> mWrappedComponent;

  TestComponent(Component<?> component) {
    super(TEST_LIFECYCLE);

    mWrappedComponent = component;
  }

  @Override
  public String getSimpleName() {
    return mWrappedComponent.getSimpleName();
  }

  Component<?> getWrappedComponent() {
    return mWrappedComponent;
  }
}
