/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.utils;

import android.view.View.MeasureSpec;

import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static org.junit.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class MeasureUtilsTest {

  @Test
  public void testWidthExactlyHeightAtMost() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(10, EXACTLY),
        SizeSpec.makeSizeSpec(30, AT_MOST),
        0.5f,
        size);

    assertEquals(10, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testWidthExactlyHeightUnspecified() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(10, EXACTLY),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        0.5f,
        size);

    assertEquals(10, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testWidthAtMostHeightExactly() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(30, AT_MOST),
        SizeSpec.makeSizeSpec(10, EXACTLY),
        2f,
        size);

    assertEquals(20, size.width);
    assertEquals(10, size.height);
  }

  @Test
  public void testWidthUnspecifiedHeightExactly() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(10, EXACTLY),
        2f,
        size);

    assertEquals(20, size.width);
    assertEquals(10, size.height);
  }

  @Test
  public void testWidthAtMostHeightAtMostWidthSmaller() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(10, AT_MOST),
        SizeSpec.makeSizeSpec(20, AT_MOST),
        0.5f,
        size);

    assertEquals(10, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testWidthAtMostHeightAtMostHeightSmaller() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(20, AT_MOST),
        SizeSpec.makeSizeSpec(10, AT_MOST),
        2f,
        size);

    assertEquals(20, size.width);
    assertEquals(10, size.height);
  }

  @Test
  public void testWidthAtMostHeightUnspecified() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(20, AT_MOST),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        1f,
        size);

    assertEquals(20, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testWidthUnspecifiedHeightAtMost() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(20, AT_MOST),
        1f,
        size);

    assertEquals(20, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testWithInstrinsicSize() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(20, AT_MOST),
        10,
        10,
        1f,
        size);

    assertEquals(10, size.width);
    assertEquals(10, size.height);
  }

  @Test
  public void testWidthExactlyHeightAtMostEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(20, EXACTLY),
        SizeSpec.makeSizeSpec(20, AT_MOST),
        1,
        size);

    assertEquals(20, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testWidthAtMostHeightExactlyEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(20, AT_MOST),
        SizeSpec.makeSizeSpec(20, EXACTLY),
        1,
        size);

    assertEquals(20, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testWidthUnspecifiedHeightUnspecified() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        10,
        size);

    assertEquals(0, size.width);
    assertEquals(0, size.height);
  }

  @Test
  public void testWidthExactlyHeightTooSmall() {
    final Size size = new Size();
    MeasureUtils.measureWithAspectRatio(
        SizeSpec.makeSizeSpec(10, EXACTLY),
        SizeSpec.makeSizeSpec(20, AT_MOST),
        0.1f,
        size);

    assertEquals(10, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testWidthUnspecifiedHeightUnspecifiedEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        size);

    assertEquals(0, size.width);
    assertEquals(0, size.height);
  }

  @Test
  public void testWidthAtMostHeightAtMostEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(20, AT_MOST),
        SizeSpec.makeSizeSpec(10, AT_MOST),
        size);
    assertEquals(10, size.width);
    assertEquals(10, size.height);

    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(20, AT_MOST),
        SizeSpec.makeSizeSpec(30, AT_MOST),
        size);
    assertEquals(20, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void textAtMostUnspecifiedEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(20, AT_MOST),
        SizeSpec.makeSizeSpec(10, UNSPECIFIED),
        size);
    assertEquals(20, size.width);
    assertEquals(20, size.height);

    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(10, UNSPECIFIED),
        SizeSpec.makeSizeSpec(30, AT_MOST),
        size);
    assertEquals(30, size.width);
    assertEquals(30, size.height);
  }

  @Test
  public void testExactlyUnspecifiedEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(20, EXACTLY),
        SizeSpec.makeSizeSpec(10, UNSPECIFIED),
        size);
    assertEquals(20, size.width);
    assertEquals(20, size.height);

    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(20, UNSPECIFIED),
        SizeSpec.makeSizeSpec(10, EXACTLY),
        size);
    assertEquals(10, size.width);
    assertEquals(10, size.height);
  }

  @Test
  public void testExactlyAtMostSmallerEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(20, EXACTLY),
        SizeSpec.makeSizeSpec(10, AT_MOST),
        size);
    assertEquals(20, size.width);
    assertEquals(10, size.height);

    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(10, AT_MOST),
        SizeSpec.makeSizeSpec(20, EXACTLY),
        size);
    assertEquals(10, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testExactlyAtMostLargerEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(20, EXACTLY),
        SizeSpec.makeSizeSpec(30, AT_MOST),
        size);
    assertEquals(20, size.width);
    assertEquals(20, size.height);

    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(30, AT_MOST),
        SizeSpec.makeSizeSpec(20, EXACTLY),
        size);
    assertEquals(20, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void textExactWidthExactHeightEqual() {
    final Size size = new Size();
    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(20, EXACTLY),
        SizeSpec.makeSizeSpec(10, EXACTLY),
        size);
    assertEquals(20, size.width);
    assertEquals(10, size.height);

    MeasureUtils.measureWithEqualDimens(
        SizeSpec.makeSizeSpec(30, EXACTLY),
        SizeSpec.makeSizeSpec(30, EXACTLY),
        size);
    assertEquals(30, size.width);
    assertEquals(30, size.height);
  }

  @Test
  public void testGetViewMeasureSpecExactly() {
    assertEquals(
        MeasureUtils.getViewMeasureSpec(MeasureSpec.makeMeasureSpec(10, MeasureSpec.EXACTLY)),
        MeasureUtils.getViewMeasureSpec(SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY)));
  }

  @Test
  public void testGetViewMeasureSpecAtMost() {
    assertEquals(
        MeasureUtils.getViewMeasureSpec(MeasureSpec.makeMeasureSpec(10, MeasureSpec.AT_MOST)),
        MeasureUtils.getViewMeasureSpec(SizeSpec.makeSizeSpec(10, SizeSpec.AT_MOST)));
  }

  @Test
  public void testGetViewMeasureSpecUnspecified() {
    assertEquals(
        MeasureUtils.getViewMeasureSpec(MeasureSpec.makeMeasureSpec(10, MeasureSpec.UNSPECIFIED)),
        MeasureUtils.getViewMeasureSpec(SizeSpec.makeSizeSpec(10, SizeSpec.UNSPECIFIED)));
  }

  @Test
  public void testMeasureWithDesiredSizeAndExactlySpec() {
    final Size size = new Size();
    MeasureUtils.measureWithDesiredPx(
        SizeSpec.makeSizeSpec(50, EXACTLY),
        SizeSpec.makeSizeSpec(30, EXACTLY),
        80,
        20,
        size);
    assertEquals(50, size.width);
    assertEquals(30, size.height);
  }

  @Test
  public void testMeasureWithDesiredSizeAndLargerAtMostSpec() {
    final Size size = new Size();
    MeasureUtils.measureWithDesiredPx(
        SizeSpec.makeSizeSpec(81, AT_MOST),
        SizeSpec.makeSizeSpec(21, AT_MOST),
        80,
        20,
        size);
    assertEquals(80, size.width);
    assertEquals(20, size.height);
  }

  @Test
  public void testMeasureWithDesiredSizeAndSmallerAtMostSpec() {
    final Size size = new Size();
    MeasureUtils.measureWithDesiredPx(
        SizeSpec.makeSizeSpec(79, AT_MOST),
        SizeSpec.makeSizeSpec(19, EXACTLY),
        80,
        20,
        size);
    assertEquals(79, size.width);
    assertEquals(19, size.height);
  }

  @Test
  public void testMeasureWithDesiredSizeAndUnspecifiedSpec() {
    final Size size = new Size();
    MeasureUtils.measureWithDesiredPx(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        80,
        20,
        size);
    assertEquals(80, size.width);
    assertEquals(20, size.height);
  }
}
