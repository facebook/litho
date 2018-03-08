/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import com.facebook.litho.animation.AnimatedProperty;
import java.util.ArrayList;

/** Utilities to interact with {@link Transition}. */
class TransitionUtils {

  /**
   * @return whether the given Transition object specifies an animation property on the given
   *     transition key.
   */
  public static boolean hasAnimationForProperty(
      String transitionKey, Transition transition, AnimatedProperty property) {
    if (transition instanceof TransitionSet) {
      ArrayList<Transition> children = ((TransitionSet) transition).getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        if (hasAnimationForProperty(transitionKey, children.get(i), property)) {
          return true;
        }
      }

      return false;
    }

    if (transition instanceof Transition.TransitionUnit) {
      final Transition.TransitionUnit transitionUnit = (Transition.TransitionUnit) transition;
      return transitionUnit.targetsKey(transitionKey) && (transitionUnit.targetsProperty(property));
    }

    if (transition instanceof Transition.BaseTransitionUnitsBuilder) {
      final Transition.BaseTransitionUnitsBuilder builder =
          (Transition.BaseTransitionUnitsBuilder) transition;
      ArrayList<Transition.TransitionUnit> units = builder.getTransitionUnits();
      for (int i = 0, size = units.size(); i < size; i++) {
        if (hasAnimationForProperty(transitionKey, units.get(i), property)) {
          return true;
        }
      }

      return false;
    }

    throw new RuntimeException("Unhandled transition type: " + transition);
  }
}
