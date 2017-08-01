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

  private RecyclerViewWrapper mRecyclerViewWrapper;
  private RecyclerEventsController mRecyclerEventsController;

  @Before
  public void setup() {
    mRecyclerEventsController = new RecyclerEventsController();
    mRecyclerViewWrapper = mock(RecyclerViewWrapper.class);
    mRecyclerEventsController.setRecyclerViewWrapper(mRecyclerViewWrapper);
  }

  @After
  public void teardown() {
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_DISABLED);
  }

  @Test
  public void testClearRefreshingOnNotRefreshingView() {
    when(mRecyclerViewWrapper.isRefreshing()).thenReturn(false);

    mRecyclerEventsController.clearRefreshing();

    verify(mRecyclerViewWrapper, never()).setRefreshing(anyBoolean());
    verify(mRecyclerViewWrapper, never()).removeCallbacks(any(Runnable.class));
    verify(mRecyclerViewWrapper, never()).post(any(Runnable.class));
  }

  @Test
  public void testClearRefreshingFromUIThread() {
    when(mRecyclerViewWrapper.isRefreshing()).thenReturn(true);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_TRUE);

    mRecyclerEventsController.clearRefreshing();

    verify(mRecyclerViewWrapper).setRefreshing(false);
    verify(mRecyclerViewWrapper, never()).removeCallbacks(any(Runnable.class));
    verify(mRecyclerViewWrapper, never()).post(any(Runnable.class));
  }

  @Test
  public void testClearRefreshingFromNonUIThread() {
    when(mRecyclerViewWrapper.isRefreshing()).thenReturn(true);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_FALSE);

    mRecyclerEventsController.clearRefreshing();

    verify(mRecyclerViewWrapper, times(1)).removeCallbacks(any(Runnable.class));
    verify(mRecyclerViewWrapper, times(1)).post(any(Runnable.class));
  }

  @Test
  public void testShowRefreshingFromUIThread() {
    when(mRecyclerViewWrapper.isRefreshing()).thenReturn(false);
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_TRUE);

    mRecyclerEventsController.showRefreshing();
    verify(mRecyclerViewWrapper).setRefreshing(true);
  }

  @Test
  public void testShowRefreshingAlreadyRefreshing() {
    when(mRecyclerViewWrapper.isRefreshing()).thenReturn(true);

    mRecyclerEventsController.showRefreshing();
    verify(mRecyclerViewWrapper, never()).setRefreshing(anyBoolean());
  }
}
