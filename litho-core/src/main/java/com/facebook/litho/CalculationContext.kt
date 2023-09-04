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

package com.facebook.litho

import android.util.Pair

interface CalculationContext {

  val treeId: Int
  val cache: MeasuredResultCache
  val treeState: TreeState
  val layoutVersion: Int
  val rootComponentId: Int
  val isFutureReleased: Boolean
  val treeFuture: TreeFuture<*>?
  val eventHandlers: List<Pair<String, EventHandler<*>>>?
  val isAccessibilityEnabled: Boolean

  /**
   * Records a Spec-generated EventHandler. This EventHandlers are an output of the calculation and
   * will be used by [EventHandlersController] to rebind existing EventHandlers.
   */
  fun recordEventHandler(globalKey: String, eventHandler: EventHandler<*>)
}
