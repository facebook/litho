/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.testing;

import com.facebook.components.Component;
import com.facebook.components.ComponentLifecycle;

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
