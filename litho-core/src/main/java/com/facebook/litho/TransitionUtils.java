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

import android.content.Context;
import android.provider.Settings;
import androidx.annotation.Nullable;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.config.ComponentsConfiguration;
import java.util.ArrayList;
import java.util.List;

/** Utilities to interact with {@link Transition}. */
class TransitionUtils {

  /**
   * Collects info about root component bounds animation, specifically whether it has animations for
   * width and height also {@link Transition.TransitionUnit} for appear animation if such is
   * defined.
   */
  static void collectRootBoundsTransitions(
      TransitionId rootTransitionId,
      Transition transition,
      AnimatedProperty property,
      Transition.RootBoundsTransition outRootBoundsTransition) {
    if (transition instanceof TransitionSet) {
      ArrayList<Transition> children = ((TransitionSet) transition).getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        collectRootBoundsTransitions(
            rootTransitionId, children.get(i), property, outRootBoundsTransition);
      }
    } else if (transition instanceof Transition.TransitionUnit) {
      final Transition.TransitionUnit transitionUnit = (Transition.TransitionUnit) transition;
      if (transitionUnit.targets(rootTransitionId) && transitionUnit.targetsProperty(property)) {
        outRootBoundsTransition.hasTransition = true;
        if (transitionUnit.hasAppearAnimation()) {
          outRootBoundsTransition.appearTransition = transitionUnit;
        }
      }
    } else if (transition instanceof Transition.BaseTransitionUnitsBuilder) {
      final Transition.BaseTransitionUnitsBuilder builder =
          (Transition.BaseTransitionUnitsBuilder) transition;
      ArrayList<Transition.TransitionUnit> units = builder.getTransitionUnits();
      for (int i = 0, size = units.size(); i < size; i++) {
        collectRootBoundsTransitions(
            rootTransitionId, units.get(i), property, outRootBoundsTransition);
      }
    } else {
      throw new RuntimeException("Unhandled transition type: " + transition);
    }
  }

  static boolean areTransitionsEnabled(@Nullable Context context) {
    if (!ComponentsConfiguration.ARE_TRANSITIONS_SUPPORTED) {
      // Transitions use some APIs that are not available before ICS.
      return false;
    }

    if (ComponentsConfiguration.isAnimationDisabled) {
      // mostly for unit tests
      return false;
    }

    if (!ComponentsConfiguration.isEndToEndTestRun) {
      return true;
    }

    if (!ComponentsConfiguration.CAN_CHECK_GLOBAL_ANIMATOR_SETTINGS) {
      return false;
    }

    if (context == null) {
      return false;
    }

    float animatorDurationScale =
        Settings.Global.getFloat(
            context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
    return ComponentsConfiguration.forceEnableTransitionsForInstrumentationTests
        || animatorDurationScale != 0f;
  }

  static void addTransitions(
      Transition transition, List<Transition> outList, @Nullable String logContext) {
    if (transition instanceof Transition.BaseTransitionUnitsBuilder) {
      outList.addAll(((Transition.BaseTransitionUnitsBuilder) transition).getTransitionUnits());
    } else if (transition != null) {
      outList.add(transition);
    } else {
      throw new IllegalStateException(
          "[" + logContext + "] Adding null to transition list is not allowed.");
    }
  }

  static void setOwnerKey(Transition transition, @Nullable String ownerKey) {
    if (transition instanceof Transition.TransitionUnit) {
      ((Transition.TransitionUnit) transition).setOwnerKey(ownerKey);
    } else if (transition instanceof TransitionSet) {
      ArrayList<Transition> children = ((TransitionSet) transition).getChildren();
      for (int index = children.size() - 1; index >= 0; index--) {
        setOwnerKey(children.get(index), ownerKey);
      }
    } else if (transition instanceof Transition.BaseTransitionUnitsBuilder) {
      ArrayList<Transition.TransitionUnit> units =
          ((Transition.BaseTransitionUnitsBuilder) transition).getTransitionUnits();
      for (int index = units.size() - 1; index >= 0; index--) {
        units.get(index).setOwnerKey(ownerKey);
      }
    } else {
      throw new RuntimeException("Unhandled transition type: " + transition);
    }
  }

  static boolean targetsAllLayout(@Nullable Transition transition) {
    if (transition == null) {
      return false;
    } else if (transition instanceof TransitionSet) {
      ArrayList<Transition> children = ((TransitionSet) transition).getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        if (targetsAllLayout(children.get(i))) {
          return true;
        }
      }
    } else if (transition instanceof Transition.TransitionUnit) {
      final Transition.TransitionUnit unit = (Transition.TransitionUnit) transition;
      final Transition.ComponentTargetType targetType =
          unit.getAnimationTarget().componentTarget.componentTargetType;
      if (targetType == Transition.ComponentTargetType.ALL
          || targetType == Transition.ComponentTargetType.AUTO_LAYOUT) {
        return true;
      }
    } else if (transition instanceof Transition.BaseTransitionUnitsBuilder) {
      final Transition.BaseTransitionUnitsBuilder builder =
          (Transition.BaseTransitionUnitsBuilder) transition;
      final List<Transition.TransitionUnit> units = builder.getTransitionUnits();
      for (int i = 0, size = units.size(); i < size; i++) {
        if (targetsAllLayout(units.get(i))) {
          return true;
        }
      }
    } else {
      throw new RuntimeException("Unhandled transition type: " + transition);
    }

    return false;
  }

  static @Nullable TransitionId createTransitionId(InternalNode node) {
    final @TransitionId.Type int type;
    final String reference;
    final String extraData;

    if (node.hasTransitionKey()) {
      reference = node.getTransitionKey();
      final Transition.TransitionKeyType transitionKeyType = node.getTransitionKeyType();
      if (transitionKeyType == Transition.TransitionKeyType.GLOBAL) {
        type = TransitionId.Type.GLOBAL;
        extraData = null;
      } else if (transitionKeyType == Transition.TransitionKeyType.LOCAL) {
        type = TransitionId.Type.SCOPED;
        extraData = node.getTransitionOwnerKey();
      } else {
        throw new IllegalArgumentException("Unhandled transition key type " + transitionKeyType);
      }
    } else {
      final Component component = node.getTailComponent();
      type = TransitionId.Type.AUTOGENERATED;
      reference = component != null ? component.getGlobalKey() : null;
      extraData = null;
    }

    return reference != null ? new TransitionId(type, reference, extraData) : null;
  }
}
