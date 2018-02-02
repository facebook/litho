/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.litho.ThreadUtils;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class RecyclerEventsControllerTest {

  private SectionsRecyclerView mSectionsRecyclerView;
  private RecyclerEventsController mRecyclerEventsController;

  @Before
  public void setup() {
    mRecyclerEventsController = new RecyclerEventsController();
    mSectionsRecyclerView = mock(SectionsRecyclerView.class);
    mRecyclerEventsController.setSectionsRecyclerView(mSectionsRecyclerView);
  }

  @After
  public void teardown() {
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_DISABLED);
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
