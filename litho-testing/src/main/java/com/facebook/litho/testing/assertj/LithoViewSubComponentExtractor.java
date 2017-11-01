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
import java.util.List;
import org.assertj.core.api.Condition;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.util.Preconditions;

public class LithoViewSubComponentExtractor
    implements Extractor<LithoView, List<InspectableComponent>> {

  private LithoViewSubComponentExtractor() {}

  @Override
  public List<InspectableComponent> extract(LithoView lithoView) {
    final InspectableComponent component = InspectableComponent.getRootInstance(lithoView);

    Preconditions.checkNotNull(
        component,
        "Could not obtain DebugComponent. "
            + "Please ensure that ComponentsConfiguration.IS_INTERNAL_BUILD is enabled.");

    return component.getChildComponents();
  }

  public static LithoViewSubComponentExtractor subComponents() {
    return new LithoViewSubComponentExtractor();
  }

  public static Condition<? super LithoView> subComponentWith(
      final Condition<InspectableComponent> inner) {
    return new Condition<LithoView>() {
      @Override
      public boolean matches(LithoView value) {
        as("sub component with <%s>", inner);
        for (InspectableComponent component : subComponents().extract(value)) {
          if (inner.matches(component)) {
            return true;
          }
        }

        return false;
      }
    };
  }
}
