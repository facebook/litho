/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.facebook.litho.ComponentContext;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class RecyclerEventsControllerTest {

  private TestSectionsRecyclerView mSectionsRecyclerView;
  private RecyclerView mRecyclerView;
  private RecyclerEventsController mRecyclerEventsController;
  private RecyclerEventsController.OnRecyclerUpdateListener mOnRecyclerUpdateListener;
  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    mRecyclerView = new RecyclerView(mComponentContext.getAndroidContext());
    mSectionsRecyclerView =
        new TestSectionsRecyclerView(mComponentContext.getAndroidContext(), mRecyclerView);
    mOnRecyclerUpdateListener = mock(RecyclerEventsController.OnRecyclerUpdateListener.class);
    mRecyclerEventsController = new RecyclerEventsController();
    mRecyclerEventsController.setSectionsRecyclerView(mSectionsRecyclerView);
  }

  @After
  public void teardown() {
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_DISABLED);
  }

  /**
   * Used for setting up initial value required for tests and then resetting the fields of {@code
   * mSectionsRecyclerView}.
   *
   * <p>This is useful when we want to mock certain behavior.
   *
   * <p>Example : when(mSectionsRecyclerView.isRefreshing()).thenReturn(true);
   */
  private void setUpInitialValues(boolean value) {
    mSectionsRecyclerView.setRefreshing(value);
    mSectionsRecyclerView.reset();
  }

  @Test
  public void testOnRecyclerListener() {
    verify(mOnRecyclerUpdateListener, never()).onUpdate(any(RecyclerView.class));

    mRecyclerEventsController.setOnRecyclerUpdateListener(mOnRecyclerUpdateListener);
    mRecyclerEventsController.setSectionsRecyclerView(null);

    mRecyclerEventsController.setSectionsRecyclerView(mSectionsRecyclerView);

    verify(mOnRecyclerUpdateListener, times(1)).onUpdate(null);
    verify(mOnRecyclerUpdateListener, times(1)).onUpdate(mRecyclerView);
  }

  @Test
  public void testClearRefreshingOnNotRefreshingView() {
    setUpInitialValues(false);
    mRecyclerEventsController.clearRefreshing();

    assertThat(mSectionsRecyclerView.getSetRefreshingValues()).isEmpty();
    assertThat(mSectionsRecyclerView.getRemoveCallbackRunnableList()).isEmpty();
    assertThat(mSectionsRecyclerView.getPostRunnableList()).isEmpty();
  }

  @Test
  public void testClearRefreshingFromUIThread() {
    setUpInitialValues(true);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_TRUE);
    mRecyclerEventsController.clearRefreshing();

    assertThat(mSectionsRecyclerView.getSetRefreshingValues().size()).isEqualTo(1);
    assertThat(mSectionsRecyclerView.getSetRefreshingValues().get(0)).isFalse();
    assertThat(mSectionsRecyclerView.getRemoveCallbackRunnableList()).isEmpty();
    assertThat(mSectionsRecyclerView.getPostRunnableList()).isEmpty();
  }

  @Test
  public void testClearRefreshingFromNonUIThread() {

    setUpInitialValues(true);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_FALSE);

    mRecyclerEventsController.clearRefreshing();
    assertThat(mSectionsRecyclerView.getRemoveCallbackRunnableList().size()).isEqualTo(1);
    assertThat(mSectionsRecyclerView.getPostRunnableList().size()).isEqualTo(1);
  }

  @Test
  public void testShowRefreshingFromUIThread() {
    setUpInitialValues(false);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_TRUE);

    mRecyclerEventsController.showRefreshing();
    assertThat(mSectionsRecyclerView.getSetRefreshingValues().get(0)).isTrue();
    assertThat(mSectionsRecyclerView.getSetRefreshingValues().size()).isEqualTo(1);
  }

  @Test
  public void testShowRefreshingAlreadyRefreshing() {
    setUpInitialValues(true);
    mRecyclerEventsController.showRefreshing();

    assertThat(mSectionsRecyclerView.getSetRefreshingValues().size()).isEqualTo(0);
  }

}
