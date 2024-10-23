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

package com.facebook.litho.screenshot

import android.app.Activity
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.internal.TestNameDetector
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Class that directly calls Screenshot.snapActivity() to take a screenshot of the given activity
 * not bounded to the main thread. This is useful for tests that need to take multiple screenshots
 * together with frame delays like Litho Transition tests
 */
class MultiScreenshotterLithoRule : TestRule {

  companion object {
    fun getTestName() = "${TestNameDetector.getTestClass()}_${TestNameDetector.getTestName()}"
  }

  private val testCounterMap = Collections.synchronizedMap(HashMap<String, AtomicInteger>())

  override fun apply(base: Statement, description: Description?): Statement {
    return base
  }

  /**
   * Takes a screen shot, counts the number of screenshots done in the test method and names the
   * screenshot in following format:
   *
   * <p>{test class name}_{test name}_{screen shot count for the test}_{given name} Ex:
   * LithoAnimationsTest_testIncrementalMountDuringAnimation_1.beforeStart
   *
   * @param activity the activity whose screenshot is taken
   * @param name given name for the screenshot
   */
  fun snap(activity: Activity, name: String) {
    val testName = getTestName()
    val count = testCounterMap.getOrPut(testName) { AtomicInteger(0) }.addAndGet(1)

    val ssName = "${testName}_$count.$name"

    Screenshot.snapActivity(activity).setName(ssName).record()
  }
}
