/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentLifecycle;

/**
 * Base class for test components which expose lifecycle information.
 *
 * @param <L>
 */
public abstract class TestComponent<L extends ComponentLifecycle> extends Component<L> {

  private boolean mOnMountCalled;
  private boolean mMounted;
  private boolean mOnUnmountCalled;
  private boolean mOnBoundsDefinedCalled;
  private boolean mOnBindCalled;
  private boolean mBound;
  private boolean mOnUnbindCalled;
  protected boolean mIsUnique;
  private boolean mOnMeasureCalled;

  protected TestComponent(L lifecycle) {
    super(lifecycle);
  }

  @Override
  public String getSimpleName() {
    return "TestComponent";
  }

  void onMountCalled() {
    mOnMountCalled = true;
    mMounted = true;
  }

  void onUnmountCalled() {
    mOnUnmountCalled = true;
    mMounted = false;
  }

  void onMeasureCalled() {
    mOnMeasureCalled = true;
  }

  void onDefineBoundsCalled() {
    mOnBoundsDefinedCalled = true;
  }

  void onBindCalled() {
    mOnBindCalled = true;
    mBound = true;
  }

  void onUnbindCalled() {
    mOnUnbindCalled = true;
    mBound = false;
  }

  /**
   * @return Whether onMount has been called.
   */
  public boolean wasOnMountCalled() {
    return mOnMountCalled;
  }

  /**
   * @return Whether the component is currently mounted.
   */
  public boolean isMounted() {
    return mMounted;
  }

  /**
   * @return Whether onUnmount has been called.
   */
  public boolean wasOnUnmountCalled() {
    return mOnUnmountCalled;
  }

  /**
   * @return Whether onBoundsDefined has been called.
   */
  public boolean wasOnBoundsDefinedCalled() {
    return mOnBoundsDefinedCalled;
  }

  /**
   * @return Whether onBind has been called.
   */
  public boolean wasOnBindCalled() {
    return mOnBindCalled;
  }

  /**
   * @return Whether the component is bound.
   */
  public boolean isBound() {
    return mBound;
  }

  /**
   * @return Whether onUnbind has been called.
   */
  public boolean wasOnUnbindCalled() {
    return mOnUnbindCalled;
  }

  public boolean wasMeasureCalled() {
    return mOnMeasureCalled;
  }

  @Override
  public int hashCode() {
    return mIsUnique ? 1 : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o instanceof TestComponent) {
      TestComponent c = (TestComponent) o;
      return !(mIsUnique || c.mIsUnique);
    }
    return false;
  }

  /**
   * Reset the tracking of which methods have been called on this component.
   */
  public void resetInteractions() {
    mOnMeasureCalled = false;
    mOnBoundsDefinedCalled = false;
    mOnBindCalled = false;
    mOnMountCalled = false;
    mOnUnbindCalled = false;
    mOnUnmountCalled = false;
  }
