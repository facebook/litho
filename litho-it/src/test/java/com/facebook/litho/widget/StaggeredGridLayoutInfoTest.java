/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.support.v7.widget.StaggeredGridLayoutManager;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class StaggeredGridLayoutInfoTest {

  @Test
  public void testOrientation() {
    final StaggeredGridLayoutInfo verticalStaggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(VERTICAL, 2);

    assertThat(verticalStaggeredGridLayoutInfo.getScrollDirection()).isEqualTo(VERTICAL);

    final StaggeredGridLayoutInfo horizontalStaggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(HORIZONTAL, 2);

    assertThat(horizontalStaggeredGridLayoutInfo.getScrollDirection()).isEqualTo(HORIZONTAL);
  }

  @Test
  public void testLayoutManagerIsStaggeredGrid() {
    final StaggeredGridLayoutInfo staggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(VERTICAL, 2);

    assertThat(staggeredGridLayoutInfo.getLayoutManager())
        .isInstanceOf(StaggeredGridLayoutManager.class);
  }

  @Test
  public void testApproximateRangeVertical() {
    final StaggeredGridLayoutInfo staggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(VERTICAL, 3);
    int rangeSize = staggeredGridLayoutInfo.approximateRangeSize(10, 10, 30, 100);

    assertThat(rangeSize).isEqualTo(30);
  }

  @Test
  public void testApproximateRangeHorizontal() {
    final StaggeredGridLayoutInfo staggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(HORIZONTAL, 2);
    int rangeSize = staggeredGridLayoutInfo.approximateRangeSize(15, 10, 100, 20);

    assertThat(rangeSize).isEqualTo(14);
  }

  @Test
  public void testGetChildMeasureSpecVertical() {
    final StaggeredGridLayoutInfo staggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(VERTICAL, 3);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final RenderInfo renderInfo = mock(RenderInfo.class);
    when(renderInfo.getSpanSize()).thenReturn(1);

    final int heightSpec = staggeredGridLayoutInfo.getChildHeightSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getMode(heightSpec)).isEqualTo(UNSPECIFIED);

    final int widthSpec = staggeredGridLayoutInfo.getChildWidthSpec(sizeSpec, renderInfo);

    assertThat(SizeSpec.getSize(widthSpec)).isEqualTo((200 / 3) * 1);
    assertThat(SizeSpec.getMode(widthSpec)).isEqualTo(EXACTLY);
  }

  @Test
  public void testGetChildMeasureSpecVerticalWithFullSpan() {
    final StaggeredGridLayoutInfo staggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(VERTICAL, 3);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final RenderInfo renderInfo = mock(RenderInfo.class);
    when(renderInfo.isFullSpan()).thenReturn(true);

    final int heightSpec = staggeredGridLayoutInfo.getChildHeightSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getMode(heightSpec)).isEqualTo(UNSPECIFIED);

    final int widthSpec = staggeredGridLayoutInfo.getChildWidthSpec(sizeSpec, renderInfo);

    assertThat(SizeSpec.getSize(widthSpec)).isEqualTo((200 / 3) * 3);
    assertThat(SizeSpec.getMode(widthSpec)).isEqualTo(EXACTLY);
  }

  @Test
  public void testGetChildMeasureSpecHorizontal() {
    final StaggeredGridLayoutInfo staggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(HORIZONTAL, 3);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final RenderInfo renderInfo = mock(RenderInfo.class);
    when(renderInfo.getSpanSize()).thenReturn(1);

    final int heightSpec = staggeredGridLayoutInfo.getChildHeightSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getSize(heightSpec)).isEqualTo((200 / 3) * 1);
    assertThat(SizeSpec.getMode(heightSpec)).isEqualTo(EXACTLY);

    final int widthSpec = staggeredGridLayoutInfo.getChildWidthSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getMode(widthSpec)).isEqualTo(UNSPECIFIED);
  }

  @Test
  public void testGetChildMeasureSpecHorizontalWithFullSpan() {
    final StaggeredGridLayoutInfo staggeredGridLayoutInfo =
        createStaggeredGridLayoutInfo(HORIZONTAL, 3);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final RenderInfo renderInfo = mock(RenderInfo.class);
    when(renderInfo.isFullSpan()).thenReturn(true);

    final int heightSpec = staggeredGridLayoutInfo.getChildHeightSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getSize(heightSpec)).isEqualTo((200 / 3) * 3);
    assertThat(SizeSpec.getMode(heightSpec)).isEqualTo(EXACTLY);

    final int widthSpec = staggeredGridLayoutInfo.getChildWidthSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getMode(widthSpec)).isEqualTo(UNSPECIFIED);
  }

  private static StaggeredGridLayoutInfo createStaggeredGridLayoutInfo(
      int direction, int spanCount) {
    return new StaggeredGridLayoutInfo(
        spanCount, direction, false, StaggeredGridLayoutManager.GAP_HANDLING_NONE);
  }
}
