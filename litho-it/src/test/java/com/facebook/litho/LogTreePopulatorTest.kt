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

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.LogTreePopulatorTest.MyKey
import com.facebook.litho.testing.logging.TestComponentsLogger
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@RunWith(LithoTestRunner::class)
class LogTreePopulatorTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(getApplicationContext(), "test", TestComponentsLogger())
  }

  @Test
  fun testCustomTreePropLogger() {
    val logger: ComponentsLogger =
        object : TestComponentsLogger() {
          override fun getExtraAnnotations(
              treePropertyProvider: TreePropertyProvider
          ): Map<String, String>? {
            return mapOf("my_key" to treePropertyProvider.getProperty(MyKey::class.java).toString())
          }
        }
    val event: PerfEvent = mock()
    context.treeProps = TreeProps().apply { put(MyKey::class.java, 1_337) }
    LogTreePopulator.populatePerfEventFromLogger(context, logger, event)
    verify(event).markerAnnotate("my_key", "1337")
  }

  @Test
  fun testSkipOnEmptyTag() {
    val logger: TestComponentsLogger =
        object : TestComponentsLogger() {
          override fun getExtraAnnotations(
              treePropertyProvider: TreePropertyProvider
          ): Map<String, String>? {
            return mapOf("my_key" to treePropertyProvider.getProperty(MyKey::class.java).toString())
          }
        }
    val event: PerfEvent = mock()
    context.treeProps = TreeProps().apply { put(MyKey::class.java, 1_337) }
    val noLogTagContext = ComponentContext(getApplicationContext(), null, null)
    val perfEvent = LogTreePopulator.populatePerfEventFromLogger(noLogTagContext, logger, event)
    assertThat(perfEvent).isNull()
    assertThat(logger.canceledPerfEvents).containsExactly(event)
    verifyNoMoreInteractions(event)
  }

  @Test
  fun testNullTreePropLogger() {
    val logger: ComponentsLogger =
        object : TestComponentsLogger() {
          override fun getExtraAnnotations(
              treePropertyProvider: TreePropertyProvider
          ): Map<String, String>? = null
        }
    val event: PerfEvent = mock()
    context.treeProps = TreeProps().apply { put(MyKey::class.java, 1_337) }
    LogTreePopulator.populatePerfEventFromLogger(context, logger, event)
    verify(event).markerAnnotate("log_tag", "test")
    verifyNoMoreInteractions(event)
  }

  @Test
  fun testSkipNullPerfEvent() {
    val logger = TestComponentsLogger()
    assertThat(LogTreePopulator.populatePerfEventFromLogger(context, logger, null)).isNull()
  }

  private class MyKey
}
