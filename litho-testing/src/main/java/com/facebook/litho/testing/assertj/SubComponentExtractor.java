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
import com.facebook.litho.DebugComponent;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.InspectableComponent;
import java.util.List;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.util.Preconditions;

public class SubComponentExtractor implements Extractor<Component<?>, List<InspectableComponent>> {

  private final ComponentContext mComponentContext;

  SubComponentExtractor(ComponentContext componentContext) {
    mComponentContext = componentContext;
  }

  @Override
  public List<InspectableComponent> extract(Component<?> input) {
    final LithoView lithoView = ComponentTestHelper.mountComponent(mComponentContext, input);
    final InspectableComponent component = InspectableComponent.getRootInstance(lithoView);

    // This is a scenario I'm not quite sure at the moment if and how it could happen, hence
    // the lack of advice for dealing with this.
    Preconditions.checkNotNull(
        component, "Could not obtain DebugComponent from the mounted Component.");

    return component.getChildComponents();
  }

  public static SubComponentExtractor subComponents(ComponentContext c) {
    return new SubComponentExtractor(c);
  }
}
