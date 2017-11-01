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
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import java.util.List;
import org.assertj.core.api.Condition;
import org.assertj.core.api.iterable.Extractor;

public class SubComponentExtractor implements Extractor<Component<?>, List<InspectableComponent>> {

  private final ComponentContext mComponentContext;

  SubComponentExtractor(ComponentContext componentContext) {
    mComponentContext = componentContext;
  }

  @Override
  public List<InspectableComponent> extract(Component<?> input) {
    final LithoView lithoView = ComponentTestHelper.mountComponent(mComponentContext, input);
    return LithoViewSubComponentExtractor.subComponents().extract(lithoView);
  }

  public static SubComponentExtractor subComponents(ComponentContext c) {
    return new SubComponentExtractor(c);
  }

  public static Condition<? super Component> subComponentWith(
      final ComponentContext c, final Condition<InspectableComponent> inner) {
    return new Condition<Component>() {
      @Override
      public boolean matches(Component value) {
        as("sub component <%s> with <%s>", value, inner);
        for (InspectableComponent component : subComponents(c).extract(value)) {
          if (inner.matches(component)) {
            return true;
          }
        }

        return false;
      }
    };
  }
}
