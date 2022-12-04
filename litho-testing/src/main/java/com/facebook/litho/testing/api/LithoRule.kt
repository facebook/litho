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
import android.view.View
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoView
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class LithoRule : TestRule, TestNodeSelectionProvider {

  private var componentContext: ComponentContext? = null
  private lateinit var testContext: TestContext

  override fun apply(statement: Statement, description: Description): Statement =
      object : Statement() {
        override fun evaluate() {
          componentContext = ComponentContext(getApplicationContext<Context>())
          statement.evaluate()
        }
      }

  fun render(componentProvider: () -> Component): LithoRule = also {
    setupLithoViewWithComponent(
        componentContext = checkNotNull(componentContext), componentProvider = componentProvider)
  }

  override fun selectNode(matcher: TestNodeMatcher): TestNodeSelection {
    return TestNodeSelection(testContext = testContext, selector = testNodeSelector(matcher))
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

  companion object {
    private val DEFAULT_WIDTH_SPEC: Int =
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
    private val DEFAULT_HEIGHT_SPEC: Int =
        View.MeasureSpec.makeMeasureSpec(2040, View.MeasureSpec.EXACTLY)
  }
}
