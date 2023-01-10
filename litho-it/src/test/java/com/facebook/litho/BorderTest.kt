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

package com.facebook.litho

import android.content.Context
import android.graphics.ComposePathEffect
import android.graphics.DashPathEffect
import android.graphics.DiscretePathEffect
import android.graphics.Path
import android.graphics.PathDashPathEffect
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class BorderTest {

  @Test
  fun testIndividualColorSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border =
        Border.create(c)
            .color(YogaEdge.LEFT, 0xFFFF0000.toInt())
            .color(YogaEdge.TOP, 0xFFFFFF00.toInt())
            .color(YogaEdge.RIGHT, 0xFFFFFFFF.toInt())
            .color(YogaEdge.BOTTOM, 0xFFFF00FF.toInt())
            .build()
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFFFFFF00.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFFFFFFFF.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFFFF00FF.toInt())
  }

  @Test
  fun testAllColorSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border = Border.create(c).color(YogaEdge.ALL, 0xFFFF0000.toInt()).build()
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFFFF0000.toInt())
  }

  @Test
  fun testHorizontalColorSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border =
        Border.create(c)
            .color(YogaEdge.ALL, 0xFFFF0000.toInt())
            .color(YogaEdge.HORIZONTAL, 0xFF00FF00.toInt())
            .build()
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFF00FF00.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFF00FF00.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFFFF0000.toInt())
  }

  @Test
  fun testVerticalColorSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border =
        Border.create(c)
            .color(YogaEdge.ALL, 0xFFFF0000.toInt())
            .color(YogaEdge.VERTICAL, 0xFF00FF00.toInt())
            .build()
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFF00FF00.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFF00FF00.toInt())
  }

  @Test
  fun testStartEndResolving() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border =
        Border.create(c)
            .color(YogaEdge.START, 0xFFFF0000.toInt())
            .color(YogaEdge.END, 0x0000FFFF)
            .widthPx(YogaEdge.START, 100)
            .widthPx(YogaEdge.END, 200)
            .build()
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0x0000FFFF)
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(100)
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(200)
  }

  @Test
  fun testIndividualWidthSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border =
        Border.create(c)
            .widthPx(YogaEdge.LEFT, 1)
            .widthPx(YogaEdge.TOP, 2)
            .widthPx(YogaEdge.RIGHT, 3)
            .widthPx(YogaEdge.BOTTOM, 4)
            .build()
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(1)
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(2)
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(3)
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(4)
  }

  @Test
  fun testAllWidthSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border = Border.create(c).widthPx(YogaEdge.ALL, 5).build()
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(5)
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(5)
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(5)
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(5)
  }

  @Test
  fun testHorizontalWidthSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border = Border.create(c).widthPx(YogaEdge.ALL, 1).widthPx(YogaEdge.HORIZONTAL, 5).build()
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(5)
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(1)
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(5)
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(1)
  }

  @Test
  fun testVerticalWidthSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border = Border.create(c).widthPx(YogaEdge.ALL, 1).widthPx(YogaEdge.VERTICAL, 5).build()
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(1)
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(5)
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(1)
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(5)
  }

  @Test
  fun testAllColorWidthSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border =
        Border.create(c)
            .color(YogaEdge.LEFT, 0xFFFF0000.toInt())
            .color(YogaEdge.TOP, 0xFFFFFF00.toInt())
            .color(YogaEdge.RIGHT, 0xFFFFFFFF.toInt())
            .color(YogaEdge.BOTTOM, 0xFFFF00FF.toInt())
            .widthPx(YogaEdge.LEFT, 1)
            .widthPx(YogaEdge.TOP, 2)
            .widthPx(YogaEdge.RIGHT, 3)
            .widthPx(YogaEdge.BOTTOM, 4)
            .build()
    assertThat(border.mEdgeColors[Border.EDGE_LEFT]).isEqualTo(0xFFFF0000.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_TOP]).isEqualTo(0xFFFFFF00.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_RIGHT]).isEqualTo(0xFFFFFFFF.toInt())
    assertThat(border.mEdgeColors[Border.EDGE_BOTTOM]).isEqualTo(0xFFFF00FF.toInt())
    assertThat(border.mEdgeWidths[Border.EDGE_LEFT]).isEqualTo(1)
    assertThat(border.mEdgeWidths[Border.EDGE_TOP]).isEqualTo(2)
    assertThat(border.mEdgeWidths[Border.EDGE_RIGHT]).isEqualTo(3)
    assertThat(border.mEdgeWidths[Border.EDGE_BOTTOM]).isEqualTo(4)
  }

  @Test
  fun testBothRadiiSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border = Border.create(c).radiusPx(1_337).build()
    assertThat(border.mRadius[Border.Corner.BOTTOM_LEFT]).isEqualTo(1_337f)
    assertThat(border.mRadius[Border.Corner.BOTTOM_RIGHT]).isEqualTo(1_337f)
    assertThat(border.mRadius[Border.Corner.TOP_LEFT]).isEqualTo(1_337f)
    assertThat(border.mRadius[Border.Corner.TOP_RIGHT]).isEqualTo(1_337f)
  }

  @Test
  fun testIndividualRadiiSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border =
        Border.create(c)
            .radiusPx(Border.Corner.TOP_LEFT, 1)
            .radiusPx(Border.Corner.TOP_RIGHT, 2)
            .radiusPx(Border.Corner.BOTTOM_RIGHT, 3)
            .radiusPx(Border.Corner.BOTTOM_LEFT, 4)
            .build()
    assertThat(border.mRadius[Border.Corner.TOP_LEFT]).isEqualTo(1f)
    assertThat(border.mRadius[Border.Corner.TOP_RIGHT]).isEqualTo(2f)
    assertThat(border.mRadius[Border.Corner.BOTTOM_RIGHT]).isEqualTo(3f)
    assertThat(border.mRadius[Border.Corner.BOTTOM_LEFT]).isEqualTo(4f)
  }

  @Test
  fun testEffectSetting() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    var border = Border.create(c).dashEffect(floatArrayOf(1f, 1f), 0f).build()
    assertThat(border.mPathEffect).isInstanceOf(DashPathEffect::class.java)
    border = Border.create(c).discreteEffect(1f, 0f).build()
    assertThat(border.mPathEffect).isInstanceOf(DiscretePathEffect::class.java)
    border =
        Border.create(c).pathDashEffect(Path(), 0f, 0f, PathDashPathEffect.Style.ROTATE).build()
    assertThat(border.mPathEffect).isInstanceOf(PathDashPathEffect::class.java)
    border = Border.create(c).discreteEffect(1f, 1f).dashEffect(floatArrayOf(1f, 2f), 1f).build()
    assertThat(border.mPathEffect).isInstanceOf(ComposePathEffect::class.java)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testTooManyEffectsThrows() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    Border.create(c)
        .pathDashEffect(Path(), 1f, 1f, PathDashPathEffect.Style.MORPH)
        .dashEffect(floatArrayOf(1f, 2f), 1f)
        .discreteEffect(1f, 2f)
        .build()
  }

  @Test(expected = IllegalArgumentException::class)
  fun testDifferentWidthWithEffectThrows() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    Border.create(c)
        .widthPx(YogaEdge.ALL, 10)
        .widthPx(YogaEdge.LEFT, 5)
        .discreteEffect(1f, 1f)
        .build()
  }

  @Test
  fun testSameObjectEquivalentTo() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border = Border.create(c).build()
    assertThat(border.isEquivalentTo(border)).isEqualTo(true)
  }

  @Test
  fun testNullObjectEquivalentTo() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border = Border.create(c).build()
    assertThat(border.isEquivalentTo(null)).isEqualTo(false)
  }

  @Test
  fun testDifferentObjectWithSameContentEquivalentTo() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border1 =
        Border.create(c)
            .color(YogaEdge.LEFT, 0xFFFF0000.toInt())
            .color(YogaEdge.TOP, 0xFFFFFF00.toInt())
            .color(YogaEdge.RIGHT, 0xFFFFFFFF.toInt())
            .color(YogaEdge.BOTTOM, 0xFFFF00FF.toInt())
            .widthPx(YogaEdge.LEFT, 1)
            .widthPx(YogaEdge.TOP, 2)
            .widthPx(YogaEdge.RIGHT, 3)
            .widthPx(YogaEdge.BOTTOM, 4)
            .radiusPx(Border.Corner.TOP_LEFT, 1)
            .radiusPx(Border.Corner.TOP_RIGHT, 2)
            .radiusPx(Border.Corner.BOTTOM_RIGHT, 3)
            .radiusPx(Border.Corner.BOTTOM_LEFT, 4)
            .build()
    val border2 =
        Border.create(c)
            .color(YogaEdge.LEFT, 0xFFFF0000.toInt())
            .color(YogaEdge.TOP, 0xFFFFFF00.toInt())
            .color(YogaEdge.RIGHT, 0xFFFFFFFF.toInt())
            .color(YogaEdge.BOTTOM, 0xFFFF00FF.toInt())
            .widthPx(YogaEdge.LEFT, 1)
            .widthPx(YogaEdge.TOP, 2)
            .widthPx(YogaEdge.RIGHT, 3)
            .widthPx(YogaEdge.BOTTOM, 4)
            .radiusPx(Border.Corner.TOP_LEFT, 1)
            .radiusPx(Border.Corner.TOP_RIGHT, 2)
            .radiusPx(Border.Corner.BOTTOM_RIGHT, 3)
            .radiusPx(Border.Corner.BOTTOM_LEFT, 4)
            .build()
    assertThat(border1.isEquivalentTo(border2)).isEqualTo(true)
  }

  @Test
  fun testDifferentObjectWithDifferentContentEquivalentTo() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val border1 =
        Border.create(c)
            .color(YogaEdge.LEFT, 0xFFFF0000.toInt())
            .color(YogaEdge.TOP, 0xFF29FF00.toInt())
            .color(YogaEdge.RIGHT, 0xFFFFFFFF.toInt())
            .color(YogaEdge.BOTTOM, 0xFFFF00FF.toInt())
            .widthPx(YogaEdge.LEFT, 1)
            .widthPx(YogaEdge.TOP, 2)
            .widthPx(YogaEdge.RIGHT, 3)
            .widthPx(YogaEdge.BOTTOM, 3)
            .radiusPx(Border.Corner.TOP_LEFT, 1)
            .radiusPx(Border.Corner.TOP_RIGHT, 2)
            .radiusPx(Border.Corner.BOTTOM_RIGHT, 6)
            .radiusPx(Border.Corner.BOTTOM_LEFT, 4)
            .build()
    val border2 =
        Border.create(c)
            .color(YogaEdge.LEFT, 0xFFFF0000.toInt())
            .color(YogaEdge.TOP, 0xFFFFFF00.toInt())
            .color(YogaEdge.RIGHT, 0xFFFFFFFF.toInt())
            .color(YogaEdge.BOTTOM, 0xFFFF00FF.toInt())
            .widthPx(YogaEdge.LEFT, 1)
            .widthPx(YogaEdge.TOP, 2)
            .widthPx(YogaEdge.RIGHT, 3)
            .widthPx(YogaEdge.BOTTOM, 4)
            .radiusPx(Border.Corner.TOP_LEFT, 1)
            .radiusPx(Border.Corner.TOP_RIGHT, 2)
            .radiusPx(Border.Corner.BOTTOM_RIGHT, 3)
            .radiusPx(Border.Corner.BOTTOM_LEFT, 4)
            .build()
    assertThat(border1.isEquivalentTo(border2)).isEqualTo(false)
  }
}
