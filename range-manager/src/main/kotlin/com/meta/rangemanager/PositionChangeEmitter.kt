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

package com.meta.rangemanager

/**
 * A source of position-change events that a [RangeManager] can subscribe to.
 *
 * Implement this interface to bridge any scroll or position source (RecyclerView, custom widget,
 * etc.) into a RangeManager. The [subscribe] method is called once when the RangeManager is
 * created; the implementation should call [onPositionChange] whenever the viewport moves.
 *
 * @param Position The type representing the current position or viewport state
 */
fun interface PositionChangeEmitter<Position> {
  fun subscribe(onPositionChange: (Position) -> Unit)
}
