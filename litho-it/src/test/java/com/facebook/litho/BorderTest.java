/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaEdge;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class BorderTest {
  @Test
  public void testIndividualColorSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border =
        Border.create(c)
            .color(YogaEdge.LEFT, 0xFFFF0000)
            .color(YogaEdge.TOP, 0xFFFFFF00)
            .color(YogaEdge.RIGHT, 0xFFFFFFFF)
            .color(YogaEdge.BOTTOM, 0xFFFF00FF)
            .build();
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFFFFFF00);
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFFFFFFFF);
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFFFF00FF);
  }

  @Test
  public void testAllColorSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border = Border.create(c).color(YogaEdge.ALL, 0xFFFF0000).build();
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFFFF0000);
  }

  @Test
  public void testHorizontalColorSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border =
        Border.create(c)
            .color(YogaEdge.ALL, 0xFFFF0000)
            .color(YogaEdge.HORIZONTAL, 0xFF00FF00)
            .build();
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFF00FF00);
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFF00FF00);
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFFFF0000);
  }

  @Test
  public void testVerticalColorSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border =
        Border.create(c)
            .color(YogaEdge.ALL, 0xFFFF0000)
            .color(YogaEdge.VERTICAL, 0xFF00FF00)
            .build();
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFF00FF00);
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFF00FF00);
  }

  @Test
  public void testStartEndResolving() {
    final ComponentContext c = new ComponentContext(application);
    Border border =
        Border.create(c)
            .color(YogaEdge.START, 0xFFFF0000)
            .color(YogaEdge.END, 0x0000FFFF)
            .widthPx(YogaEdge.START, 100)
            .widthPx(YogaEdge.END, 200)
            .build();
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0x0000FFFF);
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(100);
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(200);
  }

  @Test
  public void testIndividualWidthSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border =
        Border.create(c)
            .widthPx(YogaEdge.LEFT, 1)
            .widthPx(YogaEdge.TOP, 2)
            .widthPx(YogaEdge.RIGHT, 3)
            .widthPx(YogaEdge.BOTTOM, 4)
            .build();
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(1);
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(2);
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(3);
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(4);
  }

  @Test
  public void testAllWidthSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border = Border.create(c).widthPx(YogaEdge.ALL, 5).build();
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(5);
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(5);
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(5);
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(5);
  }

  @Test
  public void testHorizontalWidthSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border =
        Border.create(c).widthPx(YogaEdge.ALL, 1).widthPx(YogaEdge.HORIZONTAL, 5).build();
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(5);
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(1);
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(5);
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(1);
  }

  @Test
  public void testVerticalWidthSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border = Border.create(c).widthPx(YogaEdge.ALL, 1).widthPx(YogaEdge.VERTICAL, 5).build();
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(1);
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(5);
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(1);
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(5);
  }

  @Test
  public void testAllColorWidthSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border =
        Border.create(c)
            .color(YogaEdge.LEFT, 0xFFFF0000)
            .color(YogaEdge.TOP, 0xFFFFFF00)
            .color(YogaEdge.RIGHT, 0xFFFFFFFF)
            .color(YogaEdge.BOTTOM, 0xFFFF00FF)
            .widthPx(YogaEdge.LEFT, 1)
            .widthPx(YogaEdge.TOP, 2)
            .widthPx(YogaEdge.RIGHT, 3)
            .widthPx(YogaEdge.BOTTOM, 4)
            .build();
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000);
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFFFFFF00);
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFFFFFFFF);
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFFFF00FF);

    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(1);
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(2);
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(3);
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(4);
  }

  @Test
  public void testEffectSetting() {
    final ComponentContext c = new ComponentContext(application);
    Border border = Border.create(c).dashEffect(new float[] {1f, 1f}, 0f).build();
    assertThat(border.mPathEffect).isInstanceOf(DashPathEffect.class);

    border = Border.create(c).discreteEffect(1f, 0f).build();
    assertThat(border.mPathEffect).isInstanceOf(DiscretePathEffect.class);

    border =
        Border.create(c)
            .pathDashEffect(new Path(), 0f, 0f, PathDashPathEffect.Style.ROTATE)
            .build();
    assertThat(border.mPathEffect).isInstanceOf(PathDashPathEffect.class);

    border = Border.create(c).discreteEffect(1f, 1f).dashEffect(new float[] {1f, 2f}, 1f).build();
    assertThat(border.mPathEffect).isInstanceOf(ComposePathEffect.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTooManyEffectsThrows() {
    final ComponentContext c = new ComponentContext(application);
    Border.create(c)
        .pathDashEffect(new Path(), 1f, 1f, PathDashPathEffect.Style.MORPH)
        .dashEffect(new float[] {1f, 2f}, 1f)
        .discreteEffect(1f, 2f)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDifferentWidthWithEffectThrows() {
    final ComponentContext c = new ComponentContext(application);
    Border.create(c)
        .widthPx(YogaEdge.ALL, 10)
        .widthPx(YogaEdge.LEFT, 5)
        .discreteEffect(1f, 1f)
        .build();
  }
}
