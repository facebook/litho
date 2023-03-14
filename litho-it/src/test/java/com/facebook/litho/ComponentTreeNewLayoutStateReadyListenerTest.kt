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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentTreeNewLayoutStateReadyListenerTest {

  private lateinit var context: ComponentContext
  private lateinit var component: Component
  private lateinit var componentTree: ComponentTree
  private lateinit var resolveThreadShadowLooper: ShadowLooper
  private lateinit var layoutThreadShadowLooper: ShadowLooper
  private lateinit var listener: ComponentTree.NewLayoutStateReadyListener
  private var widthSpec = 0
  private var widthSpec2 = 0
  private var heightSpec = 0
  private var heightSpec2 = 0

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
    listener = mock()
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
  fun testListenerInvokedForSetRoot() {
    componentTree.newLayoutStateReadyListener = listener
    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec)
    verify(listener).onNewLayoutStateReady(componentTree)
  }

  @Test
  fun testListenerInvokedForSetRootAsync() {
    componentTree.newLayoutStateReadyListener = listener
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)
    verify(listener, never()).onNewLayoutStateReady(anyOrNull<ComponentTree>())

    // Now the background thread run the queued task.
    runToEndOfTasks()
    verify(listener).onNewLayoutStateReady(componentTree)
  }

  @Test
  fun testListenerInvokedOnlyOnceForMultipleSetRootAsync() {
    componentTree.newLayoutStateReadyListener = listener
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)
    componentTree.setSizeSpecAsync(widthSpec2, heightSpec2)
    verify(listener, never()).onNewLayoutStateReady(anyOrNull<ComponentTree>())

    // Now the background thread run the queued task.
    runToEndOfTasks()
    verify(listener).onNewLayoutStateReady(componentTree)
  }

  @Test
  fun testListenerInvokedForSetRootAsyncWhenAttached() {
    componentTree.newLayoutStateReadyListener = listener
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    verify(listener, never()).onNewLayoutStateReady(anyOrNull<ComponentTree>())

    // Now the background thread run the queued task.
    runToEndOfTasks()
    verify(listener).onNewLayoutStateReady(componentTree)
  }

  @Test
  fun testListenerInvokedForMeasure() {
    componentTree.newLayoutStateReadyListener = listener
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    componentTree.setSizeSpec(widthSpec, heightSpec)
    verify(listener).onNewLayoutStateReady(componentTree)
    reset(listener)
    componentTree.measure(widthSpec2, heightSpec2, intArrayOf(0, 0), false)
    verify(listener).onNewLayoutStateReady(componentTree)
  }

  @Test
  fun testListenerNotInvokedWhenMeasureDoesntComputeALayout() {
    componentTree.newLayoutStateReadyListener = listener
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    componentTree.setSizeSpec(widthSpec, heightSpec)
    verify(listener).onNewLayoutStateReady(componentTree)
    reset(listener)
    componentTree.measure(widthSpec, heightSpec, intArrayOf(0, 0), false)
    verify(listener, never()).onNewLayoutStateReady(componentTree)
  }

  @Test
  fun testListenerNotInvokedWhenNewMeasureSpecsAreCompatible() {
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    componentTree.setSizeSpec(widthSpec, heightSpec)
    componentTree.newLayoutStateReadyListener = listener
    componentTree.setSizeSpec(widthSpec, heightSpec)
    verify(listener, never()).onNewLayoutStateReady(componentTree)
  }
}
