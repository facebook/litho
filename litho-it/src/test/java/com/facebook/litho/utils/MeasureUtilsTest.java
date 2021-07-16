/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.utils;

import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.utils.MeasureUtils.getViewMeasureSpec;
import static com.facebook.litho.utils.MeasureUtils.measureWithAspectRatio;
import static com.facebook.litho.utils.MeasureUtils.measureWithDesiredPx;
import static com.facebook.litho.utils.MeasureUtils.measureWithEqualDimens;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.View.MeasureSpec;
import com.facebook.litho.Size;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MeasureUtilsTest {

  @Test
  public void testWidthExactlyHeightAtMost() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(10, EXACTLY), makeSizeSpec(30, AT_MOST), 0.5f, size);

    assertThat(size.width).isEqualTo(10);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testWidthExactlyHeightUnspecified() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(10, EXACTLY), makeSizeSpec(0, UNSPECIFIED), 0.5f, size);

    assertThat(size.width).isEqualTo(10);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testWidthAtMostHeightExactly() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(30, AT_MOST), makeSizeSpec(10, EXACTLY), 2f, size);

    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(10);
  }

  @Test
  public void testWidthUnspecifiedHeightExactly() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(10, EXACTLY), 2f, size);

    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(10);
  }

  @Test
  public void testWidthAtMostHeightAtMostWidthSmaller() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(10, AT_MOST), makeSizeSpec(20, AT_MOST), 0.5f, size);

    assertThat(size.width).isEqualTo(10);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testWidthAtMostHeightAtMostHeightSmaller() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(20, AT_MOST), makeSizeSpec(10, AT_MOST), 2f, size);

    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(10);
  }

  @Test
  public void testWidthAtMostHeightUnspecified() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(20, AT_MOST), makeSizeSpec(0, UNSPECIFIED), 1f, size);

    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testWidthUnspecifiedHeightAtMost() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(20, AT_MOST), 1f, size);

    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testWithInstrinsicSize() {
    final Size size = new Size();
    measureWithAspectRatio(
        makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(20, AT_MOST), 10, 10, 1f, size);

    assertThat(size.width).isEqualTo(10);
    assertThat(size.height).isEqualTo(10);
  }

  @Test
  public void testWidthExactlyHeightAtMostEqual() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(20, EXACTLY), makeSizeSpec(20, AT_MOST), 1, size);

    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testWidthAtMostHeightExactlyEqual() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(20, AT_MOST), makeSizeSpec(20, EXACTLY), 1, size);

    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testWidthUnspecifiedHeightUnspecified() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED), 10, size);

    assertThat(size.width).isEqualTo(0);
    assertThat(size.height).isEqualTo(0);
  }

  @Test
  public void testWidthExactlyHeightTooSmall() {
    final Size size = new Size();
    measureWithAspectRatio(makeSizeSpec(10, EXACTLY), makeSizeSpec(20, AT_MOST), 0.1f, size);

    assertThat(size.width).isEqualTo(10);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testWidthUnspecifiedHeightUnspecifiedEqual() {
    final Size size = new Size();
    measureWithEqualDimens(makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED), size);

    assertThat(size.width).isEqualTo(0);
    assertThat(size.height).isEqualTo(0);
  }

  @Test
  public void testWidthAtMostHeightAtMostEqual() {
    final Size size = new Size();
    measureWithEqualDimens(makeSizeSpec(20, AT_MOST), makeSizeSpec(10, AT_MOST), size);
    assertThat(size.width).isEqualTo(10);
    assertThat(size.height).isEqualTo(10);

    measureWithEqualDimens(makeSizeSpec(20, AT_MOST), makeSizeSpec(30, AT_MOST), size);
    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void textAtMostUnspecifiedEqual() {
    final Size size = new Size();
    measureWithEqualDimens(makeSizeSpec(20, AT_MOST), makeSizeSpec(10, UNSPECIFIED), size);
    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);

    measureWithEqualDimens(makeSizeSpec(10, UNSPECIFIED), makeSizeSpec(30, AT_MOST), size);
    assertThat(size.width).isEqualTo(30);
    assertThat(size.height).isEqualTo(30);
  }

  @Test
  public void testExactlyUnspecifiedEqual() {
    final Size size = new Size();
    measureWithEqualDimens(makeSizeSpec(20, EXACTLY), makeSizeSpec(10, UNSPECIFIED), size);
    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);

    measureWithEqualDimens(makeSizeSpec(20, UNSPECIFIED), makeSizeSpec(10, EXACTLY), size);
    assertThat(size.width).isEqualTo(10);
    assertThat(size.height).isEqualTo(10);
  }

  @Test
  public void testExactlyAtMostSmallerEqual() {
    final Size size = new Size();
    measureWithEqualDimens(makeSizeSpec(20, EXACTLY), makeSizeSpec(10, AT_MOST), size);
    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(10);

    measureWithEqualDimens(makeSizeSpec(10, AT_MOST), makeSizeSpec(20, EXACTLY), size);
    assertThat(size.width).isEqualTo(10);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testExactlyAtMostLargerEqual() {
    final Size size = new Size();
    measureWithEqualDimens(makeSizeSpec(20, EXACTLY), makeSizeSpec(30, AT_MOST), size);
    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);

    measureWithEqualDimens(makeSizeSpec(30, AT_MOST), makeSizeSpec(20, EXACTLY), size);
    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void textExactWidthExactHeightEqual() {
    final Size size = new Size();
    measureWithEqualDimens(makeSizeSpec(20, EXACTLY), makeSizeSpec(10, EXACTLY), size);
    assertThat(size.width).isEqualTo(20);
    assertThat(size.height).isEqualTo(10);

    measureWithEqualDimens(makeSizeSpec(30, EXACTLY), makeSizeSpec(30, EXACTLY), size);
    assertThat(size.width).isEqualTo(30);
    assertThat(size.height).isEqualTo(30);
  }

  @Test
  public void testGetViewMeasureSpecExactly() {
    assertThat(getViewMeasureSpec(makeSizeSpec(10, EXACTLY)))
        .isEqualTo(getViewMeasureSpec(makeMeasureSpec(10, MeasureSpec.EXACTLY)));
  }

  @Test
  public void testGetViewMeasureSpecAtMost() {
    assertThat(getViewMeasureSpec(makeSizeSpec(10, AT_MOST)))
        .isEqualTo(getViewMeasureSpec(makeMeasureSpec(10, MeasureSpec.AT_MOST)));
  }

  @Test
  public void testGetViewMeasureSpecUnspecified() {
    assertThat(getViewMeasureSpec(makeSizeSpec(10, UNSPECIFIED)))
        .isEqualTo(getViewMeasureSpec(makeMeasureSpec(10, MeasureSpec.UNSPECIFIED)));
  }

  @Test
  public void testMeasureWithDesiredSizeAndExactlySpec() {
    final Size size = new Size();
    measureWithDesiredPx(makeSizeSpec(50, EXACTLY), makeSizeSpec(30, EXACTLY), 80, 20, size);
    assertThat(size.width).isEqualTo(50);
    assertThat(size.height).isEqualTo(30);
  }

  @Test
  public void testMeasureWithDesiredSizeAndLargerAtMostSpec() {
    final Size size = new Size();
    measureWithDesiredPx(makeSizeSpec(81, AT_MOST), makeSizeSpec(21, AT_MOST), 80, 20, size);
    assertThat(size.width).isEqualTo(80);
    assertThat(size.height).isEqualTo(20);
  }

  @Test
  public void testMeasureWithDesiredSizeAndSmallerAtMostSpec() {
    final Size size = new Size();
    measureWithDesiredPx(makeSizeSpec(79, AT_MOST), makeSizeSpec(19, EXACTLY), 80, 20, size);
    assertThat(size.width).isEqualTo(79);
    assertThat(size.height).isEqualTo(19);
  }

  @Test
  public void testMeasureWithDesiredSizeAndUnspecifiedSpec() {
    final Size size = new Size();
    measureWithDesiredPx(makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED), 80, 20, size);
    assertThat(size.width).isEqualTo(80);
    assertThat(size.height).isEqualTo(20);
  }
}
