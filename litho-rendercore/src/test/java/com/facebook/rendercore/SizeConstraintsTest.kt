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

  private val maxValue30Bits: Int = 0x3FFFFFFF - 1
  private val maxValue18Bits: Int = 0x3FFFF - 1
  private val maxValue13Bits: Int = 0x1FFF - 1

  @Test
  fun `create - exact size constraints - is successful`() {
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
  fun `create - exact size constraints with maximum supported values - is successful`() {
    val c =
        SizeConstraints(
            minWidth = maxValue30Bits,
            maxWidth = maxValue30Bits,
            minHeight = maxValue30Bits,
            maxHeight = maxValue30Bits)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(maxValue30Bits)
    assertThat(c.maxWidth).isEqualTo(maxValue30Bits)
    assertThat(c.minHeight).isEqualTo(maxValue30Bits)
    assertThat(c.maxHeight).isEqualTo(maxValue30Bits)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isTrue
    assertThat(c.hasExactHeight).isTrue
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = maxValue30Bits, height = maxValue30Bits).fitsWithin(c)).isTrue
    assertThat(Size(width = maxValue30Bits + 1, height = maxValue30Bits).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue30Bits, height = maxValue30Bits + 1).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue30Bits + 1, height = maxValue30Bits + 1).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - exact size constraints with width larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(
              minWidth = maxValue30Bits + 1,
              maxWidth = maxValue30Bits + 1,
              minHeight = maxValue30Bits,
              maxHeight = maxValue30Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minWidth must be <= 1073741822")
  }

  @Test
  fun `create - exact size constraints with height larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(
              minWidth = maxValue30Bits,
              maxWidth = maxValue30Bits,
              minHeight = maxValue30Bits + 1,
              maxHeight = maxValue30Bits + 1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minHeight must be <= 1073741822")
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
  fun `create - bounded size constraints with zero min values and maximum supported max values - is successful`() {
    val c =
        SizeConstraints(
            minWidth = 0, maxWidth = maxValue30Bits, minHeight = 0, maxHeight = maxValue30Bits)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(maxValue30Bits)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(maxValue30Bits)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 20, height = 200).fitsWithin(c)).isTrue
    assertThat(Size(width = maxValue30Bits + 1, height = 200).fitsWithin(c)).isFalse
    assertThat(Size(width = 20, height = maxValue30Bits + 1).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue30Bits + 1, height = maxValue30Bits + 1).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - bounded size constraints with zero min values and maxWidth larger than max supported value - is successful`() {
    assertThatThrownBy {
          SizeConstraints(
              minWidth = 0,
              maxWidth = maxValue30Bits + 1,
              minHeight = 0,
              maxHeight = maxValue30Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be <= 1073741822")
  }

  @Test
  fun `create - bounded size constraints with zero min values and maxHeight larger than max supported value - is successful`() {
    assertThatThrownBy {
          SizeConstraints(
              minWidth = 0,
              maxWidth = maxValue30Bits,
              minHeight = 0,
              maxHeight = maxValue30Bits + 1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be <= 1073741822")
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
  fun `create - range size constraints with maximum supported values - is successful`() {
    val c =
        SizeConstraints(
            minWidth = maxValue13Bits,
            maxWidth = maxValue18Bits,
            minHeight = maxValue13Bits,
            maxHeight = maxValue18Bits)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(maxValue13Bits)
    assertThat(c.maxWidth).isEqualTo(maxValue18Bits)
    assertThat(c.minHeight).isEqualTo(maxValue13Bits)
    assertThat(c.maxHeight).isEqualTo(maxValue18Bits)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = maxValue18Bits, height = maxValue18Bits).fitsWithin(c)).isTrue
    assertThat(Size(width = maxValue18Bits + 1, height = maxValue18Bits).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue18Bits, height = maxValue18Bits + 1).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue18Bits + 1, height = maxValue18Bits + 1).fitsWithin(c)).isFalse
  }

  @Test
  fun `create - range size constraints with minWidth larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(
              minWidth = maxValue13Bits + 1,
              maxWidth = maxValue18Bits,
              minHeight = maxValue13Bits,
              maxHeight = maxValue18Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minWidth must be <= 8190")
  }

  @Test
  fun `create - range size constraints with maxWidth larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(
              minWidth = maxValue13Bits,
              maxWidth = maxValue18Bits + 1,
              minHeight = maxValue13Bits,
              maxHeight = maxValue18Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be <= 262142")
  }

  @Test
  fun `create - range size constraints with minHeight larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(
              minWidth = maxValue13Bits,
              maxWidth = maxValue18Bits,
              minHeight = maxValue13Bits + 1,
              maxHeight = maxValue18Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minHeight must be <= 8190")
  }

  @Test
  fun `create - range size constraints with maxHeight larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(
              minWidth = maxValue13Bits,
              maxWidth = maxValue18Bits,
              minHeight = maxValue13Bits,
              maxHeight = maxValue18Bits + 1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be <= 262142")
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
          SizeConstraints(minWidth = 0, maxWidth = maxValue30Bits + 1, minHeight = 0, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be <= $maxValue30Bits")
  }

  @Test
  fun `create - size constraints with maxHeight larger than MaxValue - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = maxValue30Bits + 1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be <= $maxValue30Bits")
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
  fun `copy - exact size constraints with maximum supported values - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(
                minWidth = maxValue30Bits,
                maxWidth = maxValue30Bits,
                minHeight = maxValue30Bits,
                maxHeight = maxValue30Bits)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(maxValue30Bits)
    assertThat(c.maxWidth).isEqualTo(maxValue30Bits)
    assertThat(c.minHeight).isEqualTo(maxValue30Bits)
    assertThat(c.maxHeight).isEqualTo(maxValue30Bits)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isTrue
    assertThat(c.hasExactHeight).isTrue
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = maxValue30Bits, height = maxValue30Bits).fitsWithin(c)).isTrue
    assertThat(Size(width = maxValue30Bits + 1, height = maxValue30Bits).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue30Bits, height = maxValue30Bits + 1).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue30Bits + 1, height = maxValue30Bits + 1).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - exact size constraints with width larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(
                  minWidth = maxValue30Bits + 1,
                  maxWidth = maxValue30Bits + 1,
                  minHeight = maxValue30Bits,
                  maxHeight = maxValue30Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minWidth must be <= 1073741822")
  }

  @Test
  fun `copy - exact size constraints with height larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(
                  minWidth = maxValue30Bits,
                  maxWidth = maxValue30Bits,
                  minHeight = maxValue30Bits + 1,
                  maxHeight = maxValue30Bits + 1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minHeight must be <= 1073741822")
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
  fun `copy - bounded size constraints with zero min values and maximum supported max values - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(
                minWidth = 0, maxWidth = maxValue30Bits, minHeight = 0, maxHeight = maxValue30Bits)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(maxValue30Bits)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(maxValue30Bits)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = 20, height = 200).fitsWithin(c)).isTrue
    assertThat(Size(width = maxValue30Bits + 1, height = 200).fitsWithin(c)).isFalse
    assertThat(Size(width = 20, height = maxValue30Bits + 1).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue30Bits + 1, height = maxValue30Bits + 1).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - bounded size constraints with zero min values and maxWidth larger than max supported value - is successful`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(
                  minWidth = 0,
                  maxWidth = maxValue30Bits + 1,
                  minHeight = 0,
                  maxHeight = maxValue30Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be <= 1073741822")
  }

  @Test
  fun `copy - bounded size constraints with zero min values and maxHeight larger than max supported value - is successful`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(
                  minWidth = 0,
                  maxWidth = maxValue30Bits,
                  minHeight = 0,
                  maxHeight = maxValue30Bits + 1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be <= 1073741822")
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
  fun `copy - range size constraints with maximum supported values - is successful`() {
    val c =
        SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
            .copy(
                minWidth = maxValue13Bits,
                maxWidth = maxValue18Bits,
                minHeight = maxValue13Bits,
                maxHeight = maxValue18Bits)
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(maxValue13Bits)
    assertThat(c.maxWidth).isEqualTo(maxValue18Bits)
    assertThat(c.minHeight).isEqualTo(maxValue13Bits)
    assertThat(c.maxHeight).isEqualTo(maxValue18Bits)
    assertThat(c.hasBoundedWidth).isTrue
    assertThat(c.hasBoundedHeight).isTrue
    assertThat(c.hasExactWidth).isFalse
    assertThat(c.hasExactHeight).isFalse
    assertThat(c.isZeroSize).isFalse
    assertThat(Size(width = maxValue18Bits, height = maxValue18Bits).fitsWithin(c)).isTrue
    assertThat(Size(width = maxValue18Bits + 1, height = maxValue18Bits).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue18Bits, height = maxValue18Bits + 1).fitsWithin(c)).isFalse
    assertThat(Size(width = maxValue18Bits + 1, height = maxValue18Bits + 1).fitsWithin(c)).isFalse
  }

  @Test
  fun `copy - range size constraints with minWidth larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(
                  minWidth = maxValue13Bits + 1,
                  maxWidth = maxValue18Bits,
                  minHeight = maxValue13Bits,
                  maxHeight = maxValue18Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minWidth must be <= 8190")
  }

  @Test
  fun `copy - range size constraints with maxWidth larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(
                  minWidth = maxValue13Bits,
                  maxWidth = maxValue18Bits + 1,
                  minHeight = maxValue13Bits,
                  maxHeight = maxValue18Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be <= 262142")
  }

  @Test
  fun `copy - range size constraints with minHeight larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(
                  minWidth = maxValue13Bits,
                  maxWidth = maxValue18Bits,
                  minHeight = maxValue13Bits + 1,
                  maxHeight = maxValue18Bits)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("minHeight must be <= 8190")
  }

  @Test
  fun `copy - range size constraints with maxHeight larger than max supported value - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(
                  minWidth = maxValue13Bits,
                  maxWidth = maxValue18Bits,
                  minHeight = maxValue13Bits,
                  maxHeight = maxValue18Bits + 1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be <= 262142")
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
              .copy(minWidth = 0, maxWidth = maxValue30Bits + 1, minHeight = 0, maxHeight = 0)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxWidth must be <= $maxValue30Bits")
  }

  @Test
  fun `copy - size constraints with maxHeight larger than MaxValue - should throw`() {
    assertThatThrownBy {
          SizeConstraints(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = 0)
              .copy(minWidth = 0, maxWidth = 0, minHeight = 0, maxHeight = maxValue30Bits + 1)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("maxHeight must be <= $maxValue30Bits")
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
  fun `create - size constraints with exact width and exact height measure specs with max supported values - is successful`() {
    val c =
        SizeConstraints.fromMeasureSpecs(
            MeasureSpecUtils.exactly(maxValue30Bits), MeasureSpecUtils.exactly(maxValue30Bits))
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(maxValue30Bits)
    assertThat(c.maxWidth).isEqualTo(maxValue30Bits)
    assertThat(c.minHeight).isEqualTo(maxValue30Bits)
    assertThat(c.maxHeight).isEqualTo(maxValue30Bits)
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
  fun `create - size constraints with at most max supported value width and unspecified height measure specs - is successful`() {
    val c =
        SizeConstraints.fromMeasureSpecs(
            MeasureSpecUtils.atMost(maxValue30Bits), MeasureSpecUtils.unspecified())
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(maxValue30Bits)
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
  fun `create - size constraints with unspecified width and at most max supported value height measure specs - is successful`() {
    val c =
        SizeConstraints.fromMeasureSpecs(
            MeasureSpecUtils.unspecified(), MeasureSpecUtils.atMost(maxValue30Bits))
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(SizeConstraints.Infinity)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(maxValue30Bits)
  }

  @Test
  fun `create - size constraints with at most max supported value width and at most max supported value height measure specs - is successful`() {
    val c =
        SizeConstraints.fromMeasureSpecs(
            MeasureSpecUtils.atMost(maxValue30Bits), MeasureSpecUtils.atMost(maxValue30Bits))
    assertThat(c).isNotNull
    assertThat(c.minWidth).isEqualTo(0)
    assertThat(c.maxWidth).isEqualTo(maxValue30Bits)
    assertThat(c.minHeight).isEqualTo(0)
    assertThat(c.maxHeight).isEqualTo(maxValue30Bits)
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
        .hasMessageContaining("maxWidth must be <= $maxValue30Bits")
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
        .hasMessageContaining("maxWidth must be >= minWidth")
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
        .hasMessageContaining("maxHeight must be <= $maxValue30Bits")
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
        .hasMessageContaining("maxHeight must be >= minHeight")
  }

  @Test
  fun `compatibility - same constraints - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, old and new widths are exact min and max width values are the same and size width is the same as new max width - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, old and new widths are exact min and max width values are different and size width is the same as new max width - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 20, maxWidth = 20, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, old width is not exact and new width is exact and size width is the same as new max width - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, old width is not exact and new width is exact and size width is different than new max width - are not compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val size = Size(width = 5, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - height fits, old and new max widths are unbounded and size width fits min width - are compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, old and new max widths are unbounded and size width doesn't fit min width - are not compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(
            minWidth = 20, maxWidth = SizeConstraints.Infinity, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - height fits, new width is exact and matches size width - are compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, old width is unbounded, new width is bounded and size width fits - are compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 5, maxWidth = 30, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, old width is unbounded, new width is bounded and size width doesn't fit min width - are not compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 15, maxWidth = 30, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - height fits, old width is unbounded, new width is bounded and size width doesn't fit max width - are not compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = SizeConstraints.Infinity, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 5, maxWidth = 10, minHeight = 0, maxHeight = 100)
    val size = Size(width = 15, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - height fits, widths are bounded, new max width is stricter and size width fits - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 5, maxWidth = 20, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 5, maxWidth = 15, minHeight = 0, maxHeight = 100)
    val size = Size(width = 10, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, widths are bounded, new min width is stricter and size width fits - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 5, maxWidth = 20, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 20, minHeight = 0, maxHeight = 100)
    val size = Size(width = 15, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, widths are bounded, new min and max widths are stricter and size width fits - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 5, maxWidth = 20, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 15, minHeight = 0, maxHeight = 100)
    val size = Size(width = 15, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - height fits, widths are bounded, new min and max widths are stricter and size width doesn't fit min width - are not compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 5, maxWidth = 20, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 15, minHeight = 0, maxHeight = 100)
    val size = Size(width = 5, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - height fits, widths are bounded, new min and max widths are stricter and size width doesn't fit max width - are not compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 5, maxWidth = 20, minHeight = 0, maxHeight = 100)
    val newConstraints =
        SizeConstraints(minWidth = 10, maxWidth = 15, minHeight = 0, maxHeight = 100)
    val size = Size(width = 20, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - width fits, old and new heights are exact min and max height values are the same and size height is the same as new max width - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 10)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 10)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, old and new heights are exact min and max height values are different and size height is the same as new max width - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 20, maxHeight = 20)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 10)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, old height is not exact and new height is exact and size height is the same as new max height - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 10)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 10)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, old height is not exact and new height is exact and size height is different than new max height - are not compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 10)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 10)
    val size = Size(width = 20, height = 5)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - width fits, old and new max heights are unbounded and size height fits min height - are compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    val newConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, old and new max heights are unbounded and size height doesn't fit min height - are not compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    val newConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = 100, minHeight = 20, maxHeight = SizeConstraints.Infinity)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - width fits, new height is exact and matches size height - are compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 10)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, old height is unbounded, new height is bounded and size height fits - are compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 5, maxHeight = 30)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, old height is unbounded, new height is bounded and size height doesn't fit min height - are not compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 15, maxHeight = 30)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - width fits, old height is unbounded, new height is bounded and size height doesn't fit max height - are not compatible`() {
    val oldConstraints =
        SizeConstraints(
            minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = SizeConstraints.Infinity)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 5, maxHeight = 10)
    val size = Size(width = 20, height = 15)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - width fits, heights are bounded, new max height is stricter and size height fits - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 5, maxHeight = 20)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 5, maxHeight = 15)
    val size = Size(width = 20, height = 10)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, heights are bounded, new min height is stricter and size height fits - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 5, maxHeight = 20)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 20)
    val size = Size(width = 20, height = 15)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, heights are bounded, new min and max heights are stricter and size height fits - are compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 5, maxHeight = 20)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 15)
    val size = Size(width = 20, height = 15)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isTrue
  }

  @Test
  fun `compatibility - width fits, heights are bounded, new min and max heights are stricter and size height doesn't fit min height - are not compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 5, maxHeight = 20)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 15)
    val size = Size(width = 20, height = 5)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }

  @Test
  fun `compatibility - width fits, heights are bounded, new min and max heights are stricter and size height doesn't fit max height - are not compatible`() {
    val oldConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 5, maxHeight = 20)
    val newConstraints =
        SizeConstraints(minWidth = 0, maxWidth = 100, minHeight = 10, maxHeight = 15)
    val size = Size(width = 20, height = 20)
    assertThat(newConstraints.areCompatible(oldConstraints, size)).isFalse
  }
}
