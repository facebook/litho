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

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.facebook.rendercore.RenderState.ResolveFunc
import com.facebook.rendercore.StateUpdateReceiver.StateUpdate
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.thread.utils.ThreadUtils
import com.facebook.rendercore.thread.utils.instrumentation.FutureInstrumenter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RunnableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
            nextState: Any?,
        ) = Unit

        override fun commitToUI(
            tree: RenderTree,
            state: Any?,
            frameId: Int,
            renderTrace: RCRenderTrace?,
        ) = Unit
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
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) = Unit

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) = Unit
        },
        null,
        null,
        resolveExecutor,
    )
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
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) {
            assertThat(current).isNull()
            assertThat(next).isNotNull
            assertThat(currentState).isNull()
            assertThat(nextState).isNotNull
            wasCalled.set(true)
          }

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) = Unit
        },
        null,
        null,
    )
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
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) {
            if (current != next) {
              renderTreeCount.incrementAndGet()
            }
          }

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) = Unit
        },
        null,
        null,
    )
    val nodeToReturn: Node<Any?> =
        object : TestNode() {
          override fun calculateLayout(
              context: LayoutContext<*>,
              sizeConstraints: SizeConstraints,
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
              stateUpdateReceiver: StateUpdateReceiver<TestStateUpdate>,
              committedTree: Node<Any?>?,
              committedState: Any?,
              stateUpdatesToApply: List<TestStateUpdate>,
          ): ResolveResult<Node<Any?>, Any?> {
            return ResolveResult(
                object : TestNode() {
                  override fun calculateLayout(
                      context: LayoutContext<*>,
                      sizeConstraints: SizeConstraints,
                  ): LayoutResult {
                    firstLayoutVersion.set(context.layoutVersion)
                    return super.calculateLayout(context, sizeConstraints)
                  }
                },
                state,
            )
          }
        },
    )
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    renderState.setTree(
        object : ResolveFunc<Any?, Any?, TestStateUpdate> {
          override fun resolve(
              stateUpdateReceiver: StateUpdateReceiver<TestStateUpdate>,
              committedTree: Node<Any?>?,
              committedState: Any?,
              stateUpdatesToApply: List<TestStateUpdate>,
          ): ResolveResult<Node<Any?>, Any?> {
            return ResolveResult(
                object : TestNode() {
                  override fun calculateLayout(
                      context: LayoutContext<*>,
                      sizeConstraints: SizeConstraints,
                  ): LayoutResult {
                    secondLayoutVersion.set(context.layoutVersion)
                    return super.calculateLayout(context, sizeConstraints)
                  }
                },
                secondState,
            )
          }
        },
    )
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
              stateUpdateReceiver: StateUpdateReceiver<TestStateUpdate>,
              committedTree: Node<Any?>?,
              committedState: Any?,
              stateUpdatesToApply: List<TestStateUpdate>,
          ): ResolveResult<Node<Any?>, Any?> {
            resolveCount.incrementAndGet()
            return ResolveResult(
                object : TestNode() {
                  override fun calculateLayout(
                      context: LayoutContext<*>,
                      sizeConstraints: SizeConstraints,
                  ): LayoutResult {
                    layoutCount.incrementAndGet()
                    return super.calculateLayout(context, sizeConstraints)
                  }
                },
                Any(),
            )
          }
        },
    )
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
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) {
            numberOfCommits.incrementAndGet()
          }

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) {
            numberOfUICommits.incrementAndGet()
          }
        },
        null,
        null,
    )
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
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) {
            numberOfCommits.incrementAndGet()
          }

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) {
            numberOfUICommits.incrementAndGet()
          }
        },
        null,
        null,
    )
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
  fun `commitToUI when deferred waits for root host input commit`() {
    val committedStates = mutableListOf<Any?>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) = Unit

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) {
            committedStates.add(state)
          }
        },
        null,
        null,
        deferCommitToUIUntilMount = true,
    )
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    val state = Any()

    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), state) }

    assertThat(committedStates).isEmpty()
    val rootHostInput = renderState.uiRootHostInput
    assertThat(rootHostInput).isNotNull

    rootHostInput?.commitToUI()
    rootHostInput?.commitToUI()

    assertThat(committedStates).containsExactly(state)
  }

  @Test
  fun `commitToUI when deferred and mount input is unchanged commits immediately`() {
    val committedStates = mutableListOf<Any?>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) = Unit

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) {
            committedStates.add(state)
          }
        },
        null,
        null,
        deferCommitToUIUntilMount = true,
    )
    val node = TestNode()
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    val firstState = Any()
    val secondState = Any()

    renderState.setTree { _, _, _, _ -> ResolveResult(node, firstState) }
    val rootHostInput = renderState.uiRootHostInput
    rootHostInput?.commitToUI()
    renderState.setTree { _, _, _, _ -> ResolveResult(node, secondState) }

    assertThat(renderState.uiRootHostInput).isSameAs(rootHostInput)
    assertThat(committedStates).containsExactly(firstState, secondState)
  }

  @Test
  fun `commitToUI when deferred and pending input is reused updates pending commit`() {
    val committedStates = mutableListOf<Any?>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) = Unit

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) {
            committedStates.add(state)
          }
        },
        null,
        null,
        deferCommitToUIUntilMount = true,
    )
    val node = TestNode()
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    val firstState = Any()
    val secondState = Any()

    renderState.setTree { _, _, _, _ -> ResolveResult(node, firstState) }
    val firstRootHostInput = renderState.uiRootHostInput
    renderState.setTree { _, _, _, _ -> ResolveResult(node, secondState) }
    val secondRootHostInput = renderState.uiRootHostInput

    assertThat(secondRootHostInput).isSameAs(firstRootHostInput)
    secondRootHostInput?.commitToUI()
    firstRootHostInput?.commitToUI()

    assertThat(committedStates).containsExactly(secondState)
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
  fun stateUpdateReceiver_isStableAndStopsForwardingAfterDestroy() {
    val resolveCount = AtomicInteger(0)
    val capturedReceiver = AtomicReference<StateUpdateReceiver<TestStateUpdate>>()
    val renderState: RenderState<Any?, Any?, TestStateUpdate> =
        RenderState(RuntimeEnvironment.getApplication(), emptyDelegate, null, null)
    renderState.setTree { stateUpdateReceiver, _, _, stateUpdatesToApply ->
      resolveCount.incrementAndGet()
      capturedReceiver.updateAndGet { it ?: stateUpdateReceiver }
      assertThat(stateUpdateReceiver).isSameAs(capturedReceiver.get())
      ResolveResult(TestNode(), Any(), stateUpdatesToApply)
    }

    capturedReceiver.get().enqueueStateUpdateSync(TestStateUpdate())
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertThat(resolveCount.toInt()).isEqualTo(2)

    renderState.destroy()
    capturedReceiver.get().enqueueStateUpdateSync(TestStateUpdate())
    renderState.enqueueStateUpdateSync(TestStateUpdate())
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertThat(resolveCount.toInt()).isEqualTo(2)
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
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.getApplication(),
        emptyDelegate,
        null,
        null,
        SerialTestExecutor(Handler(resolveThread.looper)),
    )
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
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.getApplication(),
        emptyDelegate,
        null,
        null,
        SerialTestExecutor(Handler(resolveThread.looper)),
    )
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
    val renderState: RenderState<Any?, Any?, TestStateUpdate> = RenderState(
        RuntimeEnvironment.getApplication(),
        emptyDelegate,
        null,
        null,
        SerialTestExecutor(Handler(resolveThread.looper)),
    )
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

  @Test
  fun `trace progresses through running, committed, and committed-to-ui in immediate mode`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        instrumenter = instrumenter,
    )

    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

    assertThat(instrumenter.begunTraces).hasSize(1)
    val traceId = instrumenter.begunTraces.single().traceId
    assertThat(instrumenter.pointNames(traceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_RESOLVE_START,
            RenderTraceManager.POINT_RESOLVE_END,
            RenderTraceManager.POINT_LAYOUT_START,
            RenderTraceManager.POINT_LAYOUT_END,
            RenderTraceManager.POINT_TRACE_COMMITTED,
            RenderTraceManager.POINT_TRACE_COMMITTED_TO_UI,
        )
    assertThat(instrumenter.cancelled).isEmpty()
  }

  @Test
  fun `trace progresses through pending-commit-to-ui in deferred mode`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        deferCommitToUIUntilMount = true,
        instrumenter = instrumenter,
    )

    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

    val traceId = instrumenter.begunTraces.single().traceId
    assertThat(instrumenter.pointNames(traceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_RESOLVE_START,
            RenderTraceManager.POINT_RESOLVE_END,
            RenderTraceManager.POINT_LAYOUT_START,
            RenderTraceManager.POINT_LAYOUT_END,
            RenderTraceManager.POINT_TRACE_COMMITTED,
            RenderTraceManager.POINT_TRACE_PENDING_COMMIT_TO_UI,
        )

    renderState.uiRootHostInput!!.commitToUI()

    assertThat(instrumenter.pointNames(traceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_RESOLVE_START,
            RenderTraceManager.POINT_RESOLVE_END,
            RenderTraceManager.POINT_LAYOUT_START,
            RenderTraceManager.POINT_LAYOUT_END,
            RenderTraceManager.POINT_TRACE_COMMITTED,
            RenderTraceManager.POINT_TRACE_PENDING_COMMIT_TO_UI,
            RenderTraceManager.POINT_TRACE_COMMITTED_TO_UI,
        )
    assertThat(instrumenter.cancelled).isEmpty()
  }

  @Test
  fun `deferred mode cancels prior pending-commit-to-ui trace when a new one supersedes it`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        deferCommitToUIUntilMount = true,
        instrumenter = instrumenter,
    )

    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    // Size constraints are now set; this second setTree drives the full pipeline
    // synchronously, producing a new mount input that supersedes the first.
    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }

    assertThat(instrumenter.begunTraces).hasSize(2)
    val firstTraceId = instrumenter.begunTraces[0].traceId
    val secondTraceId = instrumenter.begunTraces[1].traceId
    assertThat(instrumenter.cancelled)
        .containsExactly(
            CancelledTrace(
                firstTraceId,
                RCRenderTrace.CANCEL_REASON_DEFERRED_COMMIT_TO_UI_SUPERSEDED,
            ),
        )
    assertThat(instrumenter.pointNames(secondTraceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_RESOLVE_START,
            RenderTraceManager.POINT_RESOLVE_END,
            RenderTraceManager.POINT_LAYOUT_START,
            RenderTraceManager.POINT_LAYOUT_END,
            RenderTraceManager.POINT_TRACE_COMMITTED,
            RenderTraceManager.POINT_TRACE_PENDING_COMMIT_TO_UI,
        )
  }

  @Test
  fun `destroy cancels in-flight traces`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        deferCommitToUIUntilMount = true,
        instrumenter = instrumenter,
    )

    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    renderState.destroy()

    val traceId = instrumenter.begunTraces.single().traceId
    assertThat(instrumenter.cancelled)
        .containsExactly(CancelledTrace(traceId, RCRenderTrace.CANCEL_REASON_HOST_DESTROYED))
  }

  @Test
  fun `pipeline works with no instrumenter`() {
    val numberOfUICommits = AtomicInteger()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) = Unit

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) {
            assertThat(renderTrace).isNull()
            numberOfUICommits.incrementAndGet()
          }
        },
        null,
        null,
    )

    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

    assertThat(numberOfUICommits.get()).isEqualTo(1)
  }

  @Test
  fun `older async resolve that commits late cancels with newer_resolve_committed`() {
    val instrumenter = RecordingInstrumenter()
    val resolveThread = HandlerThread("ResolveBG").apply { start() }
    try {
      val renderState = RenderState<Any?, Any?, TestStateUpdate>(
          RuntimeEnvironment.application,
          emptyDelegate,
          null,
          null,
          resolveExecutor = SerialTestExecutor(Handler(resolveThread.looper)),
          instrumenter = instrumenter,
      )

      val slowResolveStarted = CountDownLatch(1)
      val slowResolveCanProceed = CountDownLatch(1)

      // Older resolve: runs on background thread, blocks inside resolveFunc.
      renderState.setTreeAsync { _, _, _, _ ->
        slowResolveStarted.countDown()
        slowResolveCanProceed.await()
        ResolveResult(TestNode(), Any())
      }
      // Wait until the slow resolve has entered its Callable so trace is RUNNING
      // (otherwise the next begin() would supersede it as superseded_before_run).
      assertThat(slowResolveStarted.await(5, TimeUnit.SECONDS)).isTrue

      // Newer resolve commits synchronously, advances committedResolveVersion.
      renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }

      // Release the older resolve so it returns and hits maybeCommitResolveResult.
      slowResolveCanProceed.countDown()

      // Drain the background handler so the older resolve fully processes (including the
      // markCancelled call after maybeCommitResolveResult returns false).
      val drained = CountDownLatch(1)
      Handler(resolveThread.looper).post { drained.countDown() }
      assertThat(drained.await(5, TimeUnit.SECONDS)).isTrue

      val olderTraceId = instrumenter.begunTraces[0].traceId
      assertThat(instrumenter.cancelled)
          .contains(
              CancelledTrace(olderTraceId, RCRenderTrace.CANCEL_REASON_NEWER_RESOLVE_COMMITTED),
          )
    } finally {
      resolveThread.quit()
    }
  }

  @Test
  fun `shared resolve future is not cancelled when both consumers commit the same version`() {
    val instrumenter = RecordingInstrumenter()
    val queuedResolve = AtomicReference<Runnable>()
    val committedTrace = AtomicReference<RCRenderTrace>()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        object : RenderState.Delegate<Any?> {
          override fun commit(
              layoutVersion: Int,
              current: RenderTree?,
              next: RenderTree,
              currentState: Any?,
              nextState: Any?,
          ) = Unit

          override fun commitToUI(
              tree: RenderTree,
              state: Any?,
              frameId: Int,
              renderTrace: RCRenderTrace?,
          ) {
            committedTrace.set(renderTrace)
          }
        },
        null,
        null,
        resolveExecutor = Executor { queuedResolve.set(it) },
        instrumenter = instrumenter,
    )

    renderState.setTreeAsync { _, _, _, _ ->
      ResolveResult(TestNode(), Any())
    }

    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    queuedResolve.get().run()
    committedTrace.get().markComplete()

    val traceId = instrumenter.begunTraces.single().traceId
    assertThat(instrumenter.cancelled).isEmpty()
    assertThat(instrumenter.successfulTraceIds).containsExactly(traceId)
  }

  @Test
  fun `background layout defers commit to waiting main thread`() {
    val instrumenter = RecordingInstrumenter()
    val layoutStarted = CountDownLatch(1)
    val layoutCanProceed = CountDownLatch(1)
    val backgroundPipelineComplete = CountDownLatch(1)
    val backgroundFailure = AtomicReference<Throwable>()
    val layoutThread = AtomicReference<Thread>()
    val commitThread = AtomicReference<Thread>()
    val resolveThread = HandlerThread("ResolveBG").apply { start() }
    try {
      installLayoutFutureInstrumenter(
          beforeGet = { isMainThread ->
            if (isMainThread) {
              layoutCanProceed.countDown()
            }
          },
          afterGet = { isMainThread ->
            if (isMainThread) {
              await(backgroundPipelineComplete, "background commit attempt")
            }
          },
      )
      val renderState = RenderState<Any?, Any?, TestStateUpdate>(
          RuntimeEnvironment.application,
          object : RenderState.Delegate<Any?> {
            override fun commit(
                layoutVersion: Int,
                current: RenderTree?,
                next: RenderTree,
                currentState: Any?,
                nextState: Any?,
            ) {
              commitThread.set(Thread.currentThread())
            }

            override fun commitToUI(
                tree: RenderTree,
                state: Any?,
                frameId: Int,
                renderTrace: RCRenderTrace?,
            ) = Unit
          },
          null,
          null,
          deferLayoutCommitToMainThread = true,
          instrumenter = instrumenter,
          resolveExecutor =
              Executor { command ->
                Handler(resolveThread.looper).post {
                  try {
                    command.run()
                  } catch (throwable: Throwable) {
                    backgroundFailure.set(throwable)
                  } finally {
                    backgroundPipelineComplete.countDown()
                  }
                }
              },
      )
      renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
      renderState.setTreeAsync { _, _, _, _ ->
        ResolveResult(
            object : TestNode() {
              override fun calculateLayout(
                  context: LayoutContext<*>,
                  sizeConstraints: SizeConstraints,
              ): LayoutResult {
                layoutThread.set(Thread.currentThread())
                layoutStarted.countDown()
                layoutCanProceed.await()
                return super.calculateLayout(context, sizeConstraints)
              }
            },
            Any(),
        )
      }
      assertThat(layoutStarted.await(5, TimeUnit.SECONDS)).isTrue

      val mainThread = Thread.currentThread()
      renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

      backgroundFailure.get()?.let { throw it }
      assertThat(layoutThread.get()).isSameAs(resolveThread)
      assertThat(commitThread.get()).isSameAs(mainThread)
      assertThat(renderState.uiRootHostInput).isNotNull
      assertCommittedToUiWithoutCancellation(instrumenter)
    } finally {
      layoutCanProceed.countDown()
      FutureInstrumenter.provide(null)
      resolveThread.quit()
    }
  }

  @Test
  fun `background waiter defers commit when main thread runs layout`() {
    val instrumenter = RecordingInstrumenter()
    val backgroundMeasureCanStart = CountDownLatch(1)
    val backgroundJoinedLayout = CountDownLatch(1)
    val backgroundMeasureComplete = CountDownLatch(1)
    val backgroundFailure = AtomicReference<Throwable>()
    val layoutThread = AtomicReference<Thread>()
    val commitThread = AtomicReference<Thread>()
    val backgroundThread = HandlerThread("MeasureBG").apply { start() }
    try {
      installLayoutFutureInstrumenter(
          beforeGet = { isMainThread ->
            if (!isMainThread) {
              backgroundJoinedLayout.countDown()
            }
          },
          afterGet = { isMainThread ->
            if (isMainThread) {
              await(backgroundMeasureComplete, "background commit attempt")
            }
          },
      )
      val renderState = RenderState<Any?, Any?, TestStateUpdate>(
          RuntimeEnvironment.application,
          object : RenderState.Delegate<Any?> {
            override fun commit(
                layoutVersion: Int,
                current: RenderTree?,
                next: RenderTree,
                currentState: Any?,
                nextState: Any?,
            ) {
              commitThread.set(Thread.currentThread())
            }

            override fun commitToUI(
                tree: RenderTree,
                state: Any?,
                frameId: Int,
                renderTrace: RCRenderTrace?,
            ) = Unit
          },
          null,
          null,
          deferLayoutCommitToMainThread = true,
          instrumenter = instrumenter,
      )
      renderState.setTree { _, _, _, _ ->
        ResolveResult(
            object : TestNode() {
              override fun calculateLayout(
                  context: LayoutContext<*>,
                  sizeConstraints: SizeConstraints,
              ): LayoutResult {
                layoutThread.set(Thread.currentThread())
                backgroundMeasureCanStart.countDown()
                await(backgroundJoinedLayout, "background layout waiter")
                return super.calculateLayout(context, sizeConstraints)
              }
            },
            Any(),
        )
      }
      Handler(backgroundThread.looper).post {
        try {
          await(backgroundMeasureCanStart, "main layout start")
          renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
        } catch (throwable: Throwable) {
          backgroundFailure.set(throwable)
        } finally {
          backgroundMeasureComplete.countDown()
        }
      }

      val mainThread = Thread.currentThread()
      renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

      backgroundFailure.get()?.let { throw it }
      assertThat(layoutThread.get()).isSameAs(mainThread)
      assertThat(commitThread.get()).isSameAs(mainThread)
      assertThat(renderState.uiRootHostInput).isNotNull
      assertCommittedToUiWithoutCancellation(instrumenter)
    } finally {
      backgroundMeasureCanStart.countDown()
      backgroundJoinedLayout.countDown()
      FutureInstrumenter.provide(null)
      backgroundThread.quit()
    }
  }

  @Test
  fun `main thread promotes layout already committed on background`() {
    val instrumenter = RecordingInstrumenter()
    val backgroundCommitComplete = CountDownLatch(1)
    val backgroundPipelineComplete = CountDownLatch(1)
    val uiCommitCount = AtomicInteger()
    val resolveThread = HandlerThread("ResolveBG").apply { start() }
    try {
      val renderState = RenderState<Any?, Any?, TestStateUpdate>(
          RuntimeEnvironment.application,
          object : RenderState.Delegate<Any?> {
            override fun commit(
                layoutVersion: Int,
                current: RenderTree?,
                next: RenderTree,
                currentState: Any?,
                nextState: Any?,
            ) {
              backgroundCommitComplete.countDown()
            }

            override fun commitToUI(
                tree: RenderTree,
                state: Any?,
                frameId: Int,
                renderTrace: RCRenderTrace?,
            ) {
              uiCommitCount.incrementAndGet()
            }
          },
          null,
          null,
          resolveExecutor =
              Executor { command ->
                Handler(resolveThread.looper).post {
                  try {
                    command.run()
                  } finally {
                    backgroundPipelineComplete.countDown()
                  }
                }
              },
          instrumenter = instrumenter,
      )
      renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

      renderState.setTreeAsync { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
      assertThat(backgroundCommitComplete.await(5, TimeUnit.SECONDS)).isTrue
      await(backgroundPipelineComplete, "background promotion post")
      assertThat(renderState.uiRootHostInput).isNull()

      renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
      Shadows.shadowOf(Looper.getMainLooper()).idle()

      assertThat(renderState.uiRootHostInput).isNotNull
      assertThat(uiCommitCount.get()).isEqualTo(1)
      assertCommittedToUiWithoutCancellation(instrumenter)
    } finally {
      resolveThread.quit()
    }
  }

  @Test
  fun `main thread preserves deferred trace when background promotion is queued`() {
    val instrumenter = RecordingInstrumenter()
    val backgroundPipelineComplete = CountDownLatch(1)
    val resolveThread = HandlerThread("ResolveBG").apply { start() }
    try {
      val renderState = RenderState<Any?, Any?, TestStateUpdate>(
          RuntimeEnvironment.application,
          emptyDelegate,
          null,
          null,
          resolveExecutor =
              Executor { command ->
                Handler(resolveThread.looper).post {
                  try {
                    command.run()
                  } finally {
                    backgroundPipelineComplete.countDown()
                  }
                }
              },
          deferCommitToUIUntilMount = true,
          instrumenter = instrumenter,
      )
      renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

      renderState.setTreeAsync { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
      await(backgroundPipelineComplete, "background promotion post")

      renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      renderState.uiRootHostInput!!.commitToUI()

      val traceId = instrumenter.begunTraces.single().traceId
      assertThat(instrumenter.pointNames(traceId))
          .containsExactly(
              RenderTraceManager.POINT_TRACE_RUNNING,
              RenderTraceManager.POINT_RESOLVE_START,
              RenderTraceManager.POINT_RESOLVE_END,
              RenderTraceManager.POINT_LAYOUT_START,
              RenderTraceManager.POINT_LAYOUT_END,
              RenderTraceManager.POINT_TRACE_COMMITTED,
              RenderTraceManager.POINT_TRACE_PENDING_COMMIT_TO_UI,
              RenderTraceManager.POINT_TRACE_COMMITTED_TO_UI,
          )
      assertThat(instrumenter.cancelled).isEmpty()
    } finally {
      resolveThread.quit()
    }
  }

  @Test
  fun `older layout that loses the commit gate cancels with newer_layout_committed`() {
    val instrumenter = RecordingInstrumenter()
    val layoutStarted = CountDownLatch(1)
    val layoutCanProceed = CountDownLatch(1)
    val layoutInvocations = AtomicInteger(0)
    val resolveThread = HandlerThread("ResolveBG").apply { start() }
    try {
      // Factory produces a fresh node per resolve so shouldReuseResult can't short-circuit
      // the layout. The second layout invocation (the async one) blocks; the rest run through.
      val makeNode = {
        object : TestNode() {
          override fun calculateLayout(
              context: LayoutContext<*>,
              sizeConstraints: SizeConstraints,
          ): LayoutResult {
            if (layoutInvocations.getAndIncrement() == 1) {
              layoutStarted.countDown()
              layoutCanProceed.await()
            }
            return super.calculateLayout(context, sizeConstraints)
          }
        }
      }

      val renderState = RenderState<Any?, Any?, TestStateUpdate>(
          RuntimeEnvironment.application,
          emptyDelegate,
          null,
          null,
          resolveExecutor = SerialTestExecutor(Handler(resolveThread.looper)),
          instrumenter = instrumenter,
      )

      // Seed: commit a first layout so subsequent ones have a baseline committedLayoutVersion.
      renderState.setTree { _, _, _, _ -> ResolveResult(makeNode(), Any()) }
      renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

      // Async: trigger a new resolve+layout chain. The bg layout blocks before its commit gate.
      renderState.setTreeAsync { _, _, _, _ -> ResolveResult(makeNode(), Any()) }
      assertThat(layoutStarted.await(5, TimeUnit.SECONDS)).isTrue

      // Sync (test thread): a new resolve+layout commits while the bg layout is still blocked.
      renderState.setTree { _, _, _, _ -> ResolveResult(makeNode(), Any()) }

      // Release the bg layout — it returns and fails the commit gate (stale version).
      layoutCanProceed.countDown()

      val drained = CountDownLatch(1)
      Handler(resolveThread.looper).post { drained.countDown() }
      assertThat(drained.await(5, TimeUnit.SECONDS)).isTrue

      val olderLayoutTraceId = instrumenter.begunTraces[1].traceId
      assertThat(instrumenter.cancelled)
          .contains(
              CancelledTrace(
                  olderLayoutTraceId,
                  RCRenderTrace.CANCEL_REASON_NEWER_LAYOUT_COMMITTED,
              ),
          )
    } finally {
      resolveThread.quit()
    }
  }

  @Test
  fun `specs-changing measure during in-flight latest layout creates a new layout task`() {
    val outcome = runSpecsChangeDuringInflightLayout(relayoutEnabled = true)
    // Three layouts ran (seed, blocked bg, measure-driven) and two committed (seed and
    // measure-driven).
    assertThat(outcome.layoutInvocations).isEqualTo(3)
    assertThat(outcome.commits).isEqualTo(2)
    val blockedLayoutTraceId = outcome.instrumenter.begunTraces[1].traceId
    assertThat(outcome.instrumenter.cancelled)
        .contains(
            CancelledTrace(
                blockedLayoutTraceId,
                RCRenderTrace.CANCEL_REASON_NEWER_LAYOUT_COMMITTED,
            ),
        )
  }

  @Test
  fun `specs-changing measure during in-flight latest layout fast-paths when relayout is disabled`() {
    val outcome = runSpecsChangeDuringInflightLayout(relayoutEnabled = false)
    // Gate off: the measure takes the fast path — two layouts, one commit — and the latest
    // tree is never laid out.
    assertThat(outcome.layoutInvocations).isEqualTo(2)
    assertThat(outcome.commits).isEqualTo(1)
  }

  private data class SpecsChangeOutcome(
      val layoutInvocations: Int,
      val commits: Int,
      val instrumenter: RecordingInstrumenter,
  )

  // FbHandlerThreadFactory needs an initialized app scope, unavailable in this unit
  // test; a plain HandlerThread matches every other test in this file.
  @SuppressLint("BadMethodUse-android.os.HandlerThread._Constructor")
  private fun runSpecsChangeDuringInflightLayout(relayoutEnabled: Boolean): SpecsChangeOutcome {
    val instrumenter = RecordingInstrumenter()
    val layoutStarted = CountDownLatch(1)
    val layoutCanProceed = CountDownLatch(1)
    val layoutInvocations = AtomicInteger(0)
    val commits = AtomicInteger(0)
    val resolveThread = HandlerThread("ResolveBG").apply { start() }
    try {
      // Factory produces a fresh node per resolve so shouldReuseResult can't short-circuit
      // the layout. The second layout invocation (the async one) blocks; the rest run through.
      val makeNode = {
        object : TestNode() {
          override fun calculateLayout(
              context: LayoutContext<*>,
              sizeConstraints: SizeConstraints,
          ): LayoutResult {
            if (layoutInvocations.getAndIncrement() == 1) {
              layoutStarted.countDown()
              layoutCanProceed.await()
            }
            return super.calculateLayout(context, sizeConstraints)
          }
        }
      }

      val countingDelegate =
          object : RenderState.Delegate<Any?> {
            override fun commit(
                layoutVersion: Int,
                current: RenderTree?,
                next: RenderTree,
                currentState: Any?,
                nextState: Any?,
            ) {
              commits.incrementAndGet()
            }

            override fun commitToUI(
                tree: RenderTree,
                state: Any?,
                frameId: Int,
                renderTrace: RCRenderTrace?,
            ) = Unit
          }
      val renderState = RenderState<Any?, Any?, TestStateUpdate>(
          RuntimeEnvironment.application,
          countingDelegate,
          null,
          null,
          resolveExecutor = SerialTestExecutor(Handler(resolveThread.looper)),
          instrumenter = instrumenter,
          relayoutLatestTreeOnSpecsChange = relayoutEnabled,
      )

      // Seed: commit a first layout so the measure fast paths have a compatible tree.
      renderState.setTree { _, _, _, _ -> ResolveResult(makeNode(), Any()) }
      renderState.measure(SizeConstraints(maxWidth = 200, maxHeight = 200), IntArray(2))

      // Async: trigger a new resolve+layout chain. The bg layout blocks before its commit gate.
      renderState.setTreeAsync { _, _, _, _ -> ResolveResult(makeNode(), Any()) }
      assertThat(layoutStarted.await(5, TimeUnit.SECONDS)).isTrue

      // Measure with different-but-compatible specs. This must not take the fast path:
      // the specs update would stale the blocked layout at the exact-spec gate and strand
      // the latest tree with no layout. Instead a new layout task must commit for it.
      renderState.measure(SizeConstraints(maxWidth = 150, maxHeight = 150), IntArray(2))

      // Release the bg layout — it returns and fails the commit gate (stale specs+version).
      layoutCanProceed.countDown()

      val drained = CountDownLatch(1)
      Handler(resolveThread.looper).post { drained.countDown() }
      assertThat(drained.await(5, TimeUnit.SECONDS)).isTrue

      return SpecsChangeOutcome(layoutInvocations.get(), commits.get(), instrumenter)
    } finally {
      resolveThread.quit()
    }
  }

  @Test
  fun `pure re-layout via measure opens a measure trace reusing the resolve renderId`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        instrumenter = instrumenter,
    )

    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    // Different constraints → pure re-layout, no resolve. Should open a MEASURE trace.
    renderState.measure(SizeConstraints.exact(200, 200), IntArray(2))

    assertThat(instrumenter.begunTraces).hasSize(2)
    val resolveTrace = instrumenter.begunTraces[0]
    val measureTrace = instrumenter.begunTraces[1]
    assertThat(resolveTrace.operation).isEqualTo(RenderTraceOperation.RESOLVE)
    assertThat(measureTrace.operation).isEqualTo(RenderTraceOperation.MEASURE)
    assertThat(measureTrace.renderId).isEqualTo(resolveTrace.renderId)
    assertThat(instrumenter.pointNames(measureTrace.traceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_LAYOUT_START,
            RenderTraceManager.POINT_LAYOUT_END,
            RenderTraceManager.POINT_TRACE_COMMITTED,
            RenderTraceManager.POINT_TRACE_COMMITTED_TO_UI,
        )
  }

  @Test
  fun `remeasure with compatible cached layout does not open a new trace`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        instrumenter = instrumenter,
    )

    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))
    // Same constraints → cached layout is reused, no layout work, no new trace.
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

    assertThat(instrumenter.begunTraces).hasSize(1)
    assertThat(instrumenter.begunTraces.single().operation).isEqualTo(RenderTraceOperation.RESOLVE)
  }

  @Test
  fun `setTree without constraints then measure uses the resolve trace, not a measure trace`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        instrumenter = instrumenter,
    )

    // setTree runs the resolve synchronously and commits it, but layout returns early because
    // size constraints are not yet set. The resolve trace sits on pendingLayoutTrace.
    renderState.setTree { _, _, _, _ -> ResolveResult(TestNode(), Any()) }
    // measure now provides constraints and triggers the deferred layout. The existing resolve
    // trace should drive the layout commit, not a freshly created measure trace.
    renderState.measure(SizeConstraints.exact(100, 100), IntArray(2))

    assertThat(instrumenter.begunTraces).hasSize(1)
    val trace = instrumenter.begunTraces.single()
    assertThat(trace.operation).isEqualTo(RenderTraceOperation.RESOLVE)
    assertThat(instrumenter.pointNames(trace.traceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_RESOLVE_START,
            RenderTraceManager.POINT_RESOLVE_END,
            RenderTraceManager.POINT_LAYOUT_START,
            RenderTraceManager.POINT_LAYOUT_END,
            RenderTraceManager.POINT_TRACE_COMMITTED,
            RenderTraceManager.POINT_TRACE_COMMITTED_TO_UI,
        )
  }

  @Test
  fun `resolve throws and trace cancels with resolve_failed before _ended fires`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        instrumenter = instrumenter,
    )

    assertThatThrownBy {
          renderState.setTree { _, _, _, _ -> throw RuntimeException("resolve boom") }
        }
        .hasMessage("resolve boom")

    val traceId = instrumenter.begunTraces.single().traceId
    assertThat(instrumenter.cancelled)
        .containsExactly(CancelledTrace(traceId, RCRenderTrace.CANCEL_REASON_RESOLVE_FAILED))
    // _end must land before the cancel so the trace ends cleanly on failure.
    assertThat(instrumenter.pointNames(traceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_RESOLVE_START,
            RenderTraceManager.POINT_RESOLVE_END,
        )
  }

  @Test
  fun `layout throws and trace cancels with layout_failed before _ended fires`() {
    val instrumenter = RecordingInstrumenter()
    val renderState = RenderState<Any?, Any?, TestStateUpdate>(
        RuntimeEnvironment.application,
        emptyDelegate,
        null,
        null,
        instrumenter = instrumenter,
    )

    val throwingNode =
        object : TestNode() {
          override fun calculateLayout(
              context: LayoutContext<*>,
              widthSpec: Int,
              heightSpec: Int,
          ): LayoutResult = throw RuntimeException("layout boom")
        }

    renderState.setTree { _, _, _, _ -> ResolveResult(throwingNode, Any()) }
    assertThatThrownBy { renderState.measure(SizeConstraints.exact(100, 100), IntArray(2)) }
        .hasMessage("layout boom")

    val traceId = instrumenter.begunTraces.single().traceId
    assertThat(instrumenter.cancelled)
        .containsExactly(CancelledTrace(traceId, RCRenderTrace.CANCEL_REASON_LAYOUT_FAILED))
    assertThat(instrumenter.pointNames(traceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_RESOLVE_START,
            RenderTraceManager.POINT_RESOLVE_END,
            RenderTraceManager.POINT_LAYOUT_START,
            RenderTraceManager.POINT_LAYOUT_END,
        )
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

  private fun installLayoutFutureInstrumenter(
      beforeGet: (isMainThread: Boolean) -> Unit,
      afterGet: (isMainThread: Boolean) -> Unit,
  ) {
    FutureInstrumenter.provide(
        object : FutureInstrumenter.Instrumenter {
          override fun <V> instrument(
              future: RunnableFuture<V>,
              tag: String,
          ): RunnableFuture<V> {
            if (tag != "LayoutFuture") {
              return future
            }
            return object : RunnableFuture<V> by future {
              // These tests intentionally block main inside Future.get() to reproduce the race
              // between a UI measure and a background consumer of the same layout future.
              @SuppressLint("BadMethodUse-java.util.concurrent.Future.get")
              override fun get(): V {
                val isMainThread = ThreadUtils.isMainThread
                beforeGet(isMainThread)
                val result = future.get()
                afterGet(isMainThread)
                return result
              }
            }
          }
        },
    )
  }

  private fun await(latch: CountDownLatch, milestone: String) {
    assertThat(latch.await(5, TimeUnit.SECONDS))
        .describedAs("Timed out waiting for $milestone")
        .isTrue
  }

  private fun assertCommittedToUiWithoutCancellation(instrumenter: RecordingInstrumenter) {
    val traceId = instrumenter.begunTraces.single().traceId
    assertThat(instrumenter.pointNames(traceId))
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_RESOLVE_START,
            RenderTraceManager.POINT_RESOLVE_END,
            RenderTraceManager.POINT_LAYOUT_START,
            RenderTraceManager.POINT_LAYOUT_END,
            RenderTraceManager.POINT_TRACE_COMMITTED,
            RenderTraceManager.POINT_TRACE_COMMITTED_TO_UI,
        )
    assertThat(instrumenter.cancelled).isEmpty()
  }

  private data class CancelledTrace(val traceId: Int, val reason: String)

  private data class Point(val traceId: Int, val name: String)

  private class RecordingInstrumenter : TraceInstrumenter {
    val begunTraces = mutableListOf<BegunTrace>()
    val cancelled = mutableListOf<CancelledTrace>()
    val successfulTraceIds = mutableListOf<Int>()
    val points = mutableListOf<Point>()

    fun pointNames(traceId: Int): List<String> =
        points.filter { it.traceId == traceId }.map { it.name }

    override fun beginTrace(traceId: Int, renderId: Int, operation: RenderTraceOperation) {
      begunTraces.add(BegunTrace(traceId, renderId, operation))
    }

    override fun cancelTrace(traceId: Int, reason: String) {
      cancelled.add(CancelledTrace(traceId, reason))
    }

    override fun endTraceSuccess(traceId: Int) {
      successfulTraceIds.add(traceId)
    }

    override fun addPoint(traceId: Int, name: String, annotations: Map<String, Any?>?) {
      points.add(Point(traceId, name))
    }

    override fun addAnnotation(traceId: Int, key: String, value: Any) = Unit
  }

  private data class BegunTrace(
      val traceId: Int,
      val renderId: Int,
      val operation: RenderTraceOperation,
  )
}
