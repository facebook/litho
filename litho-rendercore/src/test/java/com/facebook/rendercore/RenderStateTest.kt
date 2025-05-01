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

package com.facebook.rendercore

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.facebook.rendercore.RenderState.ResolveFunc
import com.facebook.rendercore.StateUpdateReceiver.StateUpdate
import com.facebook.rendercore.testing.TestNode
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class RenderStateTest {
  private val emptyDelegate: RenderState.Delegate<Any?> =
      object : RenderState.Delegate<Any?> {
        override fun commit(
            layoutVersion: Int,
            current: RenderTree?,
            next: RenderTree,
            currentState: Any?,
            nextState: Any?
        ) = Unit

        override fun commitToUI(tree: RenderTree?, state: Any?, frameId: Int) = Unit
      }

  @Test
  fun testSettingTreeAsyncResolvesOnTheExecutor() {
    val wasExecuteCalled = AtomicBoolean()
    val wasCalledInExecute = AtomicBoolean()
    val executing = AtomicBoolean()

    val resolveExecutor = Executor { runnable ->
      wasExecuteCalled.set(true)
      executing.set(true)
      runnable.run()
      executing.set(false)
    }
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(
            RuntimeEnvironment.application,
            object : RenderState.Delegate<Any?> {
              override fun commit(
                  layoutVersion: Int,
                  current: RenderTree?,
                  next: RenderTree,
                  currentState: Any?,
                  nextState: Any?
              ) = Unit

              override fun commitToUI(tree: RenderTree?, state: Any?, frameId: Int) = Unit
            },
            null,
            null,
            resolveExecutor)
    renderState.setTreeAsync { _, _, _, _ ->
      wasCalledInExecute.set(executing.get())
      ResolveResult(TestNode(), null)
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertThat(wasExecuteCalled.get()).isTrue
    assertThat(wasCalledInExecute.get()).isTrue
  }

  @Test
  fun testCommitWhileDetached() {
    val state = Any()
    val wasCalled = AtomicBoolean()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(
            RuntimeEnvironment.application,
            object : RenderState.Delegate<Any?> {
              override fun commit(
                  layoutVersion: Int,
                  current: RenderTree?,
                  next: RenderTree,
                  currentState: Any?,
                  nextState: Any?
              ) {
                assertThat(current).isNull()
                assertThat(next).isNotNull
                assertThat(currentState).isNull()
                assertThat(nextState).isNotNull
                wasCalled.set(true)
              }

              override fun commitToUI(tree: RenderTree?, state: Any?, frameId: Int) = Unit
            },
            null,
            null)
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      ResolveResult(TestNode(), state)
    }
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    assertThat(wasCalled.get()).isTrue
  }

  @Test
  fun testReturningSameNodeSkipsLayoutIfMeasuresAreCompatible() {
    val state = Any()
    val secondState = Any()
    val layoutCount = AtomicInteger()
    val renderTreeCount = AtomicInteger()
    val sizeConstraints100 = SizeConstraints.exact(100, 100)
    val sizeConstraints200 = SizeConstraints.exact(200, 200)
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(
            RuntimeEnvironment.application,
            object : RenderState.Delegate<Any?> {
              override fun commit(
                  layoutVersion: Int,
                  current: RenderTree?,
                  next: RenderTree,
                  currentState: Any?,
                  nextState: Any?
              ) {
                if (current != next) {
                  renderTreeCount.incrementAndGet()
                }
              }

              override fun commitToUI(tree: RenderTree?, state: Any?, frameId: Int) = Unit
            },
            null,
            null)
    val nodeToReturn: Node<Any?> =
        object : TestNode() {
          override fun calculateLayout(
              context: LayoutContext<*>,
              sizeConstraints: SizeConstraints
          ): LayoutResult {
            layoutCount.incrementAndGet()
            return super.calculateLayout(context, sizeConstraints)
          }
        }
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      ResolveResult<Node<Any?>, Any?>(nodeToReturn, state)
    }
    renderState.measure(sizeConstraints100, IntArray(2))
    assertThat(layoutCount.toInt()).isEqualTo(1)
    assertThat(renderTreeCount.toInt()).isEqualTo(1)
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      ResolveResult(nodeToReturn, secondState)
    }
    assertThat(layoutCount.toInt()).isEqualTo(1)
    assertThat(renderTreeCount.toInt()).isEqualTo(1)
    renderState.measure(sizeConstraints100, IntArray(2))
    assertThat(layoutCount.toInt()).isEqualTo(1)
    assertThat(renderTreeCount.toInt()).isEqualTo(1)
    renderState.measure(sizeConstraints200, IntArray(2))
    assertThat(layoutCount.toInt()).isEqualTo(2)
    assertThat(renderTreeCount.toInt()).isEqualTo(2)
  }

  @Test
  fun testIncreasingLayoutVersion() {
    val state = Any()
    val secondState = Any()
    val firstLayoutVersion = AtomicInteger()
    val secondLayoutVersion = AtomicInteger()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(RuntimeEnvironment.application, emptyDelegate, null, null)
    renderState.setTree(
        object : ResolveFunc<Any?, Any?, TestStateUpdate> {
          override fun resolve(
              resolveContext: ResolveContext<Any?, TestStateUpdate>,
              committedTree: Node<Any?>?,
              committedState: Any?,
              stateUpdatesToApply: List<TestStateUpdate>
          ): ResolveResult<Node<Any?>, Any?> {
            return ResolveResult(
                object : TestNode() {
                  override fun calculateLayout(
                      context: LayoutContext<*>,
                      sizeConstraints: SizeConstraints
                  ): LayoutResult {
                    firstLayoutVersion.set(context.layoutVersion)
                    return super.calculateLayout(context, sizeConstraints)
                  }
                },
                state)
          }
        })
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    renderState.setTree(
        object : ResolveFunc<Any?, Any?, TestStateUpdate> {
          override fun resolve(
              resolveContext: ResolveContext<Any?, TestStateUpdate>,
              committedTree: Node<Any?>?,
              committedState: Any?,
              stateUpdatesToApply: List<TestStateUpdate>
          ): ResolveResult<Node<Any?>, Any?> {
            return ResolveResult(
                object : TestNode() {
                  override fun calculateLayout(
                      context: LayoutContext<*>,
                      sizeConstraints: SizeConstraints
                  ): LayoutResult {
                    secondLayoutVersion.set(context.layoutVersion)
                    return super.calculateLayout(context, sizeConstraints)
                  }
                },
                secondState)
          }
        })
    assertThat(secondLayoutVersion.toInt()).isEqualTo(firstLayoutVersion.toInt() + 1)
    renderState.measure(SizeConstraints.exact(200, 200), IntArray(2))
    assertThat(secondLayoutVersion.toInt()).isEqualTo(firstLayoutVersion.toInt() + 2)
  }

  @Test
  fun testRemeasure_doesnt_resolve_again() {
    val resolveCount = AtomicInteger(0)
    val layoutCount = AtomicInteger(0)
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(RuntimeEnvironment.application, emptyDelegate, null, null)
    renderState.setTree(
        object : ResolveFunc<Any?, Any?, TestStateUpdate> {
          override fun resolve(
              resolveContext: ResolveContext<Any?, TestStateUpdate>,
              committedTree: Node<Any?>?,
              committedState: Any?,
              stateUpdatesToApply: List<TestStateUpdate>
          ): ResolveResult<Node<Any?>, Any?> {
            resolveCount.incrementAndGet()
            return ResolveResult(
                object : TestNode() {
                  override fun calculateLayout(
                      context: LayoutContext<*>,
                      sizeConstraints: SizeConstraints
                  ): LayoutResult {
                    layoutCount.incrementAndGet()
                    return super.calculateLayout(context, sizeConstraints)
                  }
                },
                Any())
          }
        })
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    assertThat(resolveCount.toInt()).isEqualTo(1)
    assertThat(layoutCount.toInt()).isEqualTo(1)
    renderState.measure(SizeConstraints.exact(200, 200), IntArray(2))
    assertThat(resolveCount.toInt()).isEqualTo(1)
    assertThat(layoutCount.toInt()).isEqualTo(2)

    // Remeasureing with the same constraints doesn't "layout" again
    renderState.measure(SizeConstraints.exact(200, 200), IntArray(2))
    assertThat(resolveCount.toInt()).isEqualTo(1)
    assertThat(layoutCount.toInt()).isEqualTo(2)
  }

  @Test
  fun testNewStateWithSameTreeCommits() {
    val numberOfCommits = AtomicInteger(0)
    val numberOfUICommits = AtomicInteger(0)
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(
            RuntimeEnvironment.application,
            object : RenderState.Delegate<Any?> {
              override fun commit(
                  layoutVersion: Int,
                  current: RenderTree?,
                  next: RenderTree,
                  currentState: Any?,
                  nextState: Any?
              ) {
                numberOfCommits.incrementAndGet()
              }

              override fun commitToUI(tree: RenderTree?, state: Any?, frameId: Int) {
                numberOfUICommits.incrementAndGet()
              }
            },
            null,
            null)
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    val testNode: Node<Any?> = TestNode()
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      ResolveResult(testNode, Any())
    }
    assertThat(numberOfCommits.get()).isEqualTo(1)
    assertThat(numberOfUICommits.get()).isEqualTo(1)
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      ResolveResult(testNode, Any())
    }
    assertThat(numberOfCommits.get()).isEqualTo(2)
    assertThat(numberOfUICommits.get()).isEqualTo(2)
  }

  @Test
  fun testSameStateWithSameTreeCommits() {
    val numberOfCommits = AtomicInteger(0)
    val numberOfUICommits = AtomicInteger(0)
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(
            RuntimeEnvironment.application,
            object : RenderState.Delegate<Any?> {
              override fun commit(
                  layoutVersion: Int,
                  current: RenderTree?,
                  next: RenderTree,
                  currentState: Any?,
                  nextState: Any?
              ) {
                numberOfCommits.incrementAndGet()
              }

              override fun commitToUI(tree: RenderTree?, state: Any?, frameId: Int) {
                numberOfUICommits.incrementAndGet()
              }
            },
            null,
            null)
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    val testNode: Node<Any?> = TestNode()
    val state = Any()
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      ResolveResult(testNode, state)
    }
    assertThat(numberOfCommits.get()).isEqualTo(1)
    assertThat(numberOfUICommits.get()).isEqualTo(1)
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      ResolveResult(testNode, state)
    }
    assertThat(numberOfCommits.get()).isEqualTo(2)
    assertThat(numberOfUICommits.get()).isEqualTo(2)
  }

  @Test
  fun enqueueStateUpdate_whenTriggered_resolvesAgainWithStateUpdates() {
    val resolveCount = AtomicInteger(0)
    val appliedStateUpdates = AtomicReference<List<*>>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(RuntimeEnvironment.getApplication(), emptyDelegate, null, null)
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      appliedStateUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }
    assertThat(resolveCount.toInt()).isEqualTo(1)
    renderState.enqueueStateUpdateSync(TestStateUpdate())
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertThat(resolveCount.toInt()).isEqualTo(2)
    assertThat(appliedStateUpdates.get()).hasSize(1)
  }

  @Test
  fun `only applied state updates are cleared after commit`() {
    val pendingUpdates = AtomicReference<List<TestStateUpdate>>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(RuntimeEnvironment.getApplication(), emptyDelegate, null, null)
    renderState.setTree { _, _, _, stateUpdatesToApply ->
      pendingUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }

    val updateOne = TestStateUpdate()
    val updateTwo = TestStateUpdate()
    renderState.enqueueStateUpdateSync(updateOne)
    renderState.enqueueStateUpdateSync(updateTwo)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertThat(pendingUpdates.get()).hasSize(2)

    // consume the first update
    renderState.setTree { _, _, _, stateUpdatesToApply ->
      ResolveResult(TestNode(), Any(), appliedStateUpdates = listOf(stateUpdatesToApply[0]))
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    renderState.setTree { _, _, _, stateUpdatesToApply ->
      pendingUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any(), stateUpdatesToApply)
    }

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    // expect only update one to have been consumed
    assertThat(pendingUpdates.get()).hasSize(1)
    assertThat(pendingUpdates.get()).containsOnly(updateTwo)
  }

  @Test
  fun enqueueStateUpdate_whenTriggeredBeforeSetTree_appliesInResolveOnSetTree() {
    val resolveCount = AtomicInteger(0)
    val appliedStateUpdates = AtomicReference<List<*>>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(RuntimeEnvironment.getApplication(), emptyDelegate, null, null)
    renderState.enqueueStateUpdate(TestStateUpdate())
    assertThat(resolveCount.toInt()).isEqualTo(0)
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      appliedStateUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }
    assertThat(resolveCount.toInt()).isEqualTo(1)
    assertThat(appliedStateUpdates.get()).hasSize(1)
  }

  @Test
  fun enqueueStateUpdate_whenTriggeredManyTimesInARow_areBatchedAndResolveOnce() {
    val resolveCount = AtomicInteger(0)
    val appliedStateUpdates = AtomicReference<List<*>>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(RuntimeEnvironment.getApplication(), emptyDelegate, null, null)
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      appliedStateUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }

    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertThat(resolveCount.toInt()).isEqualTo(1)

    // Two state updates are triggered.
    renderState.enqueueStateUpdateSync(TestStateUpdate())
    renderState.enqueueStateUpdateSync(TestStateUpdate())
    assertThat(resolveCount.toInt()).isEqualTo(1)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    // But only one 'flushStateUpdates' is scheduled and executed.
    assertThat(resolveCount.toInt()).isEqualTo(2)
    assertThat(appliedStateUpdates.get()).hasSize(2)
  }

  @Test
  fun enqueueStateUpdate_whenSetTreeExecutesBeforeFlushStateUpdate_flushDoesNothing() {
    val resolveCount = AtomicInteger(0)
    val appliedStateUpdates = AtomicReference<List<*>>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(RuntimeEnvironment.getApplication(), emptyDelegate, null, null)
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      appliedStateUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertThat(resolveCount.toInt()).isEqualTo(1)
    renderState.enqueueStateUpdate(TestStateUpdate())

    // 'flushStateUpdates' is scheduled, but not executed yet.
    assertThat(resolveCount.toInt()).isEqualTo(1)
    assertThat(appliedStateUpdates.get()).isEmpty()
    renderState.setTree { resolveContext, committedTree, committedState, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      appliedStateUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }

    // Release the Looper to update the RenderState.
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    // 'setTree' triggered 'resolve' which applied 'pendingStateUpdates'.
    assertThat(resolveCount.toInt()).isEqualTo(2)
    assertThat(appliedStateUpdates.get()).hasSize(1)

    // 'resolve' was skipped, because 'pendingStateUpdates' are already empty.
    assertThat(resolveCount.toInt()).isEqualTo(2)
  }

  @Test
  fun setTreeAsync_resolvesOnResolveExecutor() {
    val resolveCount = AtomicInteger(0)
    val resolvedAsync = AtomicBoolean(false)
    val appliedStateUpdates = AtomicReference<List<*>>()
    val resolveThread: HandlerThread = HandlerThread("test_ResolveThread").also { it.start() }
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(
            RuntimeEnvironment.getApplication(),
            emptyDelegate,
            null,
            null,
            SerialTestExecutor(Handler(resolveThread.looper)))
    renderState.setTreeAsync { _, _, _, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      resolvedAsync.set(Thread.currentThread().name == "test_ResolveThread")
      appliedStateUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }

    Shadows.shadowOf(Looper.getMainLooper()).idle()
    Shadows.shadowOf(resolveThread.looper).idle()

    assertThat(resolveCount.toInt()).isEqualTo(1)
    assertThat(resolvedAsync.get()).isEqualTo(true)
  }

  @Test
  fun enqueueStateUpdateAsync_resolvesOnResolveExecutor() {
    val resolveCount = AtomicInteger(0)
    val resolvedAsync = AtomicBoolean(false)
    val appliedStateUpdates = AtomicReference<List<*>>()
    val resolveThread: HandlerThread = HandlerThread("test_ResolveThread").also { it.start() }
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(
            RuntimeEnvironment.getApplication(),
            emptyDelegate,
            null,
            null,
            SerialTestExecutor(Handler(resolveThread.looper)))
    renderState.setTreeAsync { _, _, _, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      resolvedAsync.set(Thread.currentThread().name == "test_ResolveThread")
      appliedStateUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }

    Shadows.shadowOf(Looper.getMainLooper()).idle()
    Shadows.shadowOf(resolveThread.looper).idle()

    assertThat(resolveCount.toInt()).isEqualTo(1)

    renderState.enqueueStateUpdate(TestStateUpdate())
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    Shadows.shadowOf(resolveThread.looper).idle()

    assertThat(resolveCount.toInt()).isEqualTo(2)
    assertThat(resolvedAsync.get()).isEqualTo(true)
  }

  @Test
  fun testSyncAsyncStateUpdateSequence() {
    val resolveCount = AtomicInteger(0)
    val resolvedAsync = AtomicBoolean(false)
    val appliedStateUpdates = AtomicReference<List<*>>()
    val resolveThread: HandlerThread = HandlerThread("test_ResolveThread").also { it.start() }
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(
            RuntimeEnvironment.getApplication(),
            emptyDelegate,
            null,
            null,
            SerialTestExecutor(Handler(resolveThread.looper)))
    renderState.setTreeAsync { _, _, _, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      resolvedAsync.set(Thread.currentThread().name == "test_ResolveThread")
      appliedStateUpdates.set(stateUpdatesToApply)
      ResolveResult(TestNode(), Any())
    }

    Shadows.shadowOf(Looper.getMainLooper()).idle()
    Shadows.shadowOf(resolveThread.looper).idle()

    assertThat(resolveCount.toInt()).isEqualTo(1)

    // sync -> async -> sync resolves on the main thread
    renderState.apply {
      enqueueStateUpdateSync(TestStateUpdate())
      enqueueStateUpdate(TestStateUpdate())
      enqueueStateUpdateSync(TestStateUpdate())
    }

    Shadows.shadowOf(Looper.getMainLooper()).idle()
    Shadows.shadowOf(resolveThread.looper).idle()

    assertThat(resolveCount.toInt()).isEqualTo(2)
    assertThat(resolvedAsync.get()).isEqualTo(false)

    // sync -> sync -> async resolves on the resolve thread
    renderState.apply {
      enqueueStateUpdateSync(TestStateUpdate())
      enqueueStateUpdateSync(TestStateUpdate())
      enqueueStateUpdate(TestStateUpdate())
    }

    Shadows.shadowOf(Looper.getMainLooper()).idle()
    Shadows.shadowOf(resolveThread.looper).idle()

    assertThat(resolveCount.toInt()).isEqualTo(3)
    assertThat(resolvedAsync.get()).isEqualTo(true)
  }

  internal class TestStateUpdate : StateUpdate<Any> {
    override fun update(o: Any): Any {
      return Unit
    }
  }

  internal class SerialTestExecutor(private val handler: Handler) : Executor {
    override fun execute(command: Runnable) {
      handler.post(command)
    }
  }
}
