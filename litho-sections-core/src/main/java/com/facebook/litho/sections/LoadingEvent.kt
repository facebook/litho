/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.sections

import com.facebook.litho.annotations.Event
import com.facebook.litho.annotations.EventHandlerRebindMode
import kotlin.jvm.JvmField

/**
 * Sections should implement this method to receive events about their children's loading state. An
 * example of the correct usage is:
 * ```
 * @OnEvent(LoadingEvent.class)
 * static void onLoadingStateChanged(
 *  SectionContext context,
 *  @FromEvent LoadingState loadingState,
 *  @Prop int someProp) {
 *    context.updateLoadingState(loadingState);
 * }
 * ```
 */
@Event(mode = EventHandlerRebindMode.NONE)
class LoadingEvent {

  enum class LoadingState {
    INITIAL_LOAD,
    LOADING,
    SUCCEEDED,
    FAILED
  }

  // Whether after this loading event the dataset is still empty
  @JvmField var isEmpty = false

  // Either INITIAL_LOAD, LOADING, SUCCEEDED OR FAILED
  lateinit var loadingState: LoadingState

  // The reason for LOAD_FAILED events.
  @JvmField var t: Throwable? = null
}
