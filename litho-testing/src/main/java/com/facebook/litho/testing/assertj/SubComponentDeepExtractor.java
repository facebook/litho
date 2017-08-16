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
import java.util.List;
import org.assertj.core.api.Condition;
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

    return LithoViewSubComponentDeepExtractor.subComponentsDeeply().extract(lithoView);
  }

  /**
   * Extract sub components recursively, from a provided Component in a depth-first manner.
   *
   * <p>E.g.
   *
   * <pre>
   * {@code assertThat(lithoView).extracting(subComponentsDeeply(c)).hasSize(2);}
   * </pre>
   */
  public static SubComponentDeepExtractor subComponentsDeeply(ComponentContext c) {
    return new SubComponentDeepExtractor(c);
  }

  public static Condition<? super Component> deepSubComponentWith(
      final ComponentContext c, final Condition<InspectableComponent> inner) {
    // TODO(T20862132): Provide better error messages.
    return new Condition<Component>() {
      @Override
      public boolean matches(Component value) {
        for (InspectableComponent component : subComponentsDeeply(c).extract(value)) {
          if (inner.matches(component)) {
            return true;
          }
        }

        return false;
      }
    };
  }
}
