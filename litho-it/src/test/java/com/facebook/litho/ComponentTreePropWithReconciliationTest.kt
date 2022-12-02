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
import com.facebook.litho.testing.logging.TestComponentsReporter
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.TreePropTestContainerComponent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentTreePropWithReconciliationTest {

  private lateinit var c: ComponentContext

  @Before
  fun setup() {
    val componentsReporter = TestComponentsReporter()
    c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    ComponentsReporter.provide(componentsReporter)
  }

  @Test
  fun test() {
    val component = TreePropTestContainerComponent.create(c).build()
    getLithoView(component)
  }

  private fun getLithoView(component: Component): LithoView {
    val lithoView = LithoView(c)
    val componentTree = ComponentTree.create(c, component).isReconciliationEnabled(true).build()
    lithoView.componentTree = componentTree
    lithoView.measure(unspecified(640), unspecified(480))
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return lithoView
  }
}
