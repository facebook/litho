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

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.facebook.litho.Handle;
import com.facebook.litho.widget.SmoothScrollAlignmentType;

/**
 * This is an internal class used by {@link SectionTree} to dispatch the last known focus request
 * when the data is not rendered on the UI.
 *
 * <p>FocusDispatcher uses two states to determine if the focus request should be dispatched:
 *
 * <p>1) Data fetching state 2) Data binding state
 *
 * <p>Both of these states have to be completed before the focus request can be dispatched.
 * Otherwise, the last known request will be kept until the states are valid again.
 */
class FocusDispatcher {

  private final SectionTree.Target mTarget;
  private @Nullable FocusRequest mFocusRequest;
  private @Nullable LoadingEvent.LoadingState mLoadingState;
  private boolean mWaitForDataBound;

  FocusDispatcher(SectionTree.Target target) {
    mTarget = target;
  }

  /**
   * Request focus to a specific index position.
   *
   * @param index position to focus on.
   */
  @UiThread
  void requestFocus(int index) {
    requestFocusWithOffset(index, 0);
  }

  @UiThread
  void requestSmoothFocus(int index, int offset, SmoothScrollAlignmentType type) {
    if (shouldDispatchRequests()) {
      mTarget.requestSmoothFocus(index, offset, type);
      return;
    }

    queueRequest(index, 0);
  }

  @UiThread
  void requestSmoothFocus(Handle target, int offset, SmoothScrollAlignmentType type) {
    if (shouldDispatchRequests()) {
      mTarget.requestSmoothFocus(target, offset, type);
      return;
    }
  }

  /**
   * Request focus to a specific index position with an offset.
   *
   * @param index position to focus on.
   * @param offset
   */
  @UiThread
  void requestFocusWithOffset(int index, int offset) {
    if (shouldDispatchRequests()) {
      mTarget.requestFocusWithOffset(index, offset);
      return;
    }

    queueRequest(index, offset);
  }

  @UiThread
  void requestFocusWithOffset(Handle target, int offset) {
    if (shouldDispatchRequests()) {
      mTarget.requestFocusWithOffset(target, offset);
      return;
    }
  }

  /** Dispatch focus request if there is an existing request and the states are valid. */
  @UiThread
  void maybeDispatchFocusRequests() {
    if (mFocusRequest == null || !shouldDispatchRequests()) {
      return;
    }

    mTarget.requestFocusWithOffset(mFocusRequest.index, mFocusRequest.offset);
    mFocusRequest = null;
  }

  /**
   * Set the state of the data fetch to the dispatcher.
   *
   * @param loadingState {@link LoadingEvent.LoadingState} of the data fetch
   */
  @UiThread
  void setLoadingState(LoadingEvent.LoadingState loadingState) {
    mLoadingState = loadingState;
  }

  /**
   * Set to false if waitForDataBound data fetched is bounded to the UI. Otherwise, set it to true.
   *
   * @param waitForDataBound should wait for data to be bounded
   */
  @UiThread
  void waitForDataBound(boolean waitForDataBound) {
    mWaitForDataBound = waitForDataBound;
  }

  /** @return true if the data fetching has been completed. */
  @UiThread
  boolean isLoadingCompleted() {
    return mLoadingState == null
        || mLoadingState == LoadingEvent.LoadingState.FAILED
        || mLoadingState == LoadingEvent.LoadingState.SUCCEEDED;
  }

  private boolean shouldDispatchRequests() {
    return isLoadingCompleted() && !mWaitForDataBound;
  }

  private void queueRequest(int index, int offset) {
    mFocusRequest = new FocusRequest(index, offset);
  }

  private static class FocusRequest {
    private final int index;
    private final int offset;

    private FocusRequest(int index, int offset) {
      this.index = index;
      this.offset = offset;
    }
  }
}
