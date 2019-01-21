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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.support.v7.widget.RecyclerView;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class RecyclerEventsControllerTest {

  private SectionsRecyclerView mSectionsRecyclerView;
  private RecyclerView mRecyclerView;
  private RecyclerEventsController mRecyclerEventsController;
  private RecyclerEventsController.OnRecyclerUpdateListener mOnRecyclerUpdateListener;

  @Before
  public void setup() {
    mSectionsRecyclerView = mock(SectionsRecyclerView.class);
    mRecyclerView = mock(RecyclerView.class);
    mOnRecyclerUpdateListener = mock(RecyclerEventsController.OnRecyclerUpdateListener.class);

    when(mSectionsRecyclerView.getRecyclerView()).thenReturn(mRecyclerView);

    mRecyclerEventsController = new RecyclerEventsController();
    mRecyclerEventsController.setSectionsRecyclerView(mSectionsRecyclerView);
  }

  @After
  public void teardown() {
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_DISABLED);
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
    when(mSectionsRecyclerView.isRefreshing()).thenReturn(false);

    mRecyclerEventsController.clearRefreshing();

    verify(mSectionsRecyclerView, never()).setRefreshing(anyBoolean());
    verify(mSectionsRecyclerView, never()).removeCallbacks(any(Runnable.class));
    verify(mSectionsRecyclerView, never()).post(any(Runnable.class));
  }

  @Test
  public void testClearRefreshingFromUIThread() {
    when(mSectionsRecyclerView.isRefreshing()).thenReturn(true);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_TRUE);

    mRecyclerEventsController.clearRefreshing();

    verify(mSectionsRecyclerView).setRefreshing(false);
    verify(mSectionsRecyclerView, never()).removeCallbacks(any(Runnable.class));
    verify(mSectionsRecyclerView, never()).post(any(Runnable.class));
  }

  @Test
  public void testClearRefreshingFromNonUIThread() {
    when(mSectionsRecyclerView.isRefreshing()).thenReturn(true);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_FALSE);

    mRecyclerEventsController.clearRefreshing();

    verify(mSectionsRecyclerView, times(1)).removeCallbacks(any(Runnable.class));
    verify(mSectionsRecyclerView, times(1)).post(any(Runnable.class));
  }

  @Test
  public void testShowRefreshingFromUIThread() {
    when(mSectionsRecyclerView.isRefreshing()).thenReturn(false);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_TRUE);

    mRecyclerEventsController.showRefreshing();
    verify(mSectionsRecyclerView).setRefreshing(true);
  }

  @Test
  public void testShowRefreshingAlreadyRefreshing() {
    when(mSectionsRecyclerView.isRefreshing()).thenReturn(true);

    mRecyclerEventsController.showRefreshing();
    verify(mSectionsRecyclerView, never()).setRefreshing(anyBoolean());
  }
}
