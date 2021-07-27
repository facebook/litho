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

package com.facebook.litho

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

const val DENSITY = 2f
const val SCALED_DENSITY = 10f

/** Unit tests [Dimen]. */
@RunWith(AndroidJUnit4::class)
class DimenTest {

  private val resourceResolver = MockResourceResolver(DENSITY, SCALED_DENSITY)

  @Test
  fun `non-negative integer px values are correctly encoded and decoded`() {
    assertThat(0.px.toPixels(resourceResolver)).isEqualTo(0)
    assertThat(1.px.toPixels(resourceResolver)).isEqualTo(1)
    assertThat(10.px.toPixels(resourceResolver)).isEqualTo(10)
    assertThat(101.px.toPixels(resourceResolver)).isEqualTo(101)
    assertThat(9001.px.toPixels(resourceResolver)).isEqualTo(9001)
  }

  @Test
  fun `negative integer px values are correctly encoded and decoded`() {
    assertThat(-1.px.toPixels(resourceResolver)).isEqualTo(-1)
    assertThat(-10.px.toPixels(resourceResolver)).isEqualTo(-10)
    assertThat(-101.px.toPixels(resourceResolver)).isEqualTo(-101)
    assertThat(-9001.px.toPixels(resourceResolver)).isEqualTo(-9001)
  }

  @Test
  fun `non-negative integer dp values are correctly encoded and decoded`() {
    assertThat(0.dp.toPixels(resourceResolver)).isEqualTo(0)
    assertThat(1.dp.toPixels(resourceResolver)).isEqualTo(2)
    assertThat(10.dp.toPixels(resourceResolver)).isEqualTo(20)
    assertThat(101.dp.toPixels(resourceResolver)).isEqualTo(202)
    assertThat(9001.dp.toPixels(resourceResolver)).isEqualTo(18002)
  }

  @Test
  fun `negative integer dp values are correctly encoded and decoded`() {
    assertThat(-1.dp.toPixels(resourceResolver)).isEqualTo(-2)
    assertThat(-10.dp.toPixels(resourceResolver)).isEqualTo(-20)
    assertThat(-101.dp.toPixels(resourceResolver)).isEqualTo(-202)
    assertThat(-9001.dp.toPixels(resourceResolver)).isEqualTo(-18002)
  }

  @Test
  fun `non-negative integer sp values are correctly encoded and decoded`() {
    assertThat(0.sp.toPixels(resourceResolver)).isEqualTo(0)
    assertThat(1.sp.toPixels(resourceResolver)).isEqualTo(10)
    assertThat(10.sp.toPixels(resourceResolver)).isEqualTo(100)
    assertThat(101.sp.toPixels(resourceResolver)).isEqualTo(1010)
    assertThat(9001.sp.toPixels(resourceResolver)).isEqualTo(90010)
  }

  @Test
  fun `negative integer sp values are correctly encoded and decoded`() {
    assertThat(-1.sp.toPixels(resourceResolver)).isEqualTo(-10)
    assertThat(-10.sp.toPixels(resourceResolver)).isEqualTo(-100)
    assertThat(-101.sp.toPixels(resourceResolver)).isEqualTo(-1010)
    assertThat(-9001.sp.toPixels(resourceResolver)).isEqualTo(-90010)
  }

  // todo here

  @Test
  fun `non-negative float px values are correctly encoded and decoded`() {
    assertThat(0f.px.toPixels(resourceResolver)).isEqualTo(0)
    assertThat(1.5f.px.toPixels(resourceResolver)).isEqualTo(1)
    assertThat(10.2f.px.toPixels(resourceResolver)).isEqualTo(10)
    assertThat(101.11f.px.toPixels(resourceResolver)).isEqualTo(101)
    assertThat(9001.9f.px.toPixels(resourceResolver)).isEqualTo(9001)
  }

  @Test
  fun `negative float px values are correctly encoded and decoded`() {
    assertThat(-1.5f.px.toPixels(resourceResolver)).isEqualTo(-1)
    assertThat(-10.2f.px.toPixels(resourceResolver)).isEqualTo(-10)
    assertThat(-101.11f.px.toPixels(resourceResolver)).isEqualTo(-101)
    assertThat(-9001.9f.px.toPixels(resourceResolver)).isEqualTo(-9001)
  }

  @Test
  fun `non-negative float dp values are correctly encoded and decoded`() {
    assertThat(0f.dp.toPixels(resourceResolver)).isEqualTo(0)
    assertThat(1.5f.dp.toPixels(resourceResolver)).isEqualTo(3)
    assertThat(10.2f.dp.toPixels(resourceResolver)).isEqualTo(20)
    assertThat(101.11f.dp.toPixels(resourceResolver)).isEqualTo(202)
    assertThat(9001.9f.dp.toPixels(resourceResolver)).isEqualTo(18004)
  }

  @Test
  fun `negative float dp values are correctly encoded and decoded`() {
    assertThat(-1.5f.dp.toPixels(resourceResolver)).isEqualTo(-3)
    assertThat(-10.2f.dp.toPixels(resourceResolver)).isEqualTo(-20)
    assertThat(-101.11f.dp.toPixels(resourceResolver)).isEqualTo(-202)
    assertThat(-9001.9f.dp.toPixels(resourceResolver)).isEqualTo(-18004)
  }

  @Test
  fun `non-negative float sp values are correctly encoded and decoded`() {
    assertThat(0f.sp.toPixels(resourceResolver)).isEqualTo(0)
    assertThat(1.5f.sp.toPixels(resourceResolver)).isEqualTo(15)
    assertThat(10.2f.sp.toPixels(resourceResolver)).isEqualTo(102)
    assertThat(101.11f.sp.toPixels(resourceResolver)).isEqualTo(1011)
    assertThat(9001.9f.sp.toPixels(resourceResolver)).isEqualTo(90019)
  }

  @Test
  fun `negative float sp values are correctly encoded and decoded`() {
    assertThat(-1.5f.sp.toPixels(resourceResolver)).isEqualTo(-15)
    assertThat(-10.2f.sp.toPixels(resourceResolver)).isEqualTo(-102)
    assertThat(-101.11f.sp.toPixels(resourceResolver)).isEqualTo(-1011)
    assertThat(-9001.9f.sp.toPixels(resourceResolver)).isEqualTo(-90019)
  }

  private class MockResourceResolver(val density: Float, val scaledDensity: Float) :
      ResourceResolver(ComponentContext(ApplicationProvider.getApplicationContext<Context>())) {
    override fun dipsToPixels(dips: Float): Int = FastMath.round(dips * density)
    override fun sipsToPixels(sips: Float): Int = FastMath.round(sips * scaledDensity)
  }
}
