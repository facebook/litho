/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.LinearLayoutManager;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

/**
 * Tests for {@link LinearLayoutInfo}
 */
@RunWith(ComponentsTestRunner.class)
public class LinearLayoutInfoTest {

  @Test
  public void testOrientations() {
    final LinearLayoutInfo verticalLinearLayoutInfo = new LinearLayoutInfo(
        application,
        VERTICAL,
        false);

    assertThat(VERTICAL).isEqualTo(verticalLinearLayoutInfo.getScrollDirection());

    final LinearLayoutInfo horizontalLinearLayoutInfo = new LinearLayoutInfo(
        application,
        HORIZONTAL,
        false);

    assertThat(HORIZONTAL).isEqualTo(horizontalLinearLayoutInfo.getScrollDirection());
  }

  @Test
  public void testGetLayoutManager() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        application,
        VERTICAL,
        false);

    assertThat(linearLayoutInfo.getLayoutManager()).isInstanceOf(LinearLayoutManager.class);
  }

  @Test
  public void testApproximateRangeVertical() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        application,
        VERTICAL,
        false);

    int rangeSize = linearLayoutInfo.approximateRangeSize(10, 10, 10, 100);

    assertThat(rangeSize).isEqualTo(10);
  }

  @Test
  public void testApproximateRangeHorizontal() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        application,
        HORIZONTAL,
        false);

    int rangeSize = linearLayoutInfo.approximateRangeSize(10, 10, 100, 10);

    assertThat(rangeSize).isEqualTo(10);
  }

  @Test
  public void testGetChildMeasureSpecVertical() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        application,
        VERTICAL,
        false);
    final int sizeSpec = makeSizeSpec(200, EXACTLY);

    final int heightSpec = linearLayoutInfo.getChildHeightSpec(sizeSpec, null);
    assertThat(makeSizeSpec(0, UNSPECIFIED)).isEqualTo(heightSpec);

    final int widthSpec = linearLayoutInfo.getChildWidthSpec(sizeSpec, null);
    assertThat(sizeSpec).isEqualTo(widthSpec);
  }

  @Test
  public void testGetChildMeasureSpecHorizontal() {
    final LinearLayoutInfo linearLayoutInfo = new LinearLayoutInfo(
        application,
        HORIZONTAL,
        false);
    final int sizeSpec = makeSizeSpec(200, EXACTLY);

    final int heightSpec = linearLayoutInfo.getChildHeightSpec(sizeSpec, null);
    assertThat(sizeSpec).isEqualTo(heightSpec);

    final int widthSpec = linearLayoutInfo.getChildWidthSpec(sizeSpec, null);
    assertThat(makeSizeSpec(0, UNSPECIFIED)).isEqualTo(widthSpec);
  }
}
