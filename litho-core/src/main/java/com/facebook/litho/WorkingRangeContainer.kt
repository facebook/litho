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

import androidx.annotation.VisibleForTesting
import java.lang.Exception
import java.util.ArrayList
import java.util.LinkedHashMap
import kotlin.jvm.JvmField

/**
 * A container that stores working range related information. It provides two major methods: a
 * register method to store a working range with a component, and a dispatch method that dispatches
 * event to components to trigger their delegated methods.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class WorkingRangeContainer {

  /**
   * Use [java.util.HashMap] to store the working range of each component. The key is composed with
   * name and working range hashcode. The value is a [RangeTuple] object that contains a working
   * range related information.
   */
  private val workingRanges: MutableMap<String, RangeTuple> by lazy { LinkedHashMap() }

  fun registerWorkingRange(
      name: String,
      workingRange: WorkingRange,
      scopedComponentInfo: ScopedComponentInfo,
      interStageProps: InterStagePropsContainer?
  ) {
    val key = "${name}_${workingRange.hashCode()}"
    val rangeTuple = workingRanges[key]
    if (rangeTuple == null) {
      workingRanges[key] = RangeTuple(name, workingRange, scopedComponentInfo, interStageProps)
    } else {
      rangeTuple.addComponent(scopedComponentInfo)
    }
  }

  /**
   * Iterate the map to check if a component is entered or exited the range, and dispatch event to
   * the component to trigger its delegate method.
   */
  fun checkWorkingRangeAndDispatch(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int,
      statusHandler: WorkingRangeStatusHandler
  ) {
    if (workingRanges.isEmpty()) {
      return
    }

    for (key in workingRanges.keys) {
      val rangeTuple = checkNotNull(workingRanges[key])
      var i = 0
      val size = rangeTuple.scopedComponentInfos.size
      while (i < size) {
        val scopedComponentInfo = rangeTuple.scopedComponentInfos[i]
        val scopedContext = scopedComponentInfo.context
        val component = scopedComponentInfo.component as SpecGeneratedComponent
        val globalKey = scopedContext.globalKey
        if (!statusHandler.isInRange(rangeTuple.name, component, globalKey) &&
            isEnteringRange(
                rangeTuple.workingRange,
                position,
                firstVisibleIndex,
                lastVisibleIndex,
                firstFullyVisibleIndex,
                lastFullyVisibleIndex)) {
          try {
            component.dispatchOnEnteredRange(
                scopedContext, rangeTuple.name, rangeTuple.interStagePropsContainer)
          } catch (e: Exception) {
            ComponentUtils.handle(scopedContext, e)
          }
          statusHandler.setEnteredRangeStatus(rangeTuple.name, component, globalKey)
        } else if (statusHandler.isInRange(rangeTuple.name, component, globalKey) &&
            isExitingRange(
                rangeTuple.workingRange,
                position,
                firstVisibleIndex,
                lastVisibleIndex,
                firstFullyVisibleIndex,
                lastFullyVisibleIndex)) {
          try {
            component.dispatchOnExitedRange(
                scopedContext, rangeTuple.name, rangeTuple.interStagePropsContainer)
          } catch (e: Exception) {
            ComponentUtils.handle(scopedContext, e)
          }
          statusHandler.setExitedRangeStatus(rangeTuple.name, component, globalKey)
        }
        i++
      }
    }
  }

  /**
   * Dispatch onExitRange if the status of the component is in the range. This method should only be
   * called when releasing a ComponentTree, thus no status update needed.
   */
  fun dispatchOnExitedRangeIfNeeded(statusHandler: WorkingRangeStatusHandler) {
    if (workingRanges.isEmpty()) {
      return
    }

    for (key in workingRanges.keys) {
      val rangeTuple = checkNotNull(workingRanges[key])
      var i = 0
      val size = rangeTuple.scopedComponentInfos.size
      while (i < size) {
        val scopedComponentInfo = rangeTuple.scopedComponentInfos[i]
        val scopedContext = scopedComponentInfo.context
        // working ranges are only available in Spec Components, so we can cast it here
        val component = scopedComponentInfo.component as SpecGeneratedComponent
        val globalKey = scopedContext.globalKey
        if (statusHandler.isInRange(rangeTuple.name, component, globalKey)) {
          try {
            component.dispatchOnExitedRange(
                scopedContext, rangeTuple.name, rangeTuple.interStagePropsContainer)
          } catch (e: Exception) {
            ComponentUtils.handle(scopedContext, e)
          }
        }
        i++
      }
    }
  }

  @get:VisibleForTesting
  val workingRangesForTestOnly: Map<String, RangeTuple>
    get() = workingRanges

  /**
   * A tuple that stores working range information for a list of components that share same name and
   * working range object.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  class RangeTuple(
      @JvmField val name: String,
      @JvmField val workingRange: WorkingRange,
      scopedComponentInfo: ScopedComponentInfo,
      @JvmField val interStagePropsContainer: InterStagePropsContainer?
  ) {

    @JvmField val scopedComponentInfos: MutableList<ScopedComponentInfo> = ArrayList()

    fun addComponent(scopedComponentInfo: ScopedComponentInfo) {
      scopedComponentInfos.add(scopedComponentInfo)
    }

    init {
      scopedComponentInfos.add(scopedComponentInfo)
    }
  }

  /** A tuple that stores raw data of a working range registration. */
  internal class Registration(
      @JvmField val name: String,
      @JvmField val workingRange: WorkingRange,
      @JvmField val scopedComponentInfo: ScopedComponentInfo
  )

  companion object {
    @JvmStatic
    fun isEnteringRange(
        workingRange: WorkingRange,
        position: Int,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        firstFullyVisibleIndex: Int,
        lastFullyVisibleIndex: Int
    ): Boolean =
        workingRange.shouldEnterRange(
            position,
            firstVisibleIndex,
            lastVisibleIndex,
            firstFullyVisibleIndex,
            lastFullyVisibleIndex)

    @JvmStatic
    fun isExitingRange(
        workingRange: WorkingRange,
        position: Int,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        firstFullyVisibleIndex: Int,
        lastFullyVisibleIndex: Int
    ): Boolean =
        workingRange.shouldExitRange(
            position,
            firstVisibleIndex,
            lastVisibleIndex,
            firstFullyVisibleIndex,
            lastFullyVisibleIndex)
  }
}
