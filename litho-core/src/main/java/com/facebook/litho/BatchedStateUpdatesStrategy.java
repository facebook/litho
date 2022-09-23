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

package com.facebook.litho;

import com.facebook.infer.annotation.Nullsafe;

/**
 * A Batched State Update Strategy will define when do we schedule a final state update and
 * corresponding layout calculation, so that we can enqueue the most reasonable number of state
 * updates without incurring with an excessive number of layout calculations.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface BatchedStateUpdatesStrategy {

  /**
   * This method is called whenever an async state update was enqueued and added to the {@link
   * StateHandler}.
   *
   * <p>This method should return {@code true} if it is considering this update to its batching
   * strategy; otherwise it should simply return {@code false}.
   */
  boolean onAsyncStateUpdateEnqueued(String attribution, boolean isCreateLayoutInProgress);

  /**
   * This method is called whenever {@link ComponentTree#updateStateInternal(boolean, String,
   * boolean)} is called.
   *
   * <p>This will help the batching strategy on decide how to update its internal state by knowing
   * that all current enqueued updates will be consumed soon.
   */
  void onInternalStateUpdateStart();

  /**
   * This method should be called whenever the resources held by this strategy should be freed or
   * reset.
   *
   * <p>This can be pending {@code Runnable}, state variables, etc.
   */
  void release();
}
