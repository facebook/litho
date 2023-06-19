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

import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class EdgesTest {

  private lateinit var edges: Edges

  @Before
  fun setup() {
    edges = Edges()
  }

  @Test
  fun testInsertingOneEdgeMultipleTimes() {
    edges[YogaEdge.TOP] = 1f
    edges[YogaEdge.TOP] = 2f
    edges[YogaEdge.TOP] = 3f
    edges[YogaEdge.TOP] = 4f
    edges[YogaEdge.TOP] = 5f
    var bits = 0.inv().toLong()
    bits = bits and (0xF.toLong() shl YogaEdge.TOP.intValue() * 4).inv()
    bits = bits or (0L shl YogaEdge.TOP.intValue() * 4)
    assertThat(edgesToValuesIndex).isEqualTo(bits)
    assertThat(valuesArray.size).isEqualTo(2)
    assertThat(valuesArray[0]).isEqualTo(5f)
    assertThat(YogaConstants.isUndefined(valuesArray[1])).isTrue
  }

  @Test
  fun testUnsettingAnEdge() {
    edges[YogaEdge.TOP] = 1f
    edges[YogaEdge.TOP] = 2f
    edges[YogaEdge.TOP] = YogaConstants.UNDEFINED
    val bits = 0.inv().toLong()
    assertThat(edgesToValuesIndex).isEqualTo(bits)
    assertThat(valuesArray.size).isEqualTo(2)
    assertThat(YogaConstants.isUndefined(valuesArray[0])).isTrue
    assertThat(YogaConstants.isUndefined(valuesArray[1])).isTrue
  }

  @Test
  fun testUnsettingNotTheFirstEdge() {
    edges[YogaEdge.TOP] = 1f
    edges[YogaEdge.LEFT] = 2f
    edges[YogaEdge.LEFT] = YogaConstants.UNDEFINED
    var bits = 0.inv().toLong()
    bits = bits and (0xF.toLong() shl YogaEdge.TOP.intValue() * 4).inv()
    bits = bits or (0L shl YogaEdge.TOP.intValue() * 4)
    assertThat(edgesToValuesIndex).isEqualTo(bits)
    assertThat(valuesArray.size).isEqualTo(2)
    assertThat(valuesArray[0]).isEqualTo(1f)
    assertThat(valuesArray[1]).isNaN
  }

  @Test
  fun testSettingMultipleEdgesIncreasesTheArray() {
    edges[YogaEdge.TOP] = 1f
    edges[YogaEdge.LEFT] = 2f
    edges[YogaEdge.ALL] = 5f
    var bits = 0.inv().toLong()
    bits = bits and (0xF.toLong() shl YogaEdge.TOP.intValue() * 4).inv()
    bits = bits and (0xF.toLong() shl YogaEdge.LEFT.intValue() * 4).inv()
    bits = bits and (0xF.toLong() shl YogaEdge.ALL.intValue() * 4).inv()
    bits = bits or (0L shl YogaEdge.TOP.intValue() * 4)
    bits = bits or (1L shl YogaEdge.LEFT.intValue() * 4)
    bits = bits or (2L shl YogaEdge.ALL.intValue() * 4)
    assertThat(edgesToValuesIndex).isEqualTo(bits)
    assertThat(valuesArray.size).isEqualTo(4)
    assertThat(valuesArray[0]).isEqualTo(1f)
    assertThat(valuesArray[1]).isEqualTo(2f)
    assertThat(valuesArray[2]).isEqualTo(5f)
    assertThat(valuesArray[3]).isNaN
  }

  @Test
  fun testUnsettingAndSettingNewEdgesReusesArraySpace() {
    edges[YogaEdge.TOP] = 1f
    edges[YogaEdge.LEFT] = 2f
    edges[YogaEdge.ALL] = 5f
    edges[YogaEdge.LEFT] = YogaConstants.UNDEFINED
    edges[YogaEdge.BOTTOM] = 4f
    var bits = 0.inv().toLong()
    bits = bits and (0xF.toLong() shl YogaEdge.TOP.intValue() * 4).inv()
    bits = bits and (0xF.toLong() shl YogaEdge.ALL.intValue() * 4).inv()
    bits = bits and (0xF.toLong() shl YogaEdge.BOTTOM.intValue() * 4).inv()
    bits = bits or (0L shl YogaEdge.TOP.intValue() * 4)
    bits = bits or (1L shl YogaEdge.BOTTOM.intValue() * 4)
    bits = bits or (2L shl YogaEdge.ALL.intValue() * 4)
    assertThat(edgesToValuesIndex).isEqualTo(bits)
    assertThat(valuesArray.size).isEqualTo(4)
    assertThat(valuesArray[0]).isEqualTo(1f)
    assertThat(valuesArray[1]).isEqualTo(4f)
    assertThat(valuesArray[2]).isEqualTo(5f)
    assertThat(valuesArray[3]).isNaN
  }

  @Test
  fun testAliasesAndResolveGetter() {
    edges[YogaEdge.ALL] = 10f
    assertThat(edges.getRaw(YogaEdge.LEFT)).isNaN
    assertThat(edges.getRaw(YogaEdge.TOP)).isNaN
    assertThat(edges.getRaw(YogaEdge.RIGHT)).isNaN
    assertThat(edges.getRaw(YogaEdge.BOTTOM)).isNaN
    assertThat(edges.getRaw(YogaEdge.ALL)).isEqualTo(10f)
    assertThat(edges[YogaEdge.LEFT]).isEqualTo(10f)
    assertThat(edges[YogaEdge.TOP]).isEqualTo(10f)
    assertThat(edges[YogaEdge.RIGHT]).isEqualTo(10f)
    assertThat(edges[YogaEdge.BOTTOM]).isEqualTo(10f)
    assertThat(edges[YogaEdge.ALL]).isEqualTo(10f)
  }

  @Test
  fun testSameObjectEquivalentTo() {
    assertThat(edges.isEquivalentTo(edges)).isEqualTo(true)
  }

  @Test
  fun testNullObjectEquivalentTo() {
    assertThat(edges.isEquivalentTo(null)).isEqualTo(false)
  }

  @Test
  fun testDifferentObjectWithSameContentEquivalentTo() {
    edges[YogaEdge.TOP] = 1f
    val mEdges2 = Edges()
    mEdges2[YogaEdge.TOP] = 1f
    assertThat(edges.isEquivalentTo(mEdges2)).isEqualTo(true)
  }

  @Test
  fun testDifferentObjectWithDifferentContentEquivalentTo() {
    edges[YogaEdge.LEFT] = 1f
    var mEdges2 = Edges()
    mEdges2[YogaEdge.TOP] = 1f
    assertThat(edges.isEquivalentTo(mEdges2)).isEqualTo(false)
    edges = Edges()
    edges[YogaEdge.TOP] = 1f
    mEdges2 = Edges()
    mEdges2[YogaEdge.TOP] = 2f
    assertThat(edges.isEquivalentTo(mEdges2)).isEqualTo(false)
  }

  private val edgesToValuesIndex: Long
    private get() = Whitebox.getInternalState<Any>(edges, "mEdgesToValuesIndex") as Long

  private val valuesArray: FloatArray
    private get() = Whitebox.getInternalState<Any>(edges, "mValues") as FloatArray
}
