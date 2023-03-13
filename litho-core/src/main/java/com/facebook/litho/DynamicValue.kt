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

import androidx.annotation.UiThread
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Class represents a DynamicValue, and provides users with ability to change the value, by exposing
 * one public method [set] It also allows attaching listeners (package level access), and takes care
 * of notifying them when the held value changes.
 *
 * @param <T> type of value held </T>
 */
open class DynamicValue<T>(initialValue: T) {

  private var value: T = initialValue
    set(value) {
      if (field === value || field == value) return

      field = value
      for (listener in listeners) {
        listener.onValueChange(this)
      }
    }

  private val listeners: MutableSet<OnValueChangeListener<T>> = CopyOnWriteArraySet()

  /**
   * Sets current value and notifies all the attached listeners
   *
   * IMPORTANT: This should only be called from the main thread! If it isn't, it can race with other
   * set() calls!
   *
   * @param value the new value
   */
  @UiThread
  fun set(value: T) {
    this.value = value
  }

  /**
   * Retrieves the current value.
   *
   * IMPORTANT: This should only be called from the main thread! If it isn't, it can race with other
   * set() calls! Generally, you shouldn't need to call get() yourself: instead the DynamicValue
   * should be passed into a property or Style which accepts a DynamicValue.
   *
   * @return the current value
   */
  @UiThread fun get(): T = value

  /**
   * Register a callback to be invoked when the value changes.
   *
   * @param listener The callback to invoke.
   */
  fun attachListener(listener: OnValueChangeListener<T>) {
    listeners.add(listener)
  }

  /**
   * Remove a callback.
   *
   * @param listener The listener to detach
   */
  fun detach(listener: OnValueChangeListener<T>) {
    listeners.remove(listener)
  }

  /** Retrieves the amount of listeners attached to this [DynamicValue]. */
  val numberOfListeners: Int
    get() = listeners.size

  /** Interface definition for a callback to be invoked when value changes. */
  fun interface OnValueChangeListener<T> {
    /**
     * Called when a view has been clicked.
     *
     * @param value The dynamic value.
     */
    fun onValueChange(value: DynamicValue<T>)
  }
}
