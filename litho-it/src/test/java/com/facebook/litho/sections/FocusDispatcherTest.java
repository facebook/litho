/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link FocusDispatcher} */
@RunWith(ComponentsTestRunner.class)
public class FocusDispatcherTest {

  private FocusDispatcher mFocusDispatcher;
  private TestTarget mTarget;

  @Before
  public void setup() {
    mTarget = new TestTarget();
    mFocusDispatcher = new FocusDispatcher(mTarget);
  }

  @Test
  public void testIsLoadingCompleted() {
    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.INITIAL_LOAD);
    assertThat(mFocusDispatcher.isLoadingCompleted()).isFalse();

    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.LOADING);
    assertThat(mFocusDispatcher.isLoadingCompleted()).isFalse();

    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.REFRESH_LOADING);
    assertThat(mFocusDispatcher.isLoadingCompleted()).isFalse();

    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.FAILED);
    assertThat(mFocusDispatcher.isLoadingCompleted()).isTrue();

    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.SUCCEEDED);
    assertThat(mFocusDispatcher.isLoadingCompleted()).isTrue();
  }

  @Test
  public void testImmediateDispatchWithLoadSuccess() {
    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.SUCCEEDED);
    mFocusDispatcher.waitForDataBound(false);

    int index = 1;
    mFocusDispatcher.requestFocus(index);
    assertThat(mTarget.getFocusedTo()).isEqualTo(index);
    assertThat(mTarget.getFocusedToOffset()).isEqualTo(0);
  }

  @Test
  public void testImmediateDispatchWithLoadFailure() {
    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.FAILED);
    mFocusDispatcher.waitForDataBound(false);

    int index = 1;
    mFocusDispatcher.requestFocus(index);
    assertThat(mTarget.getFocusedTo()).isEqualTo(index);
    assertThat(mTarget.getFocusedToOffset()).isEqualTo(0);
  }

  @Test
  public void testDispatchFocusRequestWithLoadSuccess() {
    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.REFRESH_LOADING);
    mFocusDispatcher.waitForDataBound(true);

    int index = 1;
    mFocusDispatcher.requestFocus(index);
    assertThat(mTarget.getFocusedTo()).isEqualTo(-1);
    assertThat(mTarget.getFocusedToOffset()).isEqualTo(-1);

    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.SUCCEEDED);
    mFocusDispatcher.maybeDispatchFocusRequests();
    assertThat(mTarget.getFocusedTo()).isEqualTo(-1);
    assertThat(mTarget.getFocusedToOffset()).isEqualTo(-1);

    mFocusDispatcher.waitForDataBound(false);
    mFocusDispatcher.maybeDispatchFocusRequests();
    assertThat(mTarget.getFocusedTo()).isEqualTo(index);
    assertThat(mTarget.getFocusedToOffset()).isEqualTo(0);
  }

  @Test
  public void testDispatchFocusRequestWithLoadFailure() {
    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.LOADING);
    mFocusDispatcher.waitForDataBound(true);

    int index = 2;
    int offset = 3;
    mFocusDispatcher.requestFocusWithOffset(index, offset);
    assertThat(mTarget.getFocusedTo()).isEqualTo(-1);
    assertThat(mTarget.getFocusedToOffset()).isEqualTo(-1);

    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.FAILED);
    mFocusDispatcher.maybeDispatchFocusRequests();
    assertThat(mTarget.getFocusedTo()).isEqualTo(-1);
    assertThat(mTarget.getFocusedToOffset()).isEqualTo(-1);

    mFocusDispatcher.waitForDataBound(false);
    mFocusDispatcher.maybeDispatchFocusRequests();
    assertThat(mTarget.getFocusedTo()).isEqualTo(index);
    assertThat(mTarget.getFocusedToOffset()).isEqualTo(offset);
  }
}
