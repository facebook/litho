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

package com.facebook.rendercore.geometry

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit tests for [Size]. */
@RunWith(RobolectricTestRunner::class)
class SizeTest {

  @Test
  fun `constructor creates size with correct dimensions`() {
    val size = Size(100f, 200f)
    Assertions.assertThat(size.width).isEqualTo(100f)
    Assertions.assertThat(size.height).isEqualTo(200f)
  }

  @Test
  fun `Size Zero has zero width and height`() {
    Assertions.assertThat(Size.Zero.width).isEqualTo(0f)
    Assertions.assertThat(Size.Zero.height).isEqualTo(0f)
  }

  @Test
  fun `Size Unspecified isUnspecified returns true`() {
    Assertions.assertThat(Size.Unspecified.isUnspecified).isTrue()
    Assertions.assertThat(Size.Unspecified.isSpecified).isFalse()
  }

  @Test
  fun `regular size isSpecified returns true`() {
    val size = Size(10f, 20f)
    Assertions.assertThat(size.isSpecified).isTrue()
    Assertions.assertThat(size.isUnspecified).isFalse()
  }

  @Test
  fun `component1 returns width`() {
    val size = Size(50f, 100f)
    val (width, _) = size
    Assertions.assertThat(width).isEqualTo(50f)
  }

  @Test
  fun `component2 returns height`() {
    val size = Size(50f, 100f)
    val (_, height) = size
    Assertions.assertThat(height).isEqualTo(100f)
  }

  @Test
  fun `copy creates new size with same dimensions`() {
    val original = Size(100f, 200f)
    val copied = original.copy()
    Assertions.assertThat(copied.width).isEqualTo(original.width)
    Assertions.assertThat(copied.height).isEqualTo(original.height)
  }

  @Test
  fun `copy with width override creates new size with new width`() {
    val original = Size(100f, 200f)
    val copied = original.copy(width = 150f)
    Assertions.assertThat(copied.width).isEqualTo(150f)
    Assertions.assertThat(copied.height).isEqualTo(200f)
  }

  @Test
  fun `copy with height override creates new size with new height`() {
    val original = Size(100f, 200f)
    val copied = original.copy(height = 250f)
    Assertions.assertThat(copied.width).isEqualTo(100f)
    Assertions.assertThat(copied.height).isEqualTo(250f)
  }

  @Test
  fun `isEmpty returns true for zero width`() {
    val size = Size(0f, 100f)
    Assertions.assertThat(size.isValid()).isTrue()
  }

  @Test
  fun `isEmpty returns true for zero height`() {
    val size = Size(100f, 0f)
    Assertions.assertThat(size.isValid()).isTrue()
  }

  @Test
  fun `isEmpty returns true for negative width`() {
    val size = Size(-10f, 100f)
    Assertions.assertThat(size.isValid()).isTrue()
  }

  @Test
  fun `isEmpty returns true for negative height`() {
    val size = Size(100f, -10f)
    Assertions.assertThat(size.isValid()).isTrue()
  }

  @Test
  fun `isEmpty returns false for positive dimensions`() {
    val size = Size(100f, 200f)
    Assertions.assertThat(size.isValid()).isFalse()
  }

  @Test
  fun `isEmpty returns true for Unspecified size`() {
    Assertions.assertThat(Size.Unspecified.isValid()).isTrue()
  }

  @Test
  fun `times operator multiplies dimensions by scalar`() {
    val size = Size(10f, 20f)
    val result = size * 2f
    Assertions.assertThat(result.width).isEqualTo(20f)
    Assertions.assertThat(result.height).isEqualTo(40f)
  }

  @Test
  fun `div operator divides dimensions by scalar`() {
    val size = Size(20f, 40f)
    val result = size / 2f
    Assertions.assertThat(result.width).isEqualTo(10f)
    Assertions.assertThat(result.height).isEqualTo(20f)
  }

  @Test
  fun `minDimension returns smaller of width and height`() {
    val size = Size(100f, 200f)
    Assertions.assertThat(size.minDimension).isEqualTo(100f)
  }

  @Test
  fun `minDimension returns smaller dimension when height is smaller`() {
    val size = Size(200f, 100f)
    Assertions.assertThat(size.minDimension).isEqualTo(100f)
  }

  @Test
  fun `maxDimension returns larger of width and height`() {
    val size = Size(100f, 200f)
    Assertions.assertThat(size.maxDimension).isEqualTo(200f)
  }

  @Test
  fun `maxDimension returns larger dimension when width is larger`() {
    val size = Size(200f, 100f)
    Assertions.assertThat(size.maxDimension).isEqualTo(200f)
  }

  @Test
  fun `toString returns correct format for specified size`() {
    val size = Size(100f, 200f)
    Assertions.assertThat(size.toString()).isEqualTo("Size(100.0, 200.0)")
  }

  @Test
  fun `toString returns Unspecified for unspecified size`() {
    Assertions.assertThat(Size.Unspecified.toString()).isEqualTo("Size.Unspecified")
  }

  @Test
  fun `equality works for same dimensions`() {
    val size1 = Size(100f, 200f)
    val size2 = Size(100f, 200f)
    Assertions.assertThat(size1).isEqualTo(size2)
  }

  @Test
  fun `inequality works for different dimensions`() {
    val size1 = Size(100f, 200f)
    val size2 = Size(100f, 300f)
    Assertions.assertThat(size1).isNotEqualTo(size2)
  }

  @Test
  fun `takeOrElse returns size when specified`() {
    val size = Size(100f, 200f)
    val result = size.takeOrElse { Size(50f, 50f) }
    Assertions.assertThat(result).isEqualTo(size)
  }

  @Test
  fun `takeOrElse returns block result when unspecified`() {
    val fallback = Size(50f, 50f)
    val result = Size.Unspecified.takeOrElse { fallback }
    Assertions.assertThat(result).isEqualTo(fallback)
  }

  @Test
  fun `Int times Size returns correct size`() {
    val size = Size(10f, 20f)
    val result = 3 * size
    Assertions.assertThat(result.width).isEqualTo(30f)
    Assertions.assertThat(result.height).isEqualTo(60f)
  }

  @Test
  fun `Double times Size returns correct size`() {
    val size = Size(10f, 20f)
    val result = 2.5 * size
    Assertions.assertThat(result.width).isEqualTo(25f)
    Assertions.assertThat(result.height).isEqualTo(50f)
  }

  @Test
  fun `Float times Size returns correct size`() {
    val size = Size(10f, 20f)
    val result = 1.5f * size
    Assertions.assertThat(result.width).isEqualTo(15f)
    Assertions.assertThat(result.height).isEqualTo(30f)
  }

  @Test
  fun `toRect converts size to rect with origin at 0,0`() {
    val size = Size(100f, 200f)
    val rect = size.toRect()
    Assertions.assertThat(rect.left).isEqualTo(0)
    Assertions.assertThat(rect.top).isEqualTo(0)
    Assertions.assertThat(rect.right).isEqualTo(100)
    Assertions.assertThat(rect.bottom).isEqualTo(200)
  }

  @Test
  fun `center returns correct position`() {
    val size = Size(100f, 200f)
    val center = size.center
    Assertions.assertThat(center.x).isEqualTo(50f)
    Assertions.assertThat(center.y).isEqualTo(100f)
  }

  @Test
  fun `center of zero size is zero`() {
    val center = Size.Zero.center
    Assertions.assertThat(center.x).isEqualTo(0f)
    Assertions.assertThat(center.y).isEqualTo(0f)
  }
}
