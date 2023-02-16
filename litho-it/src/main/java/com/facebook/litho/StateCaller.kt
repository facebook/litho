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

/** This class is used to trigger state updates within components. */
class StateCaller {
  fun interface StateUpdateListener {
    fun update()
  }

  private var stateUpdateListener: StateUpdateListener? = null

  fun setStateUpdateListener(stateUpdateListener: StateUpdateListener?) {
    this.stateUpdateListener = stateUpdateListener
  }

  fun update() {
    stateUpdateListener?.update()
  }
}
