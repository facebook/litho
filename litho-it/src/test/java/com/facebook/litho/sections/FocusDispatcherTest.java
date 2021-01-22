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

package com.facebook.litho.sections;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/** Tests {@link FocusDispatcher} */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
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
    mFocusDispatcher.setLoadingState(LoadingEvent.LoadingState.LOADING);
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
