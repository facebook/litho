/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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

