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
