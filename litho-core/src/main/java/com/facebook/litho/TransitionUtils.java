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

import android.content.Context;
import android.provider.Settings;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.config.ComponentsConfiguration;
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

  static boolean areTransitionsEnabled(Context context) {
    if (!ComponentsConfiguration.ARE_TRANSITIONS_SUPPORTED) {
      // Transitions use some APIs that are not available before ICS.
      return false;
    }

    if (!ComponentsConfiguration.isEndToEndTestRun) {
      return true;
    }

    if (!ComponentsConfiguration.CAN_CHECK_GLOBAL_ANIMATOR_SETTINGS) {
      return false;
    }

    float animatorDurationScale =
        Settings.Global.getFloat(
            context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
    return ComponentsConfiguration.forceEnableTransitionsForInstrumentationTests
        || animatorDurationScale != 0f;
  }
}
