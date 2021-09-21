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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)

/**
 * The StateContainer implementation for Kotlin components. It tracks all the state defined by
 * useState calls for the same component. See KState for how this is being used. This is a purely
 * immutable class and it exposes utilities to create a new instance with either a new piece of
 * state or by changing the value at a given index
 */
public final class KStateContainer extends StateContainer {

  final List<Object> mStates;

  private KStateContainer(@Nullable KStateContainer kStateContainer, @Nullable Object state) {
    final List<Object> states;
    if (kStateContainer != null) {
      states = new ArrayList<>(kStateContainer.mStates.size() + 1);
      states.addAll(kStateContainer.mStates);
    } else {
      states = new ArrayList<>();
    }
    states.add(state);

    mStates = Collections.unmodifiableList(states);
  }

  private KStateContainer(KStateContainer kStateContainer, int index, @Nullable Object newValue) {
    final ArrayList<Object> states = new ArrayList<>(kStateContainer.mStates);
    states.set(index, newValue);
    mStates = Collections.unmodifiableList(states);
  }

  @Override
  public void applyStateUpdate(StateUpdate stateUpdate) {
    throw new UnsupportedOperationException(
        "Kotlin states should not be updated through applyStateUpdate calls");
  }

  public static KStateContainer withNewState(
      @Nullable KStateContainer kStateContainer, @Nullable Object state) {
    return new KStateContainer(kStateContainer, state);
  }

  public KStateContainer copyAndMutate(int index, @Nullable Object newValue) {
    return new KStateContainer(this, index, newValue);
  }
}
