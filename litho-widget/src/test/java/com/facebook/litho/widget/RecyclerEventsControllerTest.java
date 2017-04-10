// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.widget;

import com.facebook.litho.ThreadUtils;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(ComponentsTestRunner.class)
@PrepareForTest(ThreadUtils.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
public class RecyclerEventsControllerTest {

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private RecyclerViewWrapper mRecyclerViewWrapper;
  private RecyclerEventsController mRecyclerEventsController;

  @Before
  public void setup() {
    mRecyclerEventsController = new RecyclerEventsController();
    mRecyclerViewWrapper = mock(RecyclerViewWrapper.class);
    mRecyclerEventsController.setRecyclerViewWrapper(mRecyclerViewWrapper);
    PowerMockito.mockStatic(ThreadUtils.class);
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
    PowerMockito.when(ThreadUtils.isMainThread()).thenReturn(true);

    mRecyclerEventsController.clearRefreshing();

    verify(mRecyclerViewWrapper).setRefreshing(false);
    verify(mRecyclerViewWrapper, never()).removeCallbacks(any(Runnable.class));
    verify(mRecyclerViewWrapper, never()).post(any(Runnable.class));
  }

  @Test
  public void testClearRefreshingFromNonUIThread() {
    when(mRecyclerViewWrapper.isRefreshing()).thenReturn(true);
    PowerMockito.when(ThreadUtils.isMainThread()).thenReturn(false);

    mRecyclerEventsController.clearRefreshing();

    verify(mRecyclerViewWrapper, times(1)).removeCallbacks(any(Runnable.class));
    verify(mRecyclerViewWrapper, times(1)).post(any(Runnable.class));
  }
}
