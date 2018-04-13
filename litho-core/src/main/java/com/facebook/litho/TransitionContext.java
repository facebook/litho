/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import java.util.ArrayList;

/**
 * TransitionContext is unique per LayoutState and contains all the transitions defined
 * in a component tree.
 */
class TransitionContext {

  private final ArrayList<Transition> mTransitions = new ArrayList<>();

  void addTransition(Transition transition) {
    if (transition instanceof Transition.BaseTransitionUnitsBuilder) {
      mTransitions.addAll(
          ((Transition.BaseTransitionUnitsBuilder) transition).getTransitionUnits());
    } else {
      mTransitions.add(transition);
    }
  }

  ArrayList<Transition> getTransitions() {
    return mTransitions;
  }

  void reset() {
    mTransitions.clear();
  }
}
