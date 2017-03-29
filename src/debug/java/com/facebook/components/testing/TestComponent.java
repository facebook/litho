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

