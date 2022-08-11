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

import androidx.annotation.UiThread;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Class represents a DynamicValue, and provides users with ability to change the value, by exposing
 * one public method {@link #set(Object)} It also allows attaching listeners (package level access),
 * and takes care of notifying them when the held value changes.
 *
 * <p>Nullsafe annotation have been removed from this file as infer asks for a T!! type in Kotlin
 * 1.5 progressive and T & Any type in 1.6 progressive
 *
 * @param <T> type of value held
 */
public class DynamicValue<T> {

  private T mValue;
  private final Set<OnValueChangeListener<T>> mListeners = new CopyOnWriteArraySet<>();

  public DynamicValue(T mValue) {
    this.mValue = mValue;
  }

  /**
   * Sets current value and notifies all the attached listeners
   *
   * <p>IMPORTANT: This should only be called from the main thread! If it isn't, it can race with
   * other set() calls!
   *
   * @param value the new value
   */
  @UiThread
  public void set(T value) {
    if (mValue == value || (mValue != null && mValue.equals(value))) {
      return;
    }

    mValue = value;

    for (OnValueChangeListener<T> listener : mListeners) {
      listener.onValueChange(this);
    }
  }

  /**
   * Retrieves the current value.
   *
   * <p>IMPORTANT: This should only be called from the main thread! If it isn't, it can race with
   * other set() calls! Generally, you shouldn't need to call get() yourself: instead the
   * DynamicValue should be passed into a property or Style which accepts a DynamicValue.
   *
   * @return the current value
   */
  @UiThread
  public T get() {
    return mValue;
  }

  /**
   * Register a callback to be invoked when the value changes.
   *
   * @param listener The callback to invoke.
   */
  void attachListener(OnValueChangeListener<T> listener) {
    mListeners.add(listener);
  }

  /**
   * Remove a callback.
   *
   * @param listener The listener to detach
   */
  void detach(OnValueChangeListener<T> listener) {
    mListeners.remove(listener);
  }

  /** Retrieves the amount of listeners attached to this [DynamicValue]. */
  int getNumberOfListeners() {
    return mListeners.size();
  }

  /** Interface definition for a callback to be invoked when value changes. */
  interface OnValueChangeListener<T> {
    /**
     * Called when a view has been clicked.
     *
     * @param value The dynamic value.
     */
    void onValueChange(DynamicValue<T> value);
  }
}
