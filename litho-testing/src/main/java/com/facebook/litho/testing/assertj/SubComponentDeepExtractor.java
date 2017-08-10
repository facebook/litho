/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.assertj;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.InspectableComponent;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import org.assertj.core.api.iterable.Extractor;

/**
 * Recursively extracts sub components from a Component, wrapping them in an {@link
 * InspectableComponent}.
 *
 * <p>Components are extracted in a depth-first way so that they match the hierarchy indices when
 * going from top to bottom.
 */
public final class SubComponentDeepExtractor
    implements Extractor<Component<?>, List<InspectableComponent>> {

  private final ComponentContext mComponentContext;

  private SubComponentDeepExtractor(ComponentContext componentContext) {
    mComponentContext = componentContext;
  }

  @Override
  public List<InspectableComponent> extract(Component<?> input) {
    final LithoView lithoView = ComponentTestHelper.mountComponent(mComponentContext, input);

    final List<InspectableComponent> res = new LinkedList<>();
    final Stack<InspectableComponent> stack = new Stack<>();

    stack.add(InspectableComponent.getRootInstance(lithoView));

    while (!stack.isEmpty()) {
      final InspectableComponent inspectableComponent = stack.pop();
      res.add(inspectableComponent);

      for (InspectableComponent child : inspectableComponent.getChildComponents()) {
        stack.push(child);
      }
    }

    return res;
  }

  public static SubComponentDeepExtractor subComponentsDeeply(ComponentContext c) {
    return new SubComponentDeepExtractor(c);
  }
}
