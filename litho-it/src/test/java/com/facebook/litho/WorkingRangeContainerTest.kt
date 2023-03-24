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

import com.facebook.litho.WorkingRangeContainer.RangeTuple
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class WorkingRangeContainerTest {

  private lateinit var workingRangeContainer: WorkingRangeContainer
  private lateinit var workingRange: WorkingRange
  private val component: SpecGeneratedComponent = mock()
  private val component2: SpecGeneratedComponent = mock()
  private val componentContext: ComponentContext = mock()
  private val componentContext2: ComponentContext = mock()
  private val scopedComponentInfo: ScopedComponentInfo =
      ScopedComponentInfo(component, componentContext, null)
  private val scopedComponentInfo2: ScopedComponentInfo =
      ScopedComponentInfo(component2, componentContext2, null)

  @Before
  fun setup() {
    workingRangeContainer = WorkingRangeContainer()
    workingRange = TestWorkingRange()
    whenever(componentContext.globalKey).thenReturn("component")
    whenever(componentContext2.globalKey).thenReturn("component2")
  }

  @Test
  fun testRegisterWorkingRange() {
    workingRangeContainer.registerWorkingRange(NAME, workingRange, scopedComponentInfo, null)
    val workingRanges = workingRangeContainer.workingRangesForTestOnly
    assertThat(workingRanges.size).isEqualTo(1)
    val key = workingRanges.keys.iterator().next()
    assertThat(key).isEqualTo("${NAME}_${workingRange.hashCode()}")
    val rangeTuple = workingRanges[key]
    assertThat(rangeTuple!!.workingRange).isEqualTo(workingRange)
    assertThat(rangeTuple.scopedComponentInfos.size).isEqualTo(1)
    assertThat(rangeTuple.scopedComponentInfos[0]).isEqualTo(scopedComponentInfo)
  }

  @Test
  fun testIsEnteredRange() {
    val rangeTuple = RangeTuple(NAME, workingRange, scopedComponentInfo, null)
    val workingRange = rangeTuple.workingRange
    assertThat(WorkingRangeContainer.isEnteringRange(workingRange, 0, 0, 1, 0, 1)).isEqualTo(true)
    assertThat(WorkingRangeContainer.isEnteringRange(workingRange, 0, 1, 2, 1, 2)).isEqualTo(false)
  }

  @Test
  fun testIsExitedRange() {
    val rangeTuple = RangeTuple(NAME, workingRange, scopedComponentInfo, null)
    val workingRange = rangeTuple.workingRange
    assertThat(WorkingRangeContainer.isExitingRange(workingRange, 0, 0, 1, 0, 1)).isEqualTo(false)
    assertThat(WorkingRangeContainer.isExitingRange(workingRange, 0, 1, 2, 1, 2)).isEqualTo(true)
  }

  @Test
  fun testDispatchOnExitedRangeIfNeeded() {
    val workingRange = TestWorkingRange()
    workingRangeContainer.registerWorkingRange(NAME, workingRange, scopedComponentInfo, null)
    val workingRange2 = TestWorkingRange()
    workingRangeContainer.registerWorkingRange(NAME, workingRange2, scopedComponentInfo2, null)
    val statusHandler = WorkingRangeStatusHandler()
    statusHandler.setStatus(NAME, component, "component", WorkingRangeStatusHandler.STATUS_IN_RANGE)
    doNothing()
        .`when`(component)
        .dispatchOnExitedRange(
            ArgumentMatchers.isA(ComponentContext::class.java),
            ArgumentMatchers.isA(String::class.java),
            isNull())
    statusHandler.setStatus(
        NAME, component2, "component2", WorkingRangeStatusHandler.STATUS_OUT_OF_RANGE)
    doNothing()
        .`when`(component2)
        .dispatchOnExitedRange(
            ArgumentMatchers.isA(ComponentContext::class.java),
            ArgumentMatchers.isA(String::class.java),
            isNull())
    workingRangeContainer.dispatchOnExitedRangeIfNeeded(statusHandler)
    verify(component, times(1)).dispatchOnExitedRange(componentContext, NAME, null)
    verify(component2, times(0)).dispatchOnExitedRange(componentContext, NAME, null)
  }

  private class TestWorkingRange : WorkingRange {
    var isExitRangeCalled: Boolean = false

    override fun shouldEnterRange(
        position: Int,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        firstFullyVisibleIndex: Int,
        lastFullyVisibleIndex: Int
    ): Boolean = isInRange(position, firstVisibleIndex, lastVisibleIndex)

    override fun shouldExitRange(
        position: Int,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        firstFullyVisibleIndex: Int,
        lastFullyVisibleIndex: Int
    ): Boolean {
      isExitRangeCalled = true
      return !isInRange(position, firstVisibleIndex, lastVisibleIndex)
    }

    companion object {
      private fun isInRange(position: Int, firstVisibleIndex: Int, lastVisibleIndex: Int): Boolean =
          position in firstVisibleIndex..lastVisibleIndex
    }
  }

  companion object {
    private const val NAME = "workingRangeName"
  }
}
