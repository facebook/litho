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

package com.facebook.litho.testing.api

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoView
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.BaseThreadLooperController
import com.facebook.litho.testing.ResolveAndLayoutThreadLooperController
import com.facebook.litho.testing.ThreadLooperController
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.Shadows.shadowOf

class LithoRule : TestRule, TestNodeSelectionProvider {

  private var componentContext: ComponentContext? = null
  private lateinit var testContext: TestContext

  private var threadLooperController: BaseThreadLooperController = ThreadLooperController()

  override fun apply(statement: Statement, description: Description): Statement =
      object : Statement() {
        override fun evaluate() {
          ensureThreadLooperType()

          try {
            componentContext = ComponentContext(getApplicationContext<Context>())
            threadLooperController.init()
            statement.evaluate()
          } finally {
            threadLooperController.clean()
          }
        }
      }

  fun render(componentProvider: () -> Component): LithoRule = also {
    setupLithoViewWithComponent(
        componentContext = checkNotNull(componentContext), componentProvider = componentProvider)
  }

  override fun selectNode(matcher: TestNodeMatcher): TestNodeSelection {
    idle()
    return TestNodeSelection(testContext = testContext, selector = testNodeSelector(matcher))
  }

  override fun selectNodes(matcher: TestNodeMatcher): TestNodeCollectionSelection {
    idle()
    return TestNodeCollectionSelection(
        testContext = testContext, selector = testNodeSelector(matcher))
  }

  private fun setupLithoViewWithComponent(
      componentContext: ComponentContext,
      componentProvider: () -> Component?
  ) {
    val lithoView = LithoView(componentContext)
    val componentTree = ComponentTree.create(componentContext).build()
    val component = componentProvider()

    testContext = LithoTestContext(lithoView)
    componentTree.setRoot(component)

    lithoView.componentTree = componentTree

    lithoView.onAttachedToWindowForTest()
    lithoView.measure(DEFAULT_WIDTH_SPEC, DEFAULT_HEIGHT_SPEC)
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
  }

  /**
   * Runs through all tasks on the background thread and main lopper, blocking until it completes.
   * Use if there are any async events triggered by layout ( ie visibility events) to manually drain
   * the queue
   */
  private fun idle() {
    threadLooperController.runToEndOfTasksSync()
    shadowOf(Looper.getMainLooper()).idle()
  }

  private fun ensureThreadLooperType() {
    if (ComponentsConfiguration.isSplitResolveAndLayoutWithSplitHandlers() &&
        threadLooperController is ThreadLooperController) {
      threadLooperController = ResolveAndLayoutThreadLooperController()
    } else if (!ComponentsConfiguration.isSplitResolveAndLayoutWithSplitHandlers() &&
        threadLooperController is ResolveAndLayoutThreadLooperController) {
      threadLooperController = ThreadLooperController()
    }
  }

  companion object {
    private val DEFAULT_WIDTH_SPEC: Int =
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
    private val DEFAULT_HEIGHT_SPEC: Int =
        View.MeasureSpec.makeMeasureSpec(2040, View.MeasureSpec.EXACTLY)
  }
}
