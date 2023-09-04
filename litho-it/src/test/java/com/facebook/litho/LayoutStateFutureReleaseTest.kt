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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.ThreadTestingUtils
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LayoutStateFutureReleaseTest {

  private var widthSpec = 0
  private var heightSpec = 0
  private lateinit var context: ComponentContext
  private val config =
      ComponentsConfiguration.getDefaultComponentsConfiguration().useCancelableLayoutFutures
  private lateinit var resolveThreadShadowLooper: ShadowLooper
  private lateinit var layoutThreadShadowLooper: ShadowLooper

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.getDefaultComponentsConfigurationBuilder()
            .useCancelableLayoutFutures(true))
    widthSpec = makeSizeSpec(40, EXACTLY)
    heightSpec = makeSizeSpec(40, EXACTLY)
    layoutThreadShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultLayoutThreadLooper")
                as Looper)
    resolveThreadShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultResolveThreadLooper")
                as Looper)
  }

  private fun runToEndOfTasks() {
    resolveThreadShadowLooper.runToEndOfTasks()
    layoutThreadShadowLooper.runToEndOfTasks()
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.getDefaultComponentsConfigurationBuilder()
            .useCancelableLayoutFutures(config))
  }

  @Test
  fun testStopResolvingRowChildrenIfLsfReleased() {
    val layoutStateFutureMock: TreeFuture<*> = mock { on { isReleased } doReturn false }
    val c = ComponentContext(context)
    val resolveContext = c.setRenderStateContextForTests()
    resolveContext.setLayoutStateFutureForTest(layoutStateFutureMock)
    val wait = CountDownLatch(1)
    val child1 =
        TestChildComponent(
            wait,
            null,
            object : WaitActions {
              override fun unblock(layoutStateFuture: TreeFuture<*>?) {
                doReturn(true).`when`(layoutStateFutureMock).isReleased
              }
            })
    val child2 = TestChildComponent()
    val row = Row.create(context).child(child1).child(child2).build()
    val result = row.resolve(resolveContext, c)
    Assert.assertTrue(child1.hasRunLayout)
    Assert.assertFalse(child2.hasRunLayout)
    Assert.assertNull(result)
  }

  @Test
  fun testStopResolvingColumnChildrenIfLsfReleased() {
    val layoutStateFutureMock: TreeFuture<*> = mock()
    val c = ComponentContext(context)
    val resolveContext = c.setRenderStateContextForTests()
    resolveContext.setLayoutStateFutureForTest(layoutStateFutureMock)
    val wait = CountDownLatch(1)
    val child1 =
        TestChildComponent(
            wait,
            null,
            object : WaitActions {
              override fun unblock(layoutStateFuture: TreeFuture<*>?) {
                doReturn(true).`when`(layoutStateFutureMock).isReleased
              }
            })
    val child2 = TestChildComponent()
    val column = Column.create(context).child(child1).child(child2).build()
    val result = column.resolve(resolveContext, c)
    Assert.assertTrue(child1.hasRunLayout)
    Assert.assertFalse(child2.hasRunLayout)
    Assert.assertNull(result)
  }

  // This test is similar to testMainWaitingOnBgBeforeRelease, except that the bg thread
  // LayoutStateFuture gets released after the sync layout is triggered. In this case the UI thread
  // should not be blocked on the bg thread anymore, because the released Lsf will return a null
  // LayoutState.
  @Test
  fun testDontWaitOnReleasedLSF() {
    val layoutStateFutures = arrayOfNulls<TreeFuture<*>?>(2)
    val waitBeforeAsserts = CountDownLatch(1)
    val scheduleSyncLayout = CountDownLatch(1)
    val finishBgLayout = CountDownLatch(1)
    val componentTree: ComponentTree
    val child1 =
        TestChildComponent(
            // Testing scenario: we schedule a LSF on bg thread which gets released before compat UI
            // thread
            // layout is scheduled.
            waitActions =
                object : WaitActions {
                  override fun unblock(layoutStateFuture: TreeFuture<*>?) {
                    // Something happens here which releases the ongoing lsf, such as a state update
                    // triggered from onCreateLayout.
                    if (layoutStateFutures[0] == null) {
                      layoutStateFutures[0] = layoutStateFuture
                      layoutStateFuture?.release()
                      scheduleSyncLayout.countDown()
                      ThreadTestingUtils.failSilentlyIfInterrupted {
                        finishBgLayout.await(5000, TimeUnit.MILLISECONDS)
                      }
                      waitBeforeAsserts.countDown()
                    } else {
                      layoutStateFutures[1] = layoutStateFuture
                      finishBgLayout.countDown()
                    }
                  }
                })
    val column_0 = Column.create(context).child(TestChildComponent()).build()
    val column = Column.create(context).child(child1).build()
    val handler = ThreadPoolLayoutHandler.getNewInstance(LayoutThreadPoolConfigurationImpl(1, 1, 5))
    componentTree =
        ComponentTree.create(context, column_0)
            .componentsConfiguration(
                ComponentsConfiguration.create().useCancelableLayoutFutures(true).build())
            .layoutThreadHandler(handler)
            .build()
    componentTree.setLithoView(LithoView(context))

    componentTree.setRootAndSizeSpecAsync(column, widthSpec, heightSpec)
    runToEndOfTasks()
    ThreadTestingUtils.failSilentlyIfInterrupted {
      scheduleSyncLayout.await(5000, TimeUnit.MILLISECONDS)
    }
    componentTree.setRootAndSizeSpecSync(column, widthSpec, heightSpec, Size())
    ThreadTestingUtils.failSilentlyIfInterrupted {
      waitBeforeAsserts.await(5000, TimeUnit.MILLISECONDS)
    }
    Assert.assertNotNull(layoutStateFutures[0])
    Assert.assertNotNull(layoutStateFutures[1])
  }

  private fun createTestStateUpdate(): StateContainer.StateUpdate {
    return StateContainer.StateUpdate(0)
  }

  private interface WaitActions {
    fun unblock(layoutStateFuture: TreeFuture<*>?)
  }

  private class TestChildComponent
  @JvmOverloads
  constructor(
      private val wait: CountDownLatch? = null,
      private val unlockFinishedLayout: CountDownLatch? = null,
      val waitActions: WaitActions? = null
  ) : SpecGeneratedComponent("TestChildComponent") {
    private val layoutStateFutureList: MutableList<TreeFuture<*>?>
    var hasRunLayout = false

    override fun render(
        resolveContext: ResolveContext,
        c: ComponentContext,
        widthSpec: Int,
        heightSpec: Int
    ): RenderResult {
      waitActions?.unblock(c.layoutStateFuture)
      if (wait != null) {
        ThreadTestingUtils.failSilentlyIfInterrupted { wait.await(5000, TimeUnit.MILLISECONDS) }
      }
      hasRunLayout = true
      layoutStateFutureList.add(c.layoutStateFuture)
      unlockFinishedLayout?.countDown()
      return RenderResult(Column.create(c).build())
    }

    init {
      layoutStateFutureList = ArrayList()
    }
  }
}
