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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.ResolveCancellationStrategy
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.TimeOutSemaphore
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ResolveCancellationTest {

  @get:Rule val lithoViewRule: LithoViewRule = LithoViewRule()

  @get:Rule
  val backgroundLayoutLooperRule: BackgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  /**
   * This tests describes a scenario where we have an on-going *Resolve* running synchronously in a
   * Thread when a new equivalent *Resolve* is requested.
   *
   * In this case, the incoming *Resolve* should be dropped, as an equivalent is already running in
   * a synchronous way, causing it to be redundant.
   */
  @Test
  fun `should not process an incoming redundant resolve request`() {
    // as we are not cancelling any running resolve on this test, the cancellation mode doesn't
    // matter
    val lithoView = createEmptyLithoView(ResolveCancellationStrategy.DEFAULT_SHORT_CIRCUIT)

    val controller = ResolveTestController(lithoView.componentTree)
    val testComponent = controller.newTestComponent()

    // start a synchronous Resolve in a background thread (resolve-#1)
    val resolve1 = controller.createResolveOrchestrator(version = 1, blockRender = true)

    runOnBackgroundThread { lithoView.setRoot(testComponent) }

    resolve1.waitForStart()

    // at this point, resolve-#1 should have started, but should be blocked (not finished)
    assertThat(resolve1.hasStarted).isTrue
    assertThat(resolve1.hasFinished).isFalse

    // at this moment we set the same root (which is resolve-equivalent); this will start
    // a new resolve (resolve-#2) because we have no committed resolve result yet.
    val resolve2 = controller.createResolveOrchestrator(version = 2, blockRender = false)
    lithoView.setRootAsync(testComponent)
    lithoViewRule.idle()

    // by now, resolve-#2 should have dropped and never started
    assertThat(resolve2.hasStarted).isFalse

    // we release the block we had in resolve-#1 so we can let it proceed and finish
    resolve1.release()
    resolve1.waitForEnd()

    assertThat(resolve1.hasFinished).isTrue
  }

  /**
   * This tests describes a scenario where we have an on-going *Resolve* running in the
   * *ComponentLayoutThread* when a new non-equivalent *Resolve* is requested.
   *
   * In this case, the incoming *Resolve* will effectively start a process that generates a
   * different *ResolveResult*. For this reason, we can cancel the running request, as it is
   * outdated, since we will process a new one that is different. In this test, the cancellation
   * execution is done by marking the TreeFuture as released.
   */
  @Test
  fun `should cancel a running resolve when an incoming resolve makes it redundant - with short circuit`() {
    testCancellingRunningResolve(ResolveCancellationStrategy.DEFAULT_SHORT_CIRCUIT)
  }

  /**
   * This tests describes a scenario where we have an on-going *Resolve* running in the
   * *ComponentLayoutThread* when a new non-equivalent *Resolve* is requested.
   *
   * In this case, the incoming *Resolve* will effectively start a process that generates a
   * different *ResolveResult*. For this reason, we can cancel the running request, as it is
   * outdated, since we will process a new one that is different.
   *
   * In this test, the cancellation execution is done by interrupting the Future using
   * `Future#cancel` APIs.
   */
  @Test
  fun `should cancel a running resolve when an incoming resolve makes it redundant - with interruption`() {
    testCancellingRunningResolve(ResolveCancellationStrategy.DEFAULT_INTERRUPT)
  }

  private fun testCancellingRunningResolve(cancellationExecutionMode: ResolveCancellationStrategy) {
    val lithoView = createEmptyLithoView(cancellationExecutionMode)

    val controller = ResolveTestController(lithoView.componentTree)
    val testComponent = controller.newTestComponent()

    // start an asynchronous resolve that will be blocked during the execution (resolve-#1)
    val resolve1 = controller.createResolveOrchestrator(version = 1, blockRender = true)
    lithoView.setRootAsync(testComponent)
    backgroundLayoutLooperRule.runToEndOfTasksAsync()

    // wait until resolve-#1 has started
    resolve1.waitForStart()

    // at this time, resolve-#1 should have started, but not finished because it is blocked
    assertThat(resolve1.hasStarted).isTrue
    assertThat(resolve1.hasFinished).isFalse

    // start a synchronous non-equivalent resolve (resolve-#2). in this case, it is not equivalent
    // because we instantiate a new version of the component (changing its id)
    val resolve2 = controller.createResolveOrchestrator(version = 2, blockRender = false)
    lithoView.setRoot(controller.newTestComponent())

    // as soon as the synchronous operation ends, resolve-#2 should have finished, increasing the
    // resolve counts
    assertThat(resolve2.hasFinished).isTrue

    // we release the blocking in resolve-#1 (which should have no effect, because it was already
    // cancelled)
    resolve1.release()
    lithoViewRule.idle()

    // the resolve statuses should stay the same
    assertThat(resolve1.hasFinished).isFalse
    assertThat(resolve2.hasFinished).isTrue
  }

  private fun runOnBackgroundThread(body: () -> Unit) {
    val thread = Thread { body() }
    thread.start()
  }

  private fun createEmptyLithoView(
      cancellationStrategy: ResolveCancellationStrategy
  ): TestLithoView {
    val context = getApplicationContext<Context>()
    val componentContext = ComponentContext(context)

    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().resolveCancellationStrategy(cancellationStrategy))

    val componentTree =
        ComponentTree.create(componentContext)
            .componentsConfiguration(ComponentsConfiguration.getDefaultComponentsConfiguration())
            .build()

    val lithoView =
        lithoViewRule.createTestLithoView(componentTree = componentTree) { EmptyComponent() }
    lithoView.attachToWindow().measure().layout()
    return lithoView
  }
}

