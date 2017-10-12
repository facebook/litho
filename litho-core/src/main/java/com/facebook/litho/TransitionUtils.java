/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import com.facebook.litho.animation.AnimatedProperties;
import java.util.ArrayList;

/** Utilities to interact with {@link Transition}. */
class TransitionUtils {

  /**
   * @return whether the given Transition object specifies a bounds transition on the given
   *     transition key.
   */
  public static boolean hasBoundsAnimation(String transitionKey, Transition transition) {
    if (transition instanceof TransitionSet) {
      ArrayList<Transition> children = ((TransitionSet) transition).getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        if (hasBoundsAnimation(transitionKey, children.get(i))) {
          return true;
        }
      }

      return false;
    }

    if (transition instanceof Transition.TransitionUnit) {
      final Transition.TransitionUnit transitionUnit = (Transition.TransitionUnit) transition;
      return transitionUnit.targetsKey(transitionKey)
          && (transitionUnit.targetsProperty(AnimatedProperties.HEIGHT));
    }

    if (transition instanceof Transition.TransitionUnitsBuilder) {
      final Transition.TransitionUnitsBuilder builder =
          (Transition.TransitionUnitsBuilder) transition;
      ArrayList<Transition.TransitionUnit> units = builder.getTransitionUnits();
      for (int i = 0, size = units.size(); i < size; i++) {
        if (hasBoundsAnimation(transitionKey, units.get(i))) {
          return true;
        }
      }

      return false;
    }

    throw new RuntimeException("Unhandled transition type: " + transition);
  }
}
