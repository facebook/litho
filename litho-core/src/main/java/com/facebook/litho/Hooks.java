/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadConfined;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds information about the hooks of one component. */
@ThreadConfined(ThreadConfined.ANY)
public class Hooks {

  private static final int INITIAL_MEMOIZED_VALUES_CAPACITY = 4;

  private List<Object> mMemoizedValues;
  private int mIndex;

  Hooks() {
    this(null);
  }

  @SuppressWarnings("CopyConstructorMissesField")
  Hooks(@Nullable Hooks other) {
    mMemoizedValues =
        (other == null)
            ? new ArrayList<>(INITIAL_MEMOIZED_VALUES_CAPACITY)
            : new ArrayList<>(other.mMemoizedValues);
  }

  int getAndIncrementHookIndex() {
    return mIndex++;
  }

  @SuppressWarnings("unchecked")
  <T> T get(int index) {
    return (T) mMemoizedValues.get(index);
  }

  <T> void set(int index, T value) {
    mMemoizedValues.set(index, value);
  }

  <T> void add(T value) {
    mMemoizedValues.add(value);
  }

  /** Returns true if mMemoizedValues has the element at the specified position. */
  boolean has(int index) {
    return index < mMemoizedValues.size();
  }

  int size() {
    return mMemoizedValues.size();
  }

  @VisibleForTesting
  List<Object> getMemoizedValues() {
    return Collections.unmodifiableList(mMemoizedValues);
  }
}