/**
 * This is a helper that allows you to control the execution of a *Resolve* phase. The intendent
 * usage is as follows:
 * 1. Create a new [ResolveOrchestrator] by using [createResolveOrchestrator]. You can specify if
 *    the resolve should be blocked by specifying it on the `blockRender` param. Later you can
 *    unblock the execution if needed by calling [ResolveOrchestrator.release].
 * 2. Right after, you should start a resolve phase (e.g., re-setting a root) by using an instance
 *    of [TestController.TestComponent]. This components will get the last created
 *    [ResolveOrchestrator] and will wire into the blocking mechanisms.
 *
 * As a client, once you have an instance of an [ResolveOrchestrator], you can wait for it to start,
 * finish or release any lock.
 */
private class ResolveTestController(componentTree: ComponentTree) {

  private val futureExecutionListener =
      object : TreeFuture.FutureExecutionListener {

        override fun onPreExecution(
            version: Int,
            futureExecutionType: TreeFuture.FutureExecutionType?,
            attribution: String?
        ) =
            applyOnResolve(attribution) {
              val orchestrator = orchestrators[version] ?: error("Should not be null")
              orchestrator.onStart()
            }

        override fun onPostExecution(version: Int, released: Boolean, attribution: String?) =
            applyOnResolve(attribution) {
              if (!released) {
                val orchestrator = orchestrators[version] ?: error("Should not be null")
                orchestrator.onEnd()
              }
            }

        fun applyOnResolve(attribution: String?, body: () -> Unit) {
          if (attribution == "resolve") {
            body()
          }
        }
      }

  private val orchestrators = LinkedHashMap<Int, ResolveOrchestrator>()

  init {
    componentTree.setFutureExecutionListener(futureExecutionListener)
  }

  fun createResolveOrchestrator(version: Int, blockRender: Boolean): ResolveOrchestrator {
    val orchestrator = ResolveOrchestrator(version, blockRender)
    orchestrators[version] = orchestrator
    return orchestrator
  }

  fun newTestComponent(): Component = TestComponent()

  private inner class TestComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      val orchestrator = orchestrators.values.last()
      orchestrator.onStart()

      return Row { child(Text("Hello")) }.also { orchestrator.onEnd() }
    }
  }

  inner class ResolveOrchestrator(val version: Int, private val blockResolve: Boolean) {

    private val resolveStartLatch = CountDownLatch(1)
    private val resolveEndLatch = CountDownLatch(1)
    private val blockSemaphore = TimeOutSemaphore(1).apply { acquire() }

    val hasStarted: Boolean
      get() = (resolveStartLatch.count == 0L)

    val hasFinished: Boolean
      get() = (resolveEndLatch.count == 0L)

    fun waitForStart() {
      resolveStartLatch.await()
    }

    fun waitForEnd() {
      resolveEndLatch.await()
    }

    fun release() {
      blockSemaphore.release()
    }

    internal fun onStart() {
      resolveStartLatch.countDown()

      if (blockResolve) {
        blockSemaphore.acquire()
      }
    }

    internal fun onEnd() {
      resolveEndLatch.countDown()
    }
  }
}
