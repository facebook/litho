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
