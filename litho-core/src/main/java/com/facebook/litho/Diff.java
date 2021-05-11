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

import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import javax.annotation.Nullable;

/**
 * Represents a diff between two {@code T} values. It should be used when defining the {@link
 * ShouldUpdate} callback in a ComponentSpec and may be used in {@link OnCreateTransition} to define
 * animations based on incoming changes. A Diff holds the previous and next value for a specific
 * {@link Prop} or {@link State} for a ComponentSpec.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class Diff<T> {

  private final @Nullable T mPrevious;
  private @Nullable T mNext;

  public Diff(@Nullable T previous, @Nullable T next) {
    mPrevious = previous;
    mNext = next;
  }

  @Nullable
  public T getPrevious() {
    return mPrevious;
  }

  @Nullable
  public T getNext() {
    return mNext;
  }

  public void setNext(@Nullable T next) {
    mNext = next;
  }

  @Override
  public String toString() {
    return "Diff{" + "mPrevious=" + mPrevious + ", mNext=" + mNext + '}';
  }
}
