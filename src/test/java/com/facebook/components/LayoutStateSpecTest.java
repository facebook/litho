/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestLayoutComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateSpecTest {

  private static final int COMPONENT_ID = 37;

  private int mWidthSpec;
  private int mHeightSpec;
  private LayoutState mLayoutState;
  private Component<?> mComponent;
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mWidthSpec = SizeSpec.makeSizeSpec(39, SizeSpec.EXACTLY);
    mHeightSpec = SizeSpec.makeSizeSpec(41, SizeSpec.EXACTLY);
    mComponent = TestLayoutComponent.create(mContext)
        .build();
    Whitebox.setInternalState(mComponent, "mId", COMPONENT_ID);

    mLayoutState = new LayoutState();
    Whitebox.setInternalState(mLayoutState, "mComponent", mComponent);
    Whitebox.setInternalState(mLayoutState, "mWidthSpec", mWidthSpec);
    Whitebox.setInternalState(mLayoutState, "mHeightSpec", mHeightSpec);
