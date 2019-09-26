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

  // Whether after this loading event the dataset is still empty
  public boolean isEmpty;
  // Either INITIAL_LOAD, LOADING, SUCCEEDED OR FAILED
  public LoadingState loadingState;
  // The reason for LOAD_FAILED events.
  @Nullable public Throwable t;
}
