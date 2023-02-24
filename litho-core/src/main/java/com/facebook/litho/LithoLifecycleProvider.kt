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

/**
 * Manages a Litho ComponentTree lifecycle and informs subscribed LithoLifecycleListeners when a
 * lifecycle state occurs.
 */
interface LithoLifecycleProvider {

  enum class LithoLifecycle(private val text: String) {
    HINT_VISIBLE("HINT_VISIBLE"),
    HINT_INVISIBLE("HINT_INVISIBLE"),
    DESTROYED("DESTROYED");

    fun equalsName(otherName: String): Boolean = text == otherName

    override fun toString(): String = text
  }

  fun moveToLifecycle(lithoLifecycle: LithoLifecycle)

  val lifecycleStatus: LithoLifecycle

  fun addListener(listener: LithoLifecycleListener)

  fun removeListener(listener: LithoLifecycleListener)
}
