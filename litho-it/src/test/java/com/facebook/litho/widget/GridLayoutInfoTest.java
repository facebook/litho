/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.support.v7.widget.GridLayoutManager;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

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

    assertThat(gridLayoutInfo.getLayoutManager()).isInstanceOf(GridLayoutManager.class);
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

    final RenderInfo renderInfo = mock(RenderInfo.class);
    when(renderInfo.getSpanSize()).thenReturn(2);

    final int heightSpec = gridLayoutInfo.getChildHeightSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getMode(heightSpec)).isEqualTo(UNSPECIFIED);

    final int widthSpec = gridLayoutInfo.getChildWidthSpec(sizeSpec, renderInfo);

    assertThat(SizeSpec.getSize(widthSpec)).isEqualTo((200 / 3) * 2);
    assertThat(SizeSpec.getMode(widthSpec)).isEqualTo(EXACTLY);
  }

  @Test
  public void testGetChildMeasureSpecOverride() {
    final GridLayoutInfo gridLayoutInfo = createGridLayoutInfo(VERTICAL, 3);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final RenderInfo renderInfo = mock(RenderInfo.class);
    when(renderInfo.getSpanSize()).thenReturn(2);
    when(renderInfo.getCustomAttribute(GridLayoutInfo.OVERRIDE_SIZE)).thenReturn(20);

    final int heightSpec = gridLayoutInfo.getChildHeightSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getMode(heightSpec)).isEqualTo(UNSPECIFIED);

    final int widthSpec = gridLayoutInfo.getChildWidthSpec(sizeSpec, renderInfo);

    assertThat(SizeSpec.getSize(widthSpec)).isEqualTo(20);
    assertThat(SizeSpec.getMode(widthSpec)).isEqualTo(EXACTLY);
  }

  @Test
  public void testGetChildMeasureSpecHorizontal() {
    final GridLayoutInfo gridLayoutInfo = createGridLayoutInfo(HORIZONTAL, 3);
    final int sizeSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final RenderInfo renderInfo = mock(RenderInfo.class);
    when(renderInfo.getSpanSize()).thenReturn(2);

    final int heightSpec = gridLayoutInfo.getChildHeightSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getSize(heightSpec)).isEqualTo((200 / 3) * 2);
    assertThat(SizeSpec.getMode(heightSpec)).isEqualTo(EXACTLY);

    final int widthSpec = gridLayoutInfo.getChildWidthSpec(sizeSpec, renderInfo);
    assertThat(SizeSpec.getMode(widthSpec)).isEqualTo(UNSPECIFIED);
  }

  private static GridLayoutInfo createGridLayoutInfo(int direction, int spanCount) {
    return new GridLayoutInfo(RuntimeEnvironment.application, spanCount, direction, false);
  }
}
