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

package com.facebook.litho.processor.integration.resources;

import com.facebook.litho.BaseMatcher;
import com.facebook.litho.BaseMatcherBuilder;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsPools;
import com.facebook.litho.ResourceResolver;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import org.assertj.core.api.Condition;
import org.assertj.core.api.Java6Assertions;
import org.assertj.core.description.TextDescription;

/**
 *
 * @see com.facebook.litho.processor.integration.resources.SimpleTestSampleSpec
 */
public final class SimpleTestSample implements SimpleTestSampleSpec {
  public static Matcher matcher(ComponentContext c) {
    return new Matcher(c);
  }

  public static class Matcher extends BaseMatcher<Matcher> {
    protected ResourceResolver mResourceResolver;

    Matcher(ComponentContext c) {
      mResourceResolver = ComponentsPools.acquireResourceResolver(c);
    }

    public Condition<InspectableComponent> build() {
      final Condition<InspectableComponent> mainBuilder =
          new Condition<InspectableComponent>() {
            @Override
            public boolean matches(InspectableComponent value) {
              if (!value
                  .getComponentClass()
                  .isAssignableFrom(
                      com.facebook.litho.processor.integration.resources.SimpleLayout.class)) {
                as(
                    new TextDescription(
                        "Sub-component of type \"com.facebook.litho.processor.integration.resources.SimpleLayout\""));
                return false;
              }
              final com.facebook.litho.processor.integration.resources.SimpleLayout impl =
                  (com.facebook.litho.processor.integration.resources.SimpleLayout)
                      value.getComponent();
              release();
              return true;
            }
          };
      return Java6Assertions.allOf(mainBuilder, BaseMatcherBuilder.buildCommonMatcher(this));
    }

    @Override
    public Matcher getThis() {
      return this;
    }

    private void release() {
      ComponentsPools.release(mResourceResolver);
      mResourceResolver = null;
    }
  }
}
