/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.assertj;

import com.facebook.litho.LithoView;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import org.assertj.core.api.Condition;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.util.Preconditions;

/**
 * Recursively extracts sub components from a LithoView holding one or more Components, wrapping
 * them in an {@link InspectableComponent}.
 *
 * <p>Components are extracted in a depth-first way so that they match the hierarchy indices when
 * going from top to bottom.
 */
public final class LithoViewSubComponentDeepExtractor
    implements Extractor<LithoView, List<InspectableComponent>> {

  private LithoViewSubComponentDeepExtractor() {}

  @Override
  public List<InspectableComponent> extract(LithoView lithoView) {
    final List<InspectableComponent> res = new LinkedList<>();
    final Stack<InspectableComponent> stack = new Stack<>();

    final InspectableComponent rootInstance = InspectableComponent.getRootInstance(lithoView);
    Preconditions.checkNotNull(
        rootInstance,
        "Could not obtain DebugComponent. "
            + "Please ensure that ComponentsConfiguration.IS_INTERNAL_BUILD is enabled.");
    stack.add(rootInstance);

    while (!stack.isEmpty()) {
      final InspectableComponent inspectableComponent = stack.pop();
      res.add(inspectableComponent);

      for (InspectableComponent child : inspectableComponent.getChildComponents()) {
        stack.push(child);
      }
    }

    return res;
  }

  public static LithoViewSubComponentDeepExtractor subComponentsDeeply() {
    return new LithoViewSubComponentDeepExtractor();
  }

  public static Condition<LithoView> deepSubComponentWith(
      final Condition<InspectableComponent> inner) {
    return new Condition<LithoView>() {
      @Override
      public boolean matches(LithoView lithoView) {
        as("deep sub component with <%s>", inner);
        for (InspectableComponent component : subComponentsDeeply().extract(lithoView)) {
          if (inner.matches(component)) {
            return true;
          }
        }

        return false;
      }
    };
  }
}
