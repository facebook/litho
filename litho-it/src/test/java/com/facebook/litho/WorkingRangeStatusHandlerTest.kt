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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(LithoTestRunner::class)
class WorkingRangeStatusHandlerTest {

  private val workingRangeName = "workingRangeName"
  private val globalKey = "globalKey"

  private lateinit var workingRangeStateHandler: WorkingRangeStatusHandler
  private lateinit var component: Component

  @Before
  fun setup() {
    workingRangeStateHandler = WorkingRangeStatusHandler()
    component = mock()
  }

  @Test
  fun testIsNotInRange() {
    workingRangeStateHandler.setStatus(
        workingRangeName, component, globalKey, WorkingRangeStatusHandler.STATUS_OUT_OF_RANGE)
    val notInRange = !workingRangeStateHandler.isInRange(workingRangeName, component, globalKey)
    assertThat(notInRange).isEqualTo(true)
  }

  @Test
  fun testIsInRange() {
    workingRangeStateHandler.setStatus(
        workingRangeName, component, globalKey, WorkingRangeStatusHandler.STATUS_IN_RANGE)
    val inRange = workingRangeStateHandler.isInRange(workingRangeName, component, globalKey)
    assertThat(inRange).isEqualTo(true)
  }

  @Test
  fun testSetEnteredRangeState() {
    workingRangeStateHandler.setStatus(
        workingRangeName, component, globalKey, WorkingRangeStatusHandler.STATUS_OUT_OF_RANGE)
    val notInRange = !workingRangeStateHandler.isInRange(workingRangeName, component, globalKey)
    assertThat(notInRange).isEqualTo(true)
    workingRangeStateHandler.setEnteredRangeStatus(workingRangeName, component, globalKey)
    val inRange = workingRangeStateHandler.isInRange(workingRangeName, component, globalKey)
    assertThat(inRange).isEqualTo(true)
  }

  @Test
  fun testSetExitedRangeState() {
    workingRangeStateHandler.setStatus(
        workingRangeName, component, globalKey, WorkingRangeStatusHandler.STATUS_IN_RANGE)
    val inRange = workingRangeStateHandler.isInRange(workingRangeName, component, globalKey)
    assertThat(inRange).isEqualTo(true)
    workingRangeStateHandler.setExitedRangeStatus(workingRangeName, component, globalKey)
    val notInRange = !workingRangeStateHandler.isInRange(workingRangeName, component, globalKey)
    assertThat(notInRange).isEqualTo(true)
  }
}
