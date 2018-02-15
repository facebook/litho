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
 * Sections should implement this method to receive events about their children's loading state. An
 * example of the correct usage is:
 *
 * <pre>
 * <code>
 *
 * {@literal @}OnEvent(LoadingEvent.class)
 *  static void onLoadingStateChanged(
 *     {@literal @}FromEvent LoadingState loadingState,
 *     {@literal @}Param SectionContext context,
 *     {@literal @}Prop int someProp) {
 *       context.updateLoadingState(loadingState);
 *  }
 * </code>
 * </pre>
 */
@Event
public class LoadingEvent {
  public enum LoadingState {
    INITIAL_LOAD,
    LOADING,
    SUCCEEDED,
    FAILED
  }

  //Whether after this loading event the dataset is still empty
  public boolean isEmpty;
  // Either INITIAL_LOAD, LOADING, SUCCEEDED OR FAILED
  public LoadingState loadingState;
  // The reason for LOAD_FAILED events.
  @Nullable public Throwable t;
}
