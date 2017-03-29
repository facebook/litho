/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.LinearLayoutManager;

import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@link LinearLayoutInfo}
 */
@RunWith(ComponentsTestRunner.class)
public class LinearLayoutInfoTest {

  @Test
  public void testOrientations() {
    final LinearLayoutInfo verticalLinearLayoutInfo = new LinearLayoutInfo(
        RuntimeEnvironment.application,
        VERTICAL,
        false);

    assertEquals(verticalLinearLayoutInfo.getScrollDirection(), VERTICAL);

    final LinearLayoutInfo horizontalLinearLayoutInfo = new LinearLayoutInfo(
        RuntimeEnvironment.application,
        HORIZONTAL,
        false);

    assertEquals(horizontalLinearLayoutInfo.getScrollDirection(), HORIZONTAL);
  }

  @Test
  public void testGetLayoutManager() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        RuntimeEnvironment.application,
        VERTICAL,
        false);

    assertTrue(linearLayoutInfo.getLayoutManager() instanceof LinearLayoutManager);
  }

  @Test
  public void testApproximateRangeVertical() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        RuntimeEnvironment.application,
        VERTICAL,
        false);

    int rangeSize = linearLayoutInfo.approximateRangeSize(10, 10, 10, 100);

    assertEquals(10, rangeSize);
  }

  @Test
  public void testApproximateRangeHorizontal() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        RuntimeEnvironment.application,
        HORIZONTAL,
        false);

    int rangeSize = linearLayoutInfo.approximateRangeSize(10, 10, 100, 10);

    assertEquals(10, rangeSize);
  }

  @Test
  public void testGetChildMeasureSpecVertical() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        RuntimeEnvironment.application,
        VERTICAL,
        false);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final int heightSpec = linearLayoutInfo.getChildHeightSpec(sizeSpec);
    assertEquals(heightSpec, SizeSpec.makeSizeSpec(0, UNSPECIFIED));

    final int widthSpec = linearLayoutInfo.getChildWidthSpec(sizeSpec);
    assertEquals(widthSpec, sizeSpec);
  }

  @Test
  public void testGetChildMeasureSpecHorizontal() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        RuntimeEnvironment.application,
        HORIZONTAL,
        false);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final int heightSpec = linearLayoutInfo.getChildHeightSpec(sizeSpec);
    assertEquals(heightSpec, sizeSpec);

    final int widthSpec = linearLayoutInfo.getChildWidthSpec(sizeSpec);
    assertEquals(widthSpec, SizeSpec.makeSizeSpec(0, UNSPECIFIED));
  }
}
