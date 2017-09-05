/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import com.facebook.litho.annotations.Event;
import javax.annotation.Nullable;

/**
 * ListComponents should implement this method to receive events about their children loading state.
 * An example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(LoadingEvent.class)
 * static void onLoadingStateChanged(
 *    @FromEvent LoadingState loadingState,
 *    @Param ListContext context,
 *    @Prop int someProp) {
 *      context.updateLoadingState(loadingState);
 * }
 * </pre>
 */
@Event
public class LoadingEvent {
  public enum LoadingState {INITIAL_LOAD, LOADING, SUCCEEDED, FAILED}

  //Whether after this loading event the dataset is still empty
  public boolean isEmpty;
  // Either INITIAL_LOAD, LOADING, SUCCEEDED OR FAILED
  public LoadingState loadingState;
  // The reason for LOAD_FAILED events.
  public @Nullable Throwable t;
}
