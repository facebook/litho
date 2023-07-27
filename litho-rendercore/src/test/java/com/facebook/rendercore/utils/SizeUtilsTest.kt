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

package com.facebook.rendercore.utils

import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.getOrElse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SizeUtilsTest {

  @Test
  fun `exact size - with exact width and exact height - returns exact size`() {
    assertThat(
            Size.exact(
                SizeConstraints(minWidth = 10, maxWidth = 10, minHeight = 10, maxHeight = 10)))
        .isEqualTo(Size(width = 10, height = 10))
  }

  @Test
  fun `exact size - with exact width and unbounded height - returns Invalid size`() {
    assertThat(
            Size.exact(
                SizeConstraints(
                    minWidth = 10,
                    maxWidth = 10,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity)))
        .isEqualTo(Size.Invalid)
  }

  @Test
  fun `exact size - with exact width and bounded, non-exact height - returns Invalid size`() {
    assertThat(
            Size.exact(
                SizeConstraints(minWidth = 10, maxWidth = 10, minHeight = 10, maxHeight = 20)))
        .isEqualTo(Size.Invalid)
  }

  @Test
  fun `exact size - with unbounded width and exact height - returns Invalid size`() {
    assertThat(
            Size.exact(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 10,
                    maxHeight = 10)))
        .isEqualTo(Size.Invalid)
  }

  @Test
  fun `exact size - with bounded, non-exact width and exact height - returns Invalid size`() {
    assertThat(
            Size.exact(
                SizeConstraints(minWidth = 10, maxWidth = 20, minHeight = 10, maxHeight = 10)))
        .isEqualTo(Size.Invalid)
  }

  @Test
  fun `fill space - with bounded, non-exact width and bounded, non-exact height - returns size with maxWidth and maxHeight`() {
    assertThat(
            Size.fillSpace(
                SizeConstraints(minWidth = 10, maxWidth = 20, minHeight = 30, maxHeight = 40),
                fallbackWidth = 100,
                fallbackHeight = 200))
        .isEqualTo(Size(width = 20, height = 40))
  }

  @Test
  fun `fill space - with unbounded width and bounded, non-exact height - returns size with fallbackWidth and maxHeight`() {
    assertThat(
            Size.fillSpace(
                SizeConstraints(
                    minWidth = 10,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 30,
                    maxHeight = 40),
                fallbackWidth = 100,
                fallbackHeight = 200))
        .isEqualTo(Size(width = 100, height = 40))
  }

  @Test
  fun `fill space - with bounded, non-exact width and unbounded height - returns size with maxWidth and fallbackHeight`() {
    assertThat(
            Size.fillSpace(
                SizeConstraints(
                    minWidth = 10,
                    maxWidth = 20,
                    minHeight = 30,
                    maxHeight = SizeConstraints.Infinity),
                fallbackWidth = 100,
                fallbackHeight = 200))
        .isEqualTo(Size(width = 20, height = 200))
  }

  @Test
  fun `fill space - with unbounded width and unbounded height - returns size with fallbackWidth and fallbackHeight`() {
    assertThat(
            Size.fillSpace(
                SizeConstraints(
                    minWidth = 10,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 30,
                    maxHeight = SizeConstraints.Infinity),
                fallbackWidth = 100,
                fallbackHeight = 200))
        .isEqualTo(Size(width = 100, height = 200))
  }

  @Test
  fun `fill space - with unbounded width and unbounded height and min values larger than fallback values - returns size with minWidth and minHeight`() {
    assertThat(
            Size.fillSpace(
                SizeConstraints(
                    minWidth = 1000,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 2000,
                    maxHeight = SizeConstraints.Infinity),
                fallbackWidth = 100,
                fallbackHeight = 200))
        .isEqualTo(Size(width = 1000, height = 2000))
  }

  @Test
  fun `aspect ratio - with bounded width and bounded height - returns size with maxWidth and correct height`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(minWidth = 0, maxWidth = 30, minHeight = 0, maxHeight = 40),
                aspectRatio = 2f))
        .isEqualTo(Size(width = 30, height = 15))
  }

  @Test
  fun `aspect ratio - with bounded width, bounded height, and intrinsic size - returns size with intrinsicWidth and correct height`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(minWidth = 0, maxWidth = 30, minHeight = 0, maxHeight = 40),
                aspectRatio = 2f,
                intrinsicWidth = 6,
                intrinsicHeight = 8))
        .isEqualTo(Size(width = 6, height = 3))
  }

  @Test
  fun `aspect ratio - with exact width and bounded height smaller than necessary for keeping aspect ratio - returns size with width set to maxWidth and height clamped to maxHeight`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(minWidth = 30, maxWidth = 30, minHeight = 0, maxHeight = 10),
                aspectRatio = 2f))
        .isEqualTo(Size(width = 30, height = 10))
  }

  @Test
  fun `aspect ratio - with exact width and unbounded height - returns size with maxWidth and correct height`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(
                    minWidth = 30,
                    maxWidth = 30,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity),
                aspectRatio = 2f))
        .isEqualTo(Size(width = 30, height = 15))
  }

  @Test
  fun `aspect ratio - with bounded width smaller than necessary for keeping aspect ratio and exact height - returns size with width clamped to maxWidth and height set to maxHeight`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(minWidth = 0, maxWidth = 10, minHeight = 30, maxHeight = 30),
                aspectRatio = 2f))
        .isEqualTo(Size(width = 10, height = 30))
  }

  @Test
  fun `aspect ratio - with unbounded width and exact height - returns size with correct width and maxHeight`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 30,
                    maxHeight = 30),
                aspectRatio = 2f))
        .isEqualTo(Size(width = 60, height = 30))
  }

  @Test
  fun `aspect ratio - with bounded width unbounded height - returns size with maxWidth and correct height`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = 30,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity),
                aspectRatio = 2f))
        .isEqualTo(Size(width = 30, height = 15))
  }

  @Test
  fun `aspect ratio - with unbounded width and bounded height - returns size with correct width and height set to maxHeight`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 0,
                    maxHeight = 30),
                aspectRatio = 2f))
        .isEqualTo(Size(width = 60, height = 30))
  }

  @Test
  fun `aspect ratio - with unbounded width and unbounded height - returns size with minWidth and minHeight`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(
                    minWidth = 10,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 30,
                    maxHeight = SizeConstraints.Infinity),
                aspectRatio = 2f))
        .isEqualTo(Size(width = 10, height = 30))
  }

  @Test
  fun `aspect ratio - with zero aspect ratio - returns Invalid size`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(minWidth = 0, maxWidth = 10, minHeight = 0, maxHeight = 10),
                aspectRatio = 0f))
        .isEqualTo(Size.Invalid)
  }

  @Test
  fun `aspect ratio - with negative aspect ratio - returns Invalid size`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(minWidth = 0, maxWidth = 10, minHeight = 0, maxHeight = 10),
                aspectRatio = -42f))
        .isEqualTo(Size.Invalid)
  }

  @Test
  fun `aspect ratio - with width bounded to MaxValue - 1 and unbounded height - returns size with values clamped to MaxValue - 1`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.MaxValue - 1,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity),
                aspectRatio = 0.2f))
        .isEqualTo(
            Size(width = SizeConstraints.MaxValue - 1, height = SizeConstraints.MaxValue - 1))
  }

  @Test
  fun `aspect ratio - with unbounded width and height bounded to MaxValue - 1 - returns size with values clamped to MaxValue - 1`() {
    assertThat(
            Size.withAspectRatio(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 0,
                    maxHeight = SizeConstraints.MaxValue - 1),
                aspectRatio = 2.0f))
        .isEqualTo(
            Size(width = SizeConstraints.MaxValue - 1, height = SizeConstraints.MaxValue - 1))
  }

  @Test
  fun `equal dimensions - with bounded width and bounded height - returns size with equal dimensions`() {
    assertThat(
            Size.withEqualDimensions(
                SizeConstraints(minWidth = 0, maxWidth = 30, minHeight = 0, maxHeight = 40)))
        .isEqualTo(Size(width = 30, height = 30))
  }

  @Test
  fun `equal dimensions - with exact width and bounded height smaller than width - returns size with maxWidth and maxHeight`() {
    assertThat(
            Size.withEqualDimensions(
                SizeConstraints(minWidth = 30, maxWidth = 30, minHeight = 0, maxHeight = 10)))
        .isEqualTo(Size(width = 30, height = 10))
  }

  @Test
  fun `equal dimensions - with exact width and unbounded height - returns size with equal dimensions`() {
    assertThat(
            Size.withEqualDimensions(
                SizeConstraints(
                    minWidth = 30,
                    maxWidth = 30,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity)))
        .isEqualTo(Size(width = 30, height = 30))
  }

  @Test
  fun `equal dimensions - with bounded width smaller than height and exact height - returns size with maxWidth and maxHeight`() {
    assertThat(
            Size.withEqualDimensions(
                SizeConstraints(minWidth = 0, maxWidth = 10, minHeight = 30, maxHeight = 30)))
        .isEqualTo(Size(width = 10, height = 30))
  }

  @Test
  fun `equal dimensions - with unbounded width and exact height - returns size with equal dimensions`() {
    assertThat(
            Size.withEqualDimensions(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 30,
                    maxHeight = 30)))
        .isEqualTo(Size(width = 30, height = 30))
  }

  @Test
  fun `equal dimensions - with bounded width and unbounded height - returns size with equal dimensions`() {
    assertThat(
            Size.withEqualDimensions(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = 30,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity)))
        .isEqualTo(Size(width = 30, height = 30))
  }

  @Test
  fun `equal dimensions - with unbounded width and bounded - returns size with equal dimensions`() {
    assertThat(
            Size.withEqualDimensions(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 0,
                    maxHeight = 30)))
        .isEqualTo(Size(width = 30, height = 30))
  }

  @Test
  fun `equal dimensions - with unbounded width and unbounded height - returns size with minWidth and minHeight`() {
    assertThat(
            Size.withEqualDimensions(
                SizeConstraints(
                    minWidth = 10,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 30,
                    maxHeight = SizeConstraints.Infinity)))
        .isEqualTo(Size(width = 10, height = 30))
  }

  @Test
  fun `preferred width and height - with exact width and exact height - returns size with maxWidth and maxHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(minWidth = 30, maxWidth = 30, minHeight = 40, maxHeight = 40),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 30, height = 40))
  }

  @Test
  fun `preferred width and height - with exact width and bounded height - returns size with maxWidth and maxHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(minWidth = 30, maxWidth = 30, minHeight = 20, maxHeight = 40),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 30, height = 40))
  }

  @Test
  fun `preferred width and height - with exact width and unbounded height - returns size with maxWidth and preferredHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(
                    minWidth = 30,
                    maxWidth = 30,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 30, height = 200))
  }

  @Test
  fun `preferred width and height - with bounded width and exact height - returns size with maxWidth and maxHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(minWidth = 10, maxWidth = 30, minHeight = 40, maxHeight = 40),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 30, height = 40))
  }

  @Test
  fun `preferred width and height - with bounded width and bounded height - returns size with maxWidth and maxHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(minWidth = 10, maxWidth = 30, minHeight = 20, maxHeight = 40),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 30, height = 40))
  }

  @Test
  fun `preferred width and height - with bounded width and unbounded height - returns size with maxWidth and preferredHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(
                    minWidth = 10,
                    maxWidth = 30,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 30, height = 200))
  }

  @Test
  fun `preferred width and height - with unbounded width and exact height - returns size with preferredWidth and maxHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 40,
                    maxHeight = 40),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 100, height = 40))
  }

  @Test
  fun `preferred width and height - with unbounded width and bounded height - returns size with preferredWidth and maxHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 20,
                    maxHeight = 40),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 100, height = 40))
  }

  @Test
  fun `preferred width and height - with unbounded width and unbounded height - returns size with preferredWidth and preferredHeight`() {
    assertThat(
            Size.withPreferredSize(
                SizeConstraints(
                    minWidth = 0,
                    maxWidth = SizeConstraints.Infinity,
                    minHeight = 0,
                    maxHeight = SizeConstraints.Infinity),
                preferredWidth = 100,
                preferredHeight = 200))
        .isEqualTo(Size(width = 100, height = 200))
  }

  @Test
  fun `getOrElse - with valid size - doesn't call provided lambda`() {
    var called = false
    val size =
        Size(10, 20).getOrElse {
          called = true
          Size(100, 200)
        }
    assertThat(size.width).isEqualTo(10)
    assertThat(size.height).isEqualTo(20)
    assertThat(called).isFalse
  }

  @Test
  fun `getOrElse - with invalid size - calls provided lambda`() {
    var called = false
    val size =
        Size.Invalid.getOrElse {
          called = true
          Size(100, 200)
        }
    assertThat(size.width).isEqualTo(100)
    assertThat(size.height).isEqualTo(200)
    assertThat(called).isTrue
  }
}
