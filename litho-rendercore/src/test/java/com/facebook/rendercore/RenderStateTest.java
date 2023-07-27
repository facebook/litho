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

package com.facebook.rendercore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.PAUSED;

import android.os.Looper;
import android.util.Pair;
import android.view.View;
import com.facebook.rendercore.testing.TestNode;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
public class RenderStateTest {

  final RenderState.Delegate emptyDelegate =
      new RenderState.Delegate() {
        @Override
        public void commit(
            int layoutVersion,
            RenderTree current,
            RenderTree next,
            Object currentState,
            Object nextState) {}

        @Override
        public void commitToUI(RenderTree tree, Object o) {}
      };

  @Test
  public void testSettingTreeWithExecutoreResolvesOnTheExecutor() {
    final RenderState renderState =
        new RenderState(
            RuntimeEnvironment.application,
            new RenderState.Delegate() {
              @Override
              public void commit(
                  int layoutVersion,
                  RenderTree current,
                  RenderTree next,
                  Object currentState,
                  Object nextState) {}

              @Override
              public void commitToUI(RenderTree tree, Object o) {}
            },
            null,
            null);
    final AtomicBoolean wasExecuteCalled = new AtomicBoolean();
    final AtomicBoolean wasCalledInExecute = new AtomicBoolean();
    final AtomicBoolean executing = new AtomicBoolean();

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            wasCalledInExecute.set(executing.get());
            return new Pair(new TestNode(), null);
          }
        },
        new Executor() {
          @Override
          public void execute(Runnable runnable) {
            wasExecuteCalled.set(true);
            executing.set(true);
            runnable.run();
            executing.set(false);
          }
        });

    assertThat(wasExecuteCalled.get()).isTrue();
    assertThat(wasCalledInExecute.get()).isTrue();
  }

  @Test
  public void testCommitWhileDetached() {
    final Object state = new Object();
    final AtomicBoolean wasCalled = new AtomicBoolean();

    final RenderState renderState =
        new RenderState(
            RuntimeEnvironment.application,
            new RenderState.Delegate() {
              @Override
              public void commit(
                  int layoutVersion,
                  RenderTree current,
                  RenderTree next,
                  Object currentState,
                  Object nextState) {
                assertThat(current).isNull();
                assertThat(next).isNotNull();
                assertThat(currentState).isNull();
                assertThat(nextState).isNotNull();
                wasCalled.set(true);
              }

              @Override
              public void commitToUI(RenderTree tree, Object o) {}
            },
            null,
            null);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(new TestNode(), state);
          }
        });
    renderState.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        new int[2]);
    assertThat(wasCalled.get()).isTrue();
  }

  @Test
  public void testReturningSameNodeSkipsLayoutIfMeasuresAreCompatible() {
    final Object state = new Object();
    final Object secondState = new Object();
    final AtomicInteger layoutCount = new AtomicInteger();
    final AtomicInteger renderTreeCount = new AtomicInteger();

    final int measureExactly100 = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
    final int measureExactly200 = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);

    final RenderState renderState =
        new RenderState(
            RuntimeEnvironment.application,
            new RenderState.Delegate() {
              @Override
              public void commit(
                  int layoutVersion,
                  RenderTree current,
                  RenderTree next,
                  Object currentState,
                  Object nextState) {
                if (current != next) {
                  renderTreeCount.incrementAndGet();
                }
              }

              @Override
              public void commitToUI(RenderTree tree, Object o) {}
            },
            null,
            null);
    final Node nodeToReturn =
        new TestNode() {
          @Override
          public LayoutResult calculateLayout(
              LayoutContext context, int widthSpec, int heightSpec) {
            layoutCount.incrementAndGet();
            return super.calculateLayout(context, widthSpec, heightSpec);
          }
        };

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(nodeToReturn, state);
          }
        });

    renderState.measure(measureExactly100, measureExactly100, new int[2]);

    assertThat(layoutCount.intValue()).isEqualTo(1);
    assertThat(renderTreeCount.intValue()).isEqualTo(1);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(nodeToReturn, secondState);
          }
        });

    assertThat(layoutCount.intValue()).isEqualTo(1);
    assertThat(renderTreeCount.intValue()).isEqualTo(1);

    renderState.measure(measureExactly100, measureExactly100, new int[2]);
    assertThat(layoutCount.intValue()).isEqualTo(1);
    assertThat(renderTreeCount.intValue()).isEqualTo(1);

    renderState.measure(measureExactly200, measureExactly200, new int[2]);
    assertThat(layoutCount.intValue()).isEqualTo(2);
    assertThat(renderTreeCount.intValue()).isEqualTo(2);
  }

  @Test
  public void testIncreasingLayoutVersion() {
    final Object state = new Object();
    final Object secondState = new Object();
    final AtomicInteger firstLayoutVersion = new AtomicInteger();
    final AtomicInteger secondLayoutVersion = new AtomicInteger();

    final RenderState renderState =
        new RenderState(RuntimeEnvironment.application, emptyDelegate, null, null);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(
                new TestNode() {
                  @Override
                  public LayoutResult calculateLayout(
                      LayoutContext context, int widthSpec, int heightSpec) {
                    firstLayoutVersion.set(context.getLayoutVersion());
                    return super.calculateLayout(context, widthSpec, heightSpec);
                  }
                },
                state);
          }
        });

    renderState.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        new int[2]);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(
                new TestNode() {
                  @Override
                  public LayoutResult calculateLayout(
                      LayoutContext context, int widthSpec, int heightSpec) {
                    secondLayoutVersion.set(context.getLayoutVersion());
                    return super.calculateLayout(context, widthSpec, heightSpec);
                  }
                },
                secondState);
          }
        });

    assertThat(secondLayoutVersion.intValue()).isEqualTo(firstLayoutVersion.intValue() + 1);

    renderState.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        new int[2]);

    assertThat(secondLayoutVersion.intValue()).isEqualTo(firstLayoutVersion.intValue() + 2);
  }

  @Test
  public void testRemeasure_doesnt_resolve_again() {
    final AtomicInteger resolveCount = new AtomicInteger(0);
    final AtomicInteger layoutCount = new AtomicInteger(0);

    final RenderState renderState =
        new RenderState(RuntimeEnvironment.application, emptyDelegate, null, null);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            resolveCount.incrementAndGet();
            return new Pair(
                new TestNode() {
                  @Override
                  public LayoutResult calculateLayout(
                      LayoutContext context, int widthSpec, int heightSpec) {
                    layoutCount.incrementAndGet();
                    return super.calculateLayout(context, widthSpec, heightSpec);
                  }
                },
                new Object());
          }
        });

    renderState.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        new int[2]);
    assertThat(resolveCount.intValue()).isEqualTo(1);
    assertThat(layoutCount.intValue()).isEqualTo(1);

    renderState.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        new int[2]);
    assertThat(resolveCount.intValue()).isEqualTo(1);
    assertThat(layoutCount.intValue()).isEqualTo(2);

    // Remeasureing with the same constraints doesn't "layout" again
    renderState.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        new int[2]);
    assertThat(resolveCount.intValue()).isEqualTo(1);
    assertThat(layoutCount.intValue()).isEqualTo(2);
  }

  @Test
  public void testNewStateWithSameTreeCommits() {
    final AtomicInteger numberOfCommits = new AtomicInteger(0);
    final AtomicInteger numberOfUICommits = new AtomicInteger(0);

    final RenderState renderState =
        new RenderState(
            RuntimeEnvironment.application,
            new RenderState.Delegate() {
              @Override
              public void commit(
                  int layoutVersion,
                  @Nullable RenderTree current,
                  RenderTree next,
                  @Nullable Object currentState,
                  @Nullable Object nextState) {
                numberOfCommits.incrementAndGet();
              }

              @Override
              public void commitToUI(RenderTree tree, @Nullable Object o) {
                numberOfUICommits.incrementAndGet();
              }
            },
            null,
            null);

    renderState.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        new int[2]);
    final Node testNode = new TestNode();

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(testNode, new Object());
          }
        });

    assertThat(numberOfCommits.get()).isEqualTo(1);
    assertThat(numberOfUICommits.get()).isEqualTo(1);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(testNode, new Object());
          }
        });

    assertThat(numberOfCommits.get()).isEqualTo(2);
    assertThat(numberOfUICommits.get()).isEqualTo(2);
  }

  @Test
  public void testSameStateWithSameTreeCommits() {
    final AtomicInteger numberOfCommits = new AtomicInteger(0);
    final AtomicInteger numberOfUICommits = new AtomicInteger(0);

    final RenderState renderState =
        new RenderState(
            RuntimeEnvironment.application,
            new RenderState.Delegate() {
              @Override
              public void commit(
                  int layoutVersion,
                  @Nullable RenderTree current,
                  RenderTree next,
                  @Nullable Object currentState,
                  @Nullable Object nextState) {
                numberOfCommits.incrementAndGet();
              }

              @Override
              public void commitToUI(RenderTree tree, @Nullable Object o) {
                numberOfUICommits.incrementAndGet();
              }
            },
            null,
            null);

    renderState.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        new int[2]);
    final Node testNode = new TestNode();
    final Object state = new Object();
    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(testNode, state);
          }
        });

    assertThat(numberOfCommits.get()).isEqualTo(1);
    assertThat(numberOfUICommits.get()).isEqualTo(1);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            return new Pair(testNode, state);
          }
        });

    assertThat(numberOfCommits.get()).isEqualTo(2);
    assertThat(numberOfUICommits.get()).isEqualTo(2);
  }

  @Test
  public void enqueueStateUpdate_whenTriggered_resolvesAgainWithStateUpdates() {
    final AtomicInteger resolveCount = new AtomicInteger(0);
    final AtomicReference<List<?>> appliedStateUpdates = new AtomicReference<>();

    final RenderState<Object, Void> renderState =
        new RenderState<>(RuntimeEnvironment.getApplication(), emptyDelegate, null, null);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            resolveCount.incrementAndGet();
            appliedStateUpdates.set(stateUpdatesToApply);
            return new Pair(new TestNode(), new Object());
          }
        });
    assertThat(resolveCount.intValue()).isEqualTo(1);

    renderState.enqueueStateUpdate(new TestStateUpdate());
    assertThat(resolveCount.intValue()).isEqualTo(2);
    assertThat(appliedStateUpdates.get()).hasSize(1);
  }

  @Test
  public void enqueueStateUpdate_whenResolveWithStateUpdates_appliesAndClearsStateUpdates() {
    final AtomicReference<List<?>> appliedStateUpdates = new AtomicReference<>();

    final RenderState<Object, Void> renderState =
        new RenderState<>(RuntimeEnvironment.getApplication(), emptyDelegate, null, null);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            appliedStateUpdates.set(stateUpdatesToApply);
            return new Pair(new TestNode(), new Object());
          }
        });

    renderState.enqueueStateUpdate(new TestStateUpdate());
    assertThat(appliedStateUpdates.get()).hasSize(1);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            // Below we check that these 'stateUpdatesToApply' are empty, meaning that
            // 'pendingStateUpdates' were empty.
            appliedStateUpdates.set(stateUpdatesToApply);
            return new Pair(new TestNode(), new Object());
          }
        });

    assertThat(appliedStateUpdates.get()).isEmpty();
  }

  @Test
  public void enqueueStateUpdate_whenTriggeredBeforeSetTree_appliesInResolveOnSetTree() {
    final AtomicInteger resolveCount = new AtomicInteger(0);
    final AtomicReference<List<?>> appliedStateUpdates = new AtomicReference<>();

    final RenderState<Object, Void> renderState =
        new RenderState<>(RuntimeEnvironment.getApplication(), emptyDelegate, null, null);

    renderState.enqueueStateUpdate(new TestStateUpdate());
    assertThat(resolveCount.intValue()).isEqualTo(0);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            resolveCount.incrementAndGet();
            appliedStateUpdates.set(stateUpdatesToApply);
            return new Pair(new TestNode(), new Object());
          }
        });

    assertThat(resolveCount.intValue()).isEqualTo(1);
    assertThat(appliedStateUpdates.get()).hasSize(1);
  }

  @LooperMode(PAUSED)
  @Test
  public void enqueueStateUpdate_whenTriggeredManyTimesInARow_areBatchedAndResolveOnce() {
    final AtomicInteger resolveCount = new AtomicInteger(0);
    final AtomicReference<List<?>> appliedStateUpdates = new AtomicReference<>();

    final RenderState<Object, Void> renderState =
        new RenderState<>(RuntimeEnvironment.getApplication(), emptyDelegate, null, null);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {

            resolveCount.incrementAndGet();
            appliedStateUpdates.set(stateUpdatesToApply);
            return new Pair(new TestNode(), new Object());
          }
        });
    assertThat(resolveCount.intValue()).isEqualTo(1);

    // Two state updates are triggered.
    renderState.enqueueStateUpdate(new TestStateUpdate());
    renderState.enqueueStateUpdate(new TestStateUpdate());

    assertThat(resolveCount.intValue()).isEqualTo(1);
    shadowOf(Looper.getMainLooper()).idle();

    // But only one 'flushStateUpdates' is scheduled and executed.
    assertThat(resolveCount.intValue()).isEqualTo(2);
    assertThat(appliedStateUpdates.get()).hasSize(2);
  }

  @LooperMode(PAUSED)
  @Test
  public void enqueueStateUpdate_whenSetTreeExecutesBeforeFlushStateUpdate_flushDoesNothing() {
    final AtomicInteger resolveCount = new AtomicInteger(0);
    final AtomicReference<List<?>> appliedStateUpdates = new AtomicReference<>();

    final RenderState<Object, Void> renderState =
        new RenderState<>(RuntimeEnvironment.getApplication(), emptyDelegate, null, null);

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            resolveCount.incrementAndGet();
            appliedStateUpdates.set(stateUpdatesToApply);
            return new Pair(new TestNode(), new Object());
          }
        });
    assertThat(resolveCount.intValue()).isEqualTo(1);

    renderState.enqueueStateUpdate(new TestStateUpdate());

    // 'flushStateUpdates' is scheduled, but not executed yet.
    assertThat(resolveCount.intValue()).isEqualTo(1);
    assertThat(appliedStateUpdates.get()).isEmpty();

    renderState.setTree(
        new RenderState.ResolveFunc() {
          @Override
          public Pair resolve(
              ResolveContext resolveContext,
              @Nullable Node committedTree,
              @Nullable Object committedState,
              List stateUpdatesToApply) {
            resolveCount.incrementAndGet();
            appliedStateUpdates.set(stateUpdatesToApply);
            return new Pair(new TestNode(), new Object());
          }
        });

    // 'setTree' triggered 'resolve' which applied 'pendingStateUpdates'.
    assertThat(resolveCount.intValue()).isEqualTo(2);
    assertThat(appliedStateUpdates.get()).hasSize(1);

    // Release the Looper to execute scheduled 'flushStateUpdates'.
    shadowOf(Looper.getMainLooper()).idle();
    // 'resolve' was skipped, because 'pendingStateUpdates' are already empty.
    assertThat(resolveCount.intValue()).isEqualTo(2);
  }

  static class TestStateUpdate implements StateUpdateReceiver.StateUpdate {

    @Override
    public Object update(Object o) {
      return null;
    }
  }
}
