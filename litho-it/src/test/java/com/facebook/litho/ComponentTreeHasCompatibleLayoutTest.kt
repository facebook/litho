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
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.SizeSpec.EXACTLY
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentTreeHasCompatibleLayoutTest {

  private lateinit var context: ComponentContext
  private lateinit var component: Component
  private lateinit var componentTree: ComponentTree
  private lateinit var resolveThreadShadowLooper: ShadowLooper
  private lateinit var layoutThreadShadowLooper: ShadowLooper
  private var widthSpec: Int = 0
  private var widthSpec2: Int = 0
  private var heightSpec: Int = 0
  private var heightSpec2: Int = 0

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    component = SimpleMountSpecTester.create(context).build()
    componentTree = ComponentTree.create(context, component).build()
    resolveThreadShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultResolveThreadLooper")
                as Looper)
    layoutThreadShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultLayoutThreadLooper")
                as Looper)
    widthSpec = makeSizeSpec(39, EXACTLY)
    widthSpec2 = makeSizeSpec(40, EXACTLY)
    heightSpec = makeSizeSpec(41, EXACTLY)
    heightSpec2 = makeSizeSpec(42, EXACTLY)
  }

  private fun runToEndOfTasks() {
    resolveThreadShadowLooper.runToEndOfTasks()
    layoutThreadShadowLooper.runToEndOfTasks()
  }

  @Test
  fun testNoLayoutComputed() {
    assertThat(componentTree.hasCompatibleLayout(widthSpec, heightSpec)).isFalse
  }

  @Test
  fun testMainThreadLayoutSet() {
    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec)
    assertThat(componentTree.hasCompatibleLayout(widthSpec, heightSpec)).isTrue
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isFalse
  }

  @Test
  fun testBackgroundLayoutSet() {
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)
    assertThat(componentTree.hasCompatibleLayout(widthSpec, heightSpec)).isFalse

    // Now the background thread run the queued task.
    runToEndOfTasks()
    assertThat(componentTree.hasCompatibleLayout(widthSpec, heightSpec)).isTrue
  }
}
