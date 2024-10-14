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
import com.facebook.litho.testing.subcomponents.InspectableComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.IsNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class BaseMatcherTest {

  @Mock var inspectableComponent: InspectableComponent? = null

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    whenever(inspectableComponent!!.component)
        .thenReturn(
            Row.create(ComponentContext(ApplicationProvider.getApplicationContext<Context>()))
                .wrapInView()
                .build())
  }

  @Test
  fun testMatcherCreation() {
    val matcher = TestBaseMatcher().clickHandler(IsNull.nullValue(null))
    val condition = BaseMatcherBuilder.buildCommonMatcher(matcher)
    assertThat(condition.matches(inspectableComponent)).isTrue
  }

  @Test
  fun testMatcherFailureMessage() {
    val matcher = TestBaseMatcher().clickHandler(IsNull.notNullValue(null))
    val condition = BaseMatcherBuilder.buildCommonMatcher(matcher)
    condition.matches(inspectableComponent)
    assertThat(condition.description().toString())
        .isEqualTo("Click handler <not null> (doesn't match <null>)")
  }

  internal class TestBaseMatcher : BaseMatcher<TestBaseMatcher?>() {
    override fun getThis(): TestBaseMatcher = this
  }
}
