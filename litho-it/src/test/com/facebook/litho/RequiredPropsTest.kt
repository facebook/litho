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
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import java.lang.IllegalStateException
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class RequiredPropsTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testWithoutRequiredProps() {
    var error = ""
    try {
      Text.create(context).build()
    } catch (e: IllegalStateException) {
      error = e.message ?: ""
    }
    assertTrue("Error message did not mention the missing required prop", error.contains("text"))
  }

  @Test
  fun testWithRequiredProps() {
    Text.create(context).text("text").build()
  }
}
