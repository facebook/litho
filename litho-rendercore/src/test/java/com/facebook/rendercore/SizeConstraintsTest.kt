/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore

import android.view.View
import com.facebook.rendercore.utils.MeasureSpecUtils
import java.lang.IllegalArgumentException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SizeConstraintsTest {

  @Test
  fun `create -  exact size constraints - is successful`() {
    val c = SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(0)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(0)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isTrue
    assertThat(c.hasExactHeight).isTrue
    assertThat(c.isZeroSize).isTrue
    assertThat(Size(width = 0, height = 0).fitsWithin(c)).isTrue
    assertThat(Size(width = 1, height = 0).fitsWithin(c)).isFalse
    assertThat(Size(width = 0, height = 1).fitsWithin(c)).isFalse
    assertThat(Size(width = 1, height = 1).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - bounded size constraints - is successful`() {
    val c = SizeConstraints(minWidth = 10, maxWidth = 50, minHeight = 100, maxHeight = 300)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(10)
    assertThat(c.maxWidth).isEqualTo(50)
    assertThat(c.minHeight).isEqualTo(100)
    assertThat(c.maxHeight).isEqualTo(300)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 20, height = 200).fitsWithin(c)).isTrue
    assertThat(Size(width = 100, height = 200).fitsWithin(c)).isFalse
    assertThat(Size(width = 20, height = 400).fitsWithin(c)).isFalse
    assertThat(Size(width = 100, height = 400).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - unbounded width size constraints with zero minWidth - is successful`() {
    val c =
        SizeConstraints(
            minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 100, maxHeight = 300)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.minHeight).isEqualTo(100)
    assertThat(c.maxHeight).isEqualTo(300)
    assertThat(c.hasBoundedWidth).isFalse
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 100, height = 200).fitsWithin(c)).isTrue
    assertThat(Size(width = 100, height = 400).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - unbounded height size constraints with zero minHeight - is successful`() {
    val c =
        SizeConstraints(
            minWidth = 42, maxWidth = 84, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(42)
    assertThat(c.maxWidth).isEqualTo(84)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isFalse
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 50, height = 100).fitsWithin(c)).isTrue
    assertThat(Size(width = 100, height = 100).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - unbounded width size constraints with non-zero minWidth - is successful`() {
    val c =
        SizeConstraints(
            minWidth = 10, maxWidth = SizeConstraints.Infinity, minHeight = 100, maxHeight = 300)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(10)
    assertThat(c.maxWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.minHeight).isEqualTo(100)
    assertThat(c.maxHeight).isEqualTo(300)
    assertThat(c.hasBoundedWidth).isFalse
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 20, height = 200).fitsWithin(c)).isTrue
    assertThat(Size(width = 5, height = 200).fitsWithin(c)).isFalse
    assertThat(Size(width = 20, height = 400).fitsWithin(c)).isFalse
    assertThat(Size(width = 5, height = 400).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - unbounded height size constraints with non-zero minHeight - is successful`() {
    val c =
        SizeConstraints(
            minWidth = 42, maxWidth = 84, minHeight = 10, maxHeight = SizeConstraints.Infinity)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(42)
    assertThat(c.maxWidth).isEqualTo(84)
    assertThat(c.minHeight).isEqualTo(10)
    assertThat(c.maxHeight).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isFalse
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 50, height = 100).fitsWithin(c)).isTrue
    assertThat(Size(width = 30, height = 100).fitsWithin(c)).isFalse
    assertThat(Size(width = 50, height = 5).fitsWithin(c)).isFalse
    assertThat(Size(width = 30, height = 5).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - infinite size constraints - is successful`() {
    val c =
        SizeConstraints(
            minWidth = SizeConstraints.Infinity,
            maxWidth = SizeConstraints.Infinity,
            minHeight = SizeConstraints.Infinity,
            maxHeight = SizeConstraints.Infinity)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.maxWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.minHeight).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.maxHeight).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.hasBoundedWidth).isFalse
    assertThat(c.hasBoundedHeight).isFalse
    assertThat(c.hasExactWidth).isTrue
    assertThat(c.hasExactHeight).isTrue
    assertThat(c.isZeroSize).isFalse
    assertThat(
            Size(width = SizeConstraints.Infinity, height = SizeConstraints.Infinity).fitsWithin(c))
        .isTrue
    assertThat(Size(width = 100, height = 100).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - size constraints with negative minWidth - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = -1, maxWidth = 0, minHeight = 0, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minWidth must be >= 0")
  }

  @Test
  fun `create - size constraints with negative maxWidth - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = -1, minHeight = 0, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be >= minWidth")
  }

  @Test
  fun `create - size constraints with negative minHeight - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = -1, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minHeight must be >= 0")
  }

  @Test
  fun `create - size constraints with negative maxHeight - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = -1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be >= minHeight")
  }

  @Test
  fun `create - size constraints with maxWidth larger than MaxValue - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 70000, minHeight = 0, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be < 65535")
  }

  @Test
  fun `create - size constraints with maxHeight larger than MaxValue - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 70000)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be < 65535")
  }

  @Test
  fun `copy - exact size constraints - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(minWidth = 10, maxWidth = 10, minHeight = 10, maxHeight = 10)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(10)
    assertThat(c.maxWidth).isEqualTo(10)
    assertThat(c.minHeight).isEqualTo(10)
    assertThat(c.maxHeight).isEqualTo(10)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isTrue
    assertThat(c.hasExactHeight).isTrue
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 10, height = 10).fitsWithin(c)).isTrue
    assertThat(Size(width = 20, height = 10).fitsWithin(c)).isFalse
    assertThat(Size(width = 10, height = 20).fitsWithin(c)).isFalse
    assertThat(Size(width = 20, height = 20).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - bounded size constraints - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(minWidth = 10, maxWidth = 50, minHeight = 100, maxHeight = 300)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(10)
    assertThat(c.maxWidth).isEqualTo(50)
    assertThat(c.minHeight).isEqualTo(100)
    assertThat(c.maxHeight).isEqualTo(300)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 20, height = 200).fitsWithin(c)).isTrue
    assertThat(Size(width = 5, height = 200).fitsWithin(c)).isFalse
    assertThat(Size(width = 20, height = 400).fitsWithin(c)).isFalse
    assertThat(Size(width = 5, height = 400).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - unbounded width size constraints with zero minWidth - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(
                minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 100, maxHeight = 300)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.minHeight).isEqualTo(100)
    assertThat(c.maxHeight).isEqualTo(300)
    assertThat(c.hasBoundedWidth).isFalse
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 10, height = 200).fitsWithin(c)).isTrue
    assertThat(Size(width = 10, height = 400).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - unbounded height size constraints with zero minHeight - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(minWidth = 42, maxWidth = 84, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(42)
    assertThat(c.maxWidth).isEqualTo(84)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isFalse
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 50, height = 100).fitsWithin(c)).isTrue
    assertThat(Size(width = 30, height = 100).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - unbounded width size constraints with non-zero minWidth - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(
                minWidth = 10,
                maxWidth = SizeConstraints.Infinity,
                minHeight = 100,
                maxHeight = 300)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(10)
    assertThat(c.maxWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.minHeight).isEqualTo(100)
    assertThat(c.maxHeight).isEqualTo(300)
    assertThat(c.hasBoundedWidth).isFalse
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 20, height = 200).fitsWithin(c)).isTrue
    assertThat(Size(width = 5, height = 200).fitsWithin(c)).isFalse
    assertThat(Size(width = 20, height = 400).fitsWithin(c)).isFalse
    assertThat(Size(width = 5, height = 400).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - unbounded height size constraints with non-zero minHeight - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(
                minWidth = 42, maxWidth = 84, minHeight = 10, maxHeight = SizeConstraints.Infinity)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(42)
    assertThat(c.maxWidth).isEqualTo(84)
    assertThat(c.minHeight).isEqualTo(10)
    assertThat(c.maxHeight).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isFalse
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 50, height = 100).fitsWithin(c)).isTrue
    assertThat(Size(width = 30, height = 100).fitsWithin(c)).isFalse
    assertThat(Size(width = 50, height = 5).fitsWithin(c)).isFalse
    assertThat(Size(width = 30, height = 5).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - infinite size constraints - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(
                minWidth = SizeConstraints.Infinity,
                maxWidth = SizeConstraints.Infinity,
                minHeight = SizeConstraints.Infinity,
                maxHeight = SizeConstraints.Infinity)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.maxWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.minHeight).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.maxHeight).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.hasBoundedWidth).isFalse
    assertThat(c.hasBoundedHeight).isFalse
    assertThat(c.hasExactWidth).isTrue
    assertThat(c.hasExactHeight).isTrue
    assertThat(c.isZeroSize).isFalse
    assertThat(
            Size(width = SizeConstraints.Infinity, height = SizeConstraints.Infinity).fitsWithin(c))
        .isTrue
    assertThat(Size(width = 100, height = 100).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - size constraints with negative minWidth - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(minWidth = -1, maxWidth = 0, minHeight = 0, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minWidth must be >= 0")
  }

  @Test
  fun `copy - size constraints with negative maxWidth - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(minWidth = 0, maxWidth = -1, minHeight = 0, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be >= minWidth")
  }

  @Test
  fun `copy - size constraints with negative minHeight - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(minWidth = 0, maxWidth = 0, minHeight = -1, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minHeight must be >= 0")
  }

  @Test
  fun `copy - size constraints with negative maxHeight - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = -1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be >= minHeight")
  }

  @Test
  fun `copy - size constraints with maxWidth larger than MaxValue - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(minWidth = 0, maxWidth = 70000, minHeight = 0, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be < 65535")
  }

  @Test
  fun `copy - size constraints with maxHeight larger than MaxValue - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 70000)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be < 65535")
  }

  @Test
  fun `create - size constraints with exact width and exact height measure specs - is successful`() {
    val c =
        SizeConstraints.fromMeasureSpecs(MeasureSpecUtils.exactly(10), MeasureSpecUtils.exactly(20))
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(10)
    assertThat(c.maxWidth).isEqualTo(10)
    assertThat(c.minHeight).isEqualTo(20)
    assertThat(c.maxHeight).isEqualTo(20)
  }

  @Test
  fun `create - size constraints with at most width and unspecified height measure specs - is successful`() {
    val c =
        SizeConstraints.fromMeasureSpecs(
            MeasureSpecUtils.atMost(10), MeasureSpecUtils.unspecified())
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(10)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(SizeConstraints.Infinity)
  }

  @Test
  fun `create - size constraints with unspecified width and at most height measure specs - is successful`() {
    val c =
        SizeConstraints.fromMeasureSpecs(
            MeasureSpecUtils.unspecified(), MeasureSpecUtils.atMost(10))
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(10)
  }

  @Test
  fun `convert - size constraints to exact width and exact height measure specs - is successful`() {
    val c = SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
    val widthSpec = c.toWidthSpec()
    val heightSpec = c.toHeightSpec()
    assertThat(MeasureSpecUtils.getMode(widthSpec)).isEqualTo(View.MeasureSpec.EXACTLY)
    assertThat(MeasureSpecUtils.getSize(widthSpec)).isEqualTo(0)
    assertThat(MeasureSpecUtils.getMode(heightSpec)).isEqualTo(View.MeasureSpec.EXACTLY)
    assertThat(MeasureSpecUtils.getSize(heightSpec)).isEqualTo(0)
  }

  @Test
  fun `convert - size constraints to unspecified width and unspecified height measure specs - is successful`() {
    val c =
        SizeConstraints(
            minWidth = 0,
            maxWidth = SizeConstraints.Infinity,
            minHeight = 0,
            maxHeight = SizeConstraints.Infinity)
    val widthSpec = c.toWidthSpec()
    val heightSpec = c.toHeightSpec()
    assertThat(MeasureSpecUtils.getMode(widthSpec)).isEqualTo(View.MeasureSpec.UNSPECIFIED)
    assertThat(MeasureSpecUtils.getSize(widthSpec)).isEqualTo(0)
    assertThat(MeasureSpecUtils.getMode(heightSpec)).isEqualTo(View.MeasureSpec.UNSPECIFIED)
    assertThat(MeasureSpecUtils.getSize(heightSpec)).isEqualTo(0)
  }

  @Test
  fun `convert - size constraints to at most width and at most height measure specs - is successful`() {
    val c = SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 200)
    val widthSpec = c.toWidthSpec()
    val heightSpec = c.toHeightSpec()
    assertThat(MeasureSpecUtils.getMode(widthSpec)).isEqualTo(View.MeasureSpec.AT_MOST)
    assertThat(MeasureSpecUtils.getSize(widthSpec)).isEqualTo(100)
    assertThat(MeasureSpecUtils.getMode(heightSpec)).isEqualTo(View.MeasureSpec.AT_MOST)
    assertThat(MeasureSpecUtils.getSize(heightSpec)).isEqualTo(200)
  }

  @Test
  fun `convert - size constraints to at most width and at most height measure specs - is lossy`() {
    val c = SizeConstraints(minWidth = 50, maxWidth = 100, minHeight = 120, maxHeight = 200)
    val widthSpec = c.toWidthSpec()
    val heightSpec = c.toHeightSpec()
    assertThat(MeasureSpecUtils.getMode(widthSpec)).isEqualTo(View.MeasureSpec.AT_MOST)
    assertThat(MeasureSpecUtils.getSize(widthSpec)).isEqualTo(100)
    assertThat(MeasureSpecUtils.getMode(heightSpec)).isEqualTo(View.MeasureSpec.AT_MOST)
    assertThat(MeasureSpecUtils.getSize(heightSpec)).isEqualTo(200)
  }

  @Test
  fun `convert - size constraints to unspecified width and unspecified height measure specs - is lossy`() {
    val c =
        SizeConstraints(
            minWidth = 50,
            maxWidth = SizeConstraints.Infinity,
            minHeight = 120,
            maxHeight = SizeConstraints.Infinity)
    val widthSpec = c.toWidthSpec()
    val heightSpec = c.toHeightSpec()
    assertThat(MeasureSpecUtils.getMode(widthSpec)).isEqualTo(View.MeasureSpec.UNSPECIFIED)
    assertThat(MeasureSpecUtils.getSize(widthSpec)).isEqualTo(0)
    assertThat(MeasureSpecUtils.getMode(heightSpec)).isEqualTo(View.MeasureSpec.UNSPECIFIED)
    assertThat(MeasureSpecUtils.getSize(heightSpec)).isEqualTo(0)
  }

  @Test
  fun `subtract - one from infinite maxWidth - should throw`() {
    val constraints =
        SizeConstraints(
            minWidth = 0,
            maxWidth = SizeConstraints.Infinity,
            minHeight = 0,
            maxHeight = SizeConstraints.Infinity)
    assertThatThrownBy { constraints.copy(maxWidth = constraints.maxWidth - 1) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be < 65535")
  }

  @Test
  fun `add - one to infinite maxWidth - should throw`() {
    val constraints =
        SizeConstraints(
            minWidth = 0,
            maxWidth = SizeConstraints.Infinity,
            minHeight = 0,
            maxHeight = SizeConstraints.Infinity)
    assertThatThrownBy { constraints.copy(maxWidth = constraints.maxWidth + 1) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be < 65535")
  }

  @Test
  fun `subtract - one from infinite maxHeight - should throw`() {
    val constraints =
        SizeConstraints(
            minWidth = 0,
            maxWidth = SizeConstraints.Infinity,
            minHeight = 0,
            maxHeight = SizeConstraints.Infinity)
    assertThatThrownBy { constraints.copy(maxHeight = constraints.maxHeight - 1) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be < 65535")
  }

  @Test
  fun `add - one to infinite maxHeight - should throw`() {
    val constraints =
        SizeConstraints(
            minWidth = 0,
            maxWidth = SizeConstraints.Infinity,
            minHeight = 0,
            maxHeight = SizeConstraints.Infinity)
    assertThatThrownBy { constraints.copy(maxHeight = constraints.maxHeight + 1) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be < 65535")
  }
}
