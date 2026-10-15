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

package com.facebook.litho.rangemanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.SizeSpec
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.rangemanager.LithoPreparationHealthEvent.MainThreadRender
import com.facebook.litho.rangemanager.LithoPreparationHealthEvent.MainThreadRender.Phase
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.meta.rangemanager.PreparationHealthEvent
import com.meta.rangemanager.PreparationHealthObserver
import com.meta.rangemanager.PreparationSource
import com.meta.rangemanager.UnprepareReason
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [LithoHealthEventSource] against a real [ComponentTree] on the bus, so the breadcrumb and events
 * come from Litho itself rather than from hand-built events.
 */
@RunWith(LithoTestRunner::class)
class LithoHealthEventSourceComponentTreeTest {

  private val reportedEvents = mutableListOf<Pair<Any, PreparationHealthEvent>>()

  private val observer =
      object : PreparationHealthObserver {
        override fun onItemRegistered(key: Any) = Unit

        override fun onItemPrepared(key: Any, source: PreparationSource) = Unit

        override fun onItemBound(key: Any, wasPrepared: Boolean) = Unit

        override fun onItemUnprepared(key: Any, reason: UnprepareReason) = Unit

        override fun onFirstScroll() = Unit

        override fun reportEvent(key: Any, event: PreparationHealthEvent) {
          reportedEvents.add(key to event)
        }
      }

  private val context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())

  private val source = LithoHealthEventSource(observer) { it }

  @Before
  fun setUp() {
    source.start()
  }

  @After
  fun tearDown() {
    source.stop()
  }

  @Test
  fun `reports a sync render on main once, under the tree's breadcrumb`() {
    val tree = treeWithBreadcrumb(BREADCRUMB)

    tree.setRootAndSizeSpecSync(root(), exactly(100), exactly(100))

    assertThat(reportedEvents).containsExactly(BREADCRUMB to MainThreadRender("setRootSync"))
  }

  @Test
  fun `reports a layout-only render on main when the same root gets a new size`() {
    val tree = treeWithBreadcrumb(BREADCRUMB)
    val root = root()
    tree.setRootAndSizeSpecSync(root, exactly(100), exactly(100))
    reportedEvents.clear()

    tree.setRootAndSizeSpecSync(root, exactly(200), exactly(100))

    assertThat(reportedEvents)
        .containsExactly(BREADCRUMB to MainThreadRender("setRootSync", Phase.LAYOUT))
  }

  private fun treeWithBreadcrumb(breadcrumb: String): ComponentTree =
      ComponentTree.create(context).build().also { it.setDebugLogsBreadcrumb(breadcrumb) }

  private fun root(): Component = Row()

  private fun exactly(px: Int): Int = SizeSpec.makeSizeSpec(px, SizeSpec.EXACTLY)

  private companion object {
    const val BREADCRUMB = "binder/key"
  }
}
