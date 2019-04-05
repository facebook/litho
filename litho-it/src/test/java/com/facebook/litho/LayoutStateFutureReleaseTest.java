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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateFutureReleaseTest {

  private ComponentContext mContext;
  private final boolean config = ComponentsConfiguration.useCancelableLayoutFutures;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
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
            new WaitActions() {
              @Override
              public void unblock() {
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
            new WaitActions() {
              @Override
              public void unblock() {
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

  private interface WaitActions {
    void unblock();
  }

  class TestChildComponent extends Component {

    private final WaitActions waitActions;
    boolean hasRunLayout;
    CountDownLatch wait;

    protected TestChildComponent() {
      this(null, null);
    }

    protected TestChildComponent(CountDownLatch wait, WaitActions waitActions) {
      super("TestChildComponent");
      this.wait = wait;
      this.waitActions = waitActions;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      if (wait != null && waitActions != null) {
        waitActions.unblock();
        try {
          wait.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      hasRunLayout = true;
      return Column.create(c).build();
    }
  }
}
