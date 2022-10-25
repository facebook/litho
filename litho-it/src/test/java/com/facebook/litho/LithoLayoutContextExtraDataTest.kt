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

import com.facebook.rendercore.LayoutContextExtraData
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class LithoLayoutContextExtraDataTest {

  @Mock private lateinit var yogaNode: YogaNode

  private lateinit var layoutContextExtraData: LayoutContextExtraData<*>

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    layoutContextExtraData = LithoLayoutContextExtraData(yogaNode)
  }

  @Test
  fun testGetExtraDataReturnsExpectedType() {
    val extraData = layoutContextExtraData.extraLayoutData

    assertThat(extraData).isInstanceOf(LithoLayoutContextExtraData.LithoLayoutExtraData::class.java)
  }

  @Test
  fun testGetExtraDataReturnsExpectedValueForLayoutDirection() {
    whenever(yogaNode.layoutDirection).thenReturn(YogaDirection.RTL)

    val extraData =
        layoutContextExtraData.extraLayoutData as LithoLayoutContextExtraData.LithoLayoutExtraData

    assertThat(extraData.layoutDirection).isEqualTo(YogaDirection.RTL)
  }
}
