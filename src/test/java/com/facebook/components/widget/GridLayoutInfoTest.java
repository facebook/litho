/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.GridLayoutManager;

import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(ComponentsTestRunner.class)
public class GridLayoutInfoTest {

  @Test
  public void testOrientation() {
    final GridLayoutInfo verticalGridLayoutInfo = createGridLayoutInfo(VERTICAL, 2);

    assertThat(verticalGridLayoutInfo.getScrollDirection()).isEqualTo(VERTICAL);

    final GridLayoutInfo horizontalGridLayoutInfo = createGridLayoutInfo(HORIZONTAL, 2);

    assertThat(horizontalGridLayoutInfo.getScrollDirection()).isEqualTo(HORIZONTAL);
  }

  @Test
  public void testLayoutManagerIsGrid() {
    final GridLayoutInfo gridLayoutInfo = createGridLayoutInfo(VERTICAL, 2);

    assertThat(gridLayoutInfo.getLayoutManager()).isExactlyInstanceOf(GridLayoutManager.class);
  }

  @Test
  public void testApproximateRangeVertical() {
    final GridLayoutInfo gridLayoutInfo = createGridLayoutInfo(VERTICAL, 3);
    int rangeSize = gridLayoutInfo.approximateRangeSize(10, 10, 30, 100);

    assertThat(rangeSize).isEqualTo(30);
  }

  @Test
  public void testApproximateRangeHorizontal() {
    final GridLayoutInfo gridLayoutInfo = createGridLayoutInfo(HORIZONTAL, 2);
    int rangeSize = gridLayoutInfo.approximateRangeSize(15, 10, 100, 20);

    assertThat(rangeSize).isEqualTo(14);
  }

  @Test
  public void testGetChildMeasureSpecVertical() {
    final GridLayoutInfo gridLayoutInfo = createGridLayoutInfo(VERTICAL, 3);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final int heightSpec = gridLayoutInfo.getChildHeightSpec(sizeSpec);
    assertThat(heightSpec).isEqualTo(SizeSpec.makeSizeSpec(0, UNSPECIFIED));

    final int widthSpec = gridLayoutInfo.getChildWidthSpec(sizeSpec);
    assertThat(widthSpec).isEqualTo(SizeSpec.makeSizeSpec(200 / 3, EXACTLY));
  }

  @Test
  public void testGetChildMeasureSpecHorizontal() {
    final GridLayoutInfo gridLayoutInfo = createGridLayoutInfo(HORIZONTAL, 3);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final int heightSpec = gridLayoutInfo.getChildHeightSpec(sizeSpec);
    assertThat(heightSpec).isEqualTo(SizeSpec.makeSizeSpec(200 / 3, EXACTLY));

    final int widthSpec = gridLayoutInfo.getChildWidthSpec(sizeSpec);
    assertThat(widthSpec).isEqualTo(SizeSpec.makeSizeSpec(0, UNSPECIFIED));
  }

  private static GridLayoutInfo createGridLayoutInfo(int direction,  int spanCount) {
    return new GridLayoutInfo(RuntimeEnvironment.application, spanCount, direction, false);
  }
}
