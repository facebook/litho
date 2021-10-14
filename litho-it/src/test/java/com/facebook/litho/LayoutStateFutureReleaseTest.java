/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Looper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.RunnableHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(LithoTestRunner.class)
public class LayoutStateFutureReleaseTest {
  private int mWidthSpec;
  private int mHeightSpec;

  private ComponentContext mContext;
  private final boolean config =
      ComponentsConfiguration.getDefaultComponentsConfiguration().getUseCancelableLayoutFutures();
  private ShadowLooper mLayoutThreadShadowLooper;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.getDefaultComponentsConfigurationBuilder()
            .useCancelableLayoutFutures(true));

    mWidthSpec = makeSizeSpec(40, EXACTLY);
    mHeightSpec = makeSizeSpec(40, EXACTLY);

    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.getDefaultComponentsConfigurationBuilder()
            .useCancelableLayoutFutures(config));
  }

  @Test
  public void testStopResolvingRowChildrenIfLsfReleased() {
    final ComponentTree.LayoutStateFuture layoutStateFuture =
        mock(ComponentTree.LayoutStateFuture.class);

    when(layoutStateFuture.isReleased()).thenReturn(false);
    final ComponentContext c = new ComponentContext(mContext);

    final LayoutState layoutState = new LayoutState(c);
    final LayoutStateContext layoutStateContext =
        new LayoutStateContext(layoutState, new StateHandler(), null, layoutStateFuture, null);
    c.setLayoutStateContext(layoutStateContext);

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

    final InternalNode result = row.resolve(layoutStateContext, c);
    assertTrue(child1.hasRunLayout);
    assertFalse(child2.hasRunLayout);
    assertNull(result);
  }

  @Test
  public void testStopResolvingColumnChildrenIfLsfReleased() {
    final ComponentTree.LayoutStateFuture layoutStateFuture =
        mock(ComponentTree.LayoutStateFuture.class);

    when(layoutStateFuture.isReleased()).thenReturn(false);
    final ComponentContext c = new ComponentContext(mContext);
    final LayoutState layoutState = new LayoutState(c);
    final LayoutStateContext layoutStateContext =
        new LayoutStateContext(layoutState, new StateHandler(), null, layoutStateFuture, null);
    c.setLayoutStateContext(layoutStateContext);

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

    final InternalNode result = column.resolve(layoutStateContext, c);
    assertTrue(child1.hasRunLayout);
    assertFalse(child2.hasRunLayout);
    assertNull(result);
  }

  // This test is similar to testMainWaitingOnBgBeforeRelease, except that the bg thread
  // LayoutStateFuture gets released after the sync layout is triggered. In this case the UI thread
  // should not be blocked on the bg thread anymore, because the released Lsf will return a null
  // LayoutState.
  @Test
  public void testDontWaitOnReleasedLSF() {
    final CountDownLatch waitBeforeAsserts = new CountDownLatch(1);
    final CountDownLatch scheduleSyncLayout = new CountDownLatch(1);
    final CountDownLatch finishBgLayout = new CountDownLatch(1);

    final ComponentTree componentTree;

    final TestChildComponent child1 = new TestChildComponent();

    final Column column_0 = Column.create(mContext).child(new TestChildComponent()).build();
    final Column column = Column.create(mContext).child(child1).build();

    RunnableHandler handler =
        ThreadPoolLayoutHandler.getNewInstance(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    componentTree =
        ComponentTree.create(mContext, column_0)
            .componentsConfiguration(
                ComponentsConfiguration.create().useCancelableLayoutFutures(true).build())
            .layoutThreadHandler(handler)
            .build();

    componentTree.setLithoView(new LithoView(mContext));

    final ComponentTree.LayoutStateFuture[] layoutStateFutures =
        new ComponentTree.LayoutStateFuture[2];

    // Testing scenario: we schedule a LSF on bg thread which gets released before compat UI thread
    // layout
    // is scheduled.
    child1.waitActions =
        new WaitActions() {
          @Override
          public void unblock(ComponentTree.LayoutStateFuture lsf) {
            // Something happens here which releases the ongoing lsf, such as a state update
            // triggered from onCreateLayout.
            if (layoutStateFutures[0] == null) {
              layoutStateFutures[0] = lsf;

              lsf.release();
              scheduleSyncLayout.countDown();

              try {
                finishBgLayout.await(5000, TimeUnit.MILLISECONDS);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }

              waitBeforeAsserts.countDown();
            } else {
              layoutStateFutures[1] = lsf;
              finishBgLayout.countDown();
            }
          }
        };

    componentTree.setRootAndSizeSpecAsync(column, mWidthSpec, mHeightSpec);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    try {
      scheduleSyncLayout.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    componentTree.setRootAndSizeSpec(column, mWidthSpec, mHeightSpec, new Size());

    try {
      waitBeforeAsserts.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertNotNull(layoutStateFutures[0]);
    assertNotNull(layoutStateFutures[1]);
  }

  private StateContainer.StateUpdate createTestStateUpdate() {
    return new StateContainer.StateUpdate(0);
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
      this.wait = wait;
      this.unlockFinishedLayout = unlockFinishedLayout;
      this.waitActions = waitActions;
      mLayoutStateFutureList = new ArrayList<>();
    }

    @Override
    protected RenderResult render(ComponentContext c, StateHandler stateHandler) {
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

      return new RenderResult(Column.create(c).build());
    }
  }
}
