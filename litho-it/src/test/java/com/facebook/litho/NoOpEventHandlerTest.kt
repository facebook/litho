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
import com.facebook.litho.NoOpEventHandler.Companion.getNoOpEventHandler
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.lang.RuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class NoOpEventHandlerTest {

  @Test
  fun testGetNoOpEventHandler() {
    val eventHandler: EventHandler<*> = getNoOpEventHandler<Any>()
    assertThat(eventHandler.isEquivalentTo(null)).isFalse
    val eventHandler1: EventHandler<*> = getNoOpEventHandler<Any>()
    assertThat(eventHandler.isEquivalentTo(eventHandler1)).isTrue
  }

  @Test
  fun testHasEventDispatcherNotNull() {
    val eventHandler: NoOpEventHandler<*> = getNoOpEventHandler<Any>()
    assertThat(eventHandler.isEquivalentTo(null)).isFalse
    assertThat(getNoOpEventHandler<Any>().dispatchInfo.hasEventDispatcher != null).isTrue
  }

  @Test(expected = RuntimeException::class)
  fun testComponentContextThrowsExceptionWithoutComponentScope() {
    val componentContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    assertThat(
            componentContext
                .newEventHandler<Any>(1, arrayOfNulls(1))
                .isEquivalentTo(getNoOpEventHandler<Any>()))
        .isTrue
  }

  @Test(expected = RuntimeException::class)
  fun testComponentLifeCycleThrowsExceptionWithoutComponentScope() {
    val componentContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val component: Component? = null
    assertThat(
            Component.newEventHandler<Any>(
                    component!!.javaClass, "Component", componentContext, 1, arrayOfNulls(1))
                .isEquivalentTo(getNoOpEventHandler<Any>()))
        .isTrue
  }
}
