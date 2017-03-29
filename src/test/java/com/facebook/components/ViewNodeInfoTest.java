/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;
import android.support.v4.util.Pools;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Tests {@link ViewNodeInfo}
 */
@RunWith(ComponentsTestRunner.class)
public class ViewNodeInfoTest {

  private ViewNodeInfo mViewNodeInfo;
  private LayoutOutput mLayoutOutput;

  @Before
  public void setup() {
    mViewNodeInfo = ViewNodeInfo.acquire();
    mLayoutOutput = new LayoutOutput();
  }

  @Test
  public void testTouchBoundsNoHostTranslation() {
    final InternalNode node = new TouchExpansionTestInternalNode();

