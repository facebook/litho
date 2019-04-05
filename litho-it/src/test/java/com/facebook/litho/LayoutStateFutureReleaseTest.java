/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Looper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateFutureReleaseTest {
  private int mWidthSpec;
  private int mHeightSpec;

  private ComponentContext mContext;
  private final boolean config = ComponentsConfiguration.useCancelableLayoutFutures;
  private ShadowLooper mLayoutThreadShadowLooper;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    ComponentsConfiguration.useCancelableLayoutFutures = true;

    mWidthSpec = makeSizeSpec(40, EXACTLY);
    mHeightSpec = makeSizeSpec(40, EXACTLY);

    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.useCancelableLayoutFutures = config;
  }

  @Test
  public void testStopResolvingRowChildrenIfLsfReleased() {
    final ComponentTree.LayoutStateFuture layoutStateFuture =
        mock(ComponentTree.LayoutStateFuture.class);

    when(layoutStateFuture.isReleased()).thenReturn(false);
    final ComponentContext c = new ComponentContext(mContext, null, null, null, layoutStateFuture);

    final CountDownLatch wait = new CountDownLatch(1);
    final TestChildComponent child1 =
        new TestChildComponent(
            wait,
            null,
            new WaitActions() {
              @Override
              public void unblock(ComponentTree.LayoutStateFuture lsf) {
                when(layoutStateFuture.isReleased()).thenReturn(true);
              }
            });

    final TestChildComponent child2 = new TestChildComponent();

    final Row row = Row.create(mContext).child(child1).child(child2).build();

    final ComponentLayout result = row.resolve(c);
    assertTrue(child1.hasRunLayout);
    assertFalse(child2.hasRunLayout);
    assertEquals(result, ComponentContext.NULL_LAYOUT);
  }

  @Test
  public void testStopResolvingColumnChildrenIfLsfReleased() {
    final ComponentTree.LayoutStateFuture layoutStateFuture =
        mock(ComponentTree.LayoutStateFuture.class);

    when(layoutStateFuture.isReleased()).thenReturn(false);
    final ComponentContext c = new ComponentContext(mContext, null, null, null, layoutStateFuture);

    final CountDownLatch wait = new CountDownLatch(1);
    final TestChildComponent child1 =
        new TestChildComponent(
            wait,
            null,
            new WaitActions() {
              @Override
              public void unblock(ComponentTree.LayoutStateFuture lsf) {
                when(layoutStateFuture.isReleased()).thenReturn(true);
              }
            });

    final TestChildComponent child2 = new TestChildComponent();

    final Column column = Column.create(mContext).child(child1).child(child2).build();

    final ComponentLayout result = column.resolve(c);
    assertTrue(child1.hasRunLayout);
    assertFalse(child2.hasRunLayout);
    assertEquals(result, ComponentContext.NULL_LAYOUT);
  }

  // When scheduling two async state updates, the first one will be cancelled if it doesn't complete
  // before the second one is triggered, because the layout calculation is made redundant by the
  // second one.
  @Test
  public void testReleaseRedundantBgLsf() {
    final CountDownLatch waitBeforeAsserts = new CountDownLatch(3);
    final ComponentTree componentTree;

    final TestChildComponent child1 = new TestChildComponent();
    final TestChildComponent child2 = new TestChildComponent();

    final Column column = Column.create(mContext).child(child1).child(child2).build();

    ThreadPoolLayoutHandler handler =
        new ThreadPoolLayoutHandler(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    componentTree =
        ComponentTree.create(mContext, column)
            .layoutThreadHandler(handler)
            .useSharedLayoutStateFuture(true)
            .build();

    componentTree.setLithoView(new LithoView(mContext));
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();
    assertThat(componentTree.getLayoutStateFutures()).isEmpty();

    final ComponentTree.LayoutStateFuture[] layoutStateFutures =
        new ComponentTree.LayoutStateFuture[2];
    final boolean[] isReleasedAfterStateUpdate = {false};

    // Testing scenario: the first state update layout will be cancelled when another state update
    // is
    // triggered during the first layout.
    child1.unlockFinishedLayout = waitBeforeAsserts;
    child1.waitActions =
        new WaitActions() {
          @Override
          public void unblock(ComponentTree.LayoutStateFuture lsf) {
            if (layoutStateFutures[0] == null) {
              layoutStateFutures[0] = lsf;
              componentTree.updateStateAsync(column.getGlobalKey(), new TestStateUpdate(), null);
              isReleasedAfterStateUpdate[0] = lsf.isReleased();
            } else {
              layoutStateFutures[1] = lsf;
            }
          }
        };
    child2.unlockFinishedLayout = waitBeforeAsserts;

    componentTree.updateStateAsync(column.getGlobalKey(), new TestStateUpdate(), null);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    try {
      waitBeforeAsserts.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // The first lsf is released during layout calculation.
    assertTrue(isReleasedAfterStateUpdate[0]);

    // The first layout will only resolve the first child before it gets cancelled.
    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[0]));
    assertFalse(child2.mLayoutStateFutureList.contains(layoutStateFutures[0]));

    // The second layout will complete.
    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[1]));
    assertTrue(child2.mLayoutStateFutureList.contains(layoutStateFutures[1]));
  }

  @Test
  public void testNotReleaseBlockingRedundantBgLsf() {
    final CountDownLatch waitBeforeAsserts = new CountDownLatch(4);
    final ComponentTree componentTree;

    final TestChildComponent child1 = new TestChildComponent();
    final TestChildComponent child2 = new TestChildComponent();

    final Column column = Column.create(mContext).child(child1).child(child2).build();

    ThreadPoolLayoutHandler handler =
        new ThreadPoolLayoutHandler(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    componentTree =
        ComponentTree.create(mContext, column)
            .layoutThreadHandler(handler)
            .useSharedLayoutStateFuture(true)
            .build();

    componentTree.setLithoView(new LithoView(mContext));
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();
    assertThat(componentTree.getLayoutStateFutures()).isEmpty();

    final ComponentTree.LayoutStateFuture[] layoutStateFutures =
        new ComponentTree.LayoutStateFuture[2];
    final boolean[] isReleasedAfterStateUpdate = {false};

    child1.unlockFinishedLayout = waitBeforeAsserts;
    child1.waitActions =
        new WaitActions() {
          @Override
          public void unblock(ComponentTree.LayoutStateFuture lsf) {
            if (layoutStateFutures[0] == null) {
              lsf.registerForResponse(true);
              layoutStateFutures[0] = lsf;
              componentTree.updateStateAsync(column.getGlobalKey(), new TestStateUpdate(), null);
              isReleasedAfterStateUpdate[0] = lsf.isReleased();
            } else {
              layoutStateFutures[1] = lsf;
            }
          }
        };
    child2.unlockFinishedLayout = waitBeforeAsserts;

    componentTree.updateStateAsync(column.getGlobalKey(), new TestStateUpdate(), null);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    try {
      waitBeforeAsserts.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertFalse(isReleasedAfterStateUpdate[0]);

    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[0]));
    assertTrue(child2.mLayoutStateFutureList.contains(layoutStateFutures[0]));

    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[1]));
    assertTrue(child2.mLayoutStateFutureList.contains(layoutStateFutures[1]));
  }

  // When scheduling an async state update while a sync state update is still completing, the first
  // layout will NOT be cancelled because the result is needed immediately.
  @Test
  public void testDontReleaseRedundantMainLsfFromBg() {
    final CountDownLatch waitBeforeAsserts = new CountDownLatch(4);
    final ComponentTree componentTree;

    final TestChildComponent child1 = new TestChildComponent();
    final TestChildComponent child2 = new TestChildComponent();

    final Column column = Column.create(mContext).child(child1).child(child2).build();

    ThreadPoolLayoutHandler handler =
        new ThreadPoolLayoutHandler(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    componentTree =
        ComponentTree.create(mContext, column)
            .layoutThreadHandler(handler)
            .useSharedLayoutStateFuture(true)
            .build();

    componentTree.setLithoView(new LithoView(mContext));
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();
    assertThat(componentTree.getLayoutStateFutures()).isEmpty();

    final ComponentTree.LayoutStateFuture[] layoutStateFutures =
        new ComponentTree.LayoutStateFuture[2];
    final boolean[] isReleasedAfterStateUpdate = {false};

    // Testing scenario: during the sync state update, an async state update is triggered (from the
    // same thread).
    // The sync state update will not be cancelled.
    child1.unlockFinishedLayout = waitBeforeAsserts;
    child1.waitActions =
        new WaitActions() {
          @Override
          public void unblock(ComponentTree.LayoutStateFuture lsf) {
            if (layoutStateFutures[0] == null) {
              layoutStateFutures[0] = lsf;
              componentTree.updateStateAsync(column.getGlobalKey(), new TestStateUpdate(), null);
              isReleasedAfterStateUpdate[0] = lsf.isReleased();
            } else {
              layoutStateFutures[1] = lsf;
            }
          }
        };
    child2.unlockFinishedLayout = waitBeforeAsserts;

    componentTree.updateStateSync(column.getGlobalKey(), new TestStateUpdate(), null);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    try {
      waitBeforeAsserts.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // The first lsf is released during layout calculation.
    assertFalse(isReleasedAfterStateUpdate[0]);

    // Both state update calls will finish resolving both children.
    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[0]));
    assertTrue(child2.mLayoutStateFutureList.contains(layoutStateFutures[0]));

    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[1]));
    assertTrue(child2.mLayoutStateFutureList.contains(layoutStateFutures[1]));
  }

  // When scheduling a sync state update while a sync state update is still completing, the first
  // layout will NOT be cancelled because the result is needed immediately.
  @Test
  public void testDontReleaseRedundantMainLsfFromMain() {
    final CountDownLatch waitBeforeAsserts = new CountDownLatch(4);
    final ComponentTree componentTree;

    final TestChildComponent child1 = new TestChildComponent();
    final TestChildComponent child2 = new TestChildComponent();

    final Column column = Column.create(mContext).child(child1).child(child2).build();

    ThreadPoolLayoutHandler handler =
        new ThreadPoolLayoutHandler(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    componentTree =
        ComponentTree.create(mContext, column)
            .layoutThreadHandler(handler)
            .useSharedLayoutStateFuture(true)
            .build();

    componentTree.setLithoView(new LithoView(mContext));
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();
    assertThat(componentTree.getLayoutStateFutures()).isEmpty();

    final ComponentTree.LayoutStateFuture[] layoutStateFutures =
        new ComponentTree.LayoutStateFuture[2];
    final boolean[] isReleasedAfterStateUpdate = {false};

    // Testing scenario: during the sync state update, a sync state update is triggered (from the
    // same thread).
    // The sync state update will not be cancelled.
    child1.unlockFinishedLayout = waitBeforeAsserts;
    child1.waitActions =
        new WaitActions() {
          @Override
          public void unblock(ComponentTree.LayoutStateFuture lsf) {
            if (layoutStateFutures[0] == null) {
              layoutStateFutures[0] = lsf;
              componentTree.updateStateSync(column.getGlobalKey(), new TestStateUpdate(), null);
              isReleasedAfterStateUpdate[0] = lsf.isReleased();
            } else {
              layoutStateFutures[1] = lsf;
            }
          }
        };
    child2.unlockFinishedLayout = waitBeforeAsserts;

    componentTree.updateStateSync(column.getGlobalKey(), new TestStateUpdate(), null);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    try {
      waitBeforeAsserts.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // The first lsf is released during layout calculation.
    assertFalse(isReleasedAfterStateUpdate[0]);

    // Both state update calls will finish resolving both children.
    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[0]));
    assertTrue(child2.mLayoutStateFutureList.contains(layoutStateFutures[0]));

    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[1]));
    assertTrue(child2.mLayoutStateFutureList.contains(layoutStateFutures[1]));
  }

  // When scheduling an async state update while a sync state update is still completing, the first
  // layout will be cancelled because the result is redundant.
  @Test
  public void testReleaseRedundantBgLsfFromMain() {
    final CountDownLatch waitBeforeAsserts = new CountDownLatch(3);
    final ComponentTree componentTree;
    final CountDownLatch triggerSyncStateUpdate = new CountDownLatch(1);
    final CountDownLatch waitForSyncStateUpdate = new CountDownLatch(1);

    final TestChildComponent child1 = new TestChildComponent();
    final TestChildComponent child2 = new TestChildComponent();

    final Column column = Column.create(mContext).child(child1).child(child2).build();

    ThreadPoolLayoutHandler handler =
        new ThreadPoolLayoutHandler(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    componentTree =
        ComponentTree.create(mContext, column)
            .layoutThreadHandler(handler)
            .useSharedLayoutStateFuture(true)
            .build();

    componentTree.setLithoView(new LithoView(mContext));
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();
    assertThat(componentTree.getLayoutStateFutures()).isEmpty();

    final ComponentTree.LayoutStateFuture[] layoutStateFutures =
        new ComponentTree.LayoutStateFuture[2];
    final boolean[] isReleasedAfterStateUpdate = {false};

    // Testing scenario: during the sync state update, a sync state update is triggered (from the
    // same thread).
    // The sync state update will not be cancelled.
    child1.unlockFinishedLayout = waitBeforeAsserts;
    child1.waitActions =
        new WaitActions() {
          @Override
          public void unblock(ComponentTree.LayoutStateFuture lsf) {
            if (layoutStateFutures[0] == null) {
              layoutStateFutures[0] = lsf;
              // Notify main thread to start a sync layout
              triggerSyncStateUpdate.countDown();

              // Wait for sync state update to be started and check if this LSF is released.
              try {
                waitForSyncStateUpdate.await(5000, TimeUnit.MILLISECONDS);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }

              isReleasedAfterStateUpdate[0] = lsf.isReleased();
            } else {
              layoutStateFutures[1] = lsf;
            }
          }
        };
    child2.unlockFinishedLayout = waitBeforeAsserts;

    componentTree.updateStateAsync(column.getGlobalKey(), new TestStateUpdate(), null);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    try {
      triggerSyncStateUpdate.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    componentTree.updateStateSync(column.getGlobalKey(), new TestStateUpdate(), null);

    waitForSyncStateUpdate.countDown();

    try {
      waitBeforeAsserts.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // The first lsf is released during layout calculation.
    assertTrue(isReleasedAfterStateUpdate[0]);

    // Only the second state update will resolve both children.
    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[0]));
    assertFalse(child2.mLayoutStateFutureList.contains(layoutStateFutures[0]));

    assertTrue(child1.mLayoutStateFutureList.contains(layoutStateFutures[1]));
    assertTrue(child2.mLayoutStateFutureList.contains(layoutStateFutures[1]));
  }

  final class TestStateUpdate implements ComponentLifecycle.StateUpdate {

    @Override
    public void updateState(StateContainer stateContainer) {}
  }

  private interface WaitActions {
    void unblock(ComponentTree.LayoutStateFuture layoutStateFuture);
  }

  class TestChildComponent extends Component {

    private WaitActions waitActions;
    boolean hasRunLayout;
    CountDownLatch wait;
    CountDownLatch unlockFinishedLayout;
    List<ComponentTree.LayoutStateFuture> mLayoutStateFutureList;

    protected TestChildComponent() {
      this(null, null, null);
    }

    protected TestChildComponent(
        CountDownLatch wait, CountDownLatch unlockFinishedLayout, WaitActions waitActions) {
      super("TestChildComponent");
      this.wait = wait;
      this.unlockFinishedLayout = unlockFinishedLayout;
      this.waitActions = waitActions;
      mLayoutStateFutureList = new ArrayList<>();
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      if (waitActions != null) {
        waitActions.unblock(c.getLayoutStateFuture());
      }

      if (wait != null) {
        try {
          wait.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      hasRunLayout = true;
      mLayoutStateFutureList.add(c.getLayoutStateFuture());

      if (unlockFinishedLayout != null) {
        unlockFinishedLayout.countDown();
      }

      return Column.create(c).build();
    }

    @Override
    Component makeShallowCopyWithNewId() {
      final TestChildComponent component = (TestChildComponent) super.makeShallowCopyWithNewId();
      component.hasRunLayout = false;
      return component;
    }
  }
}
