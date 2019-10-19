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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import java.util.List;
import org.assertj.core.api.Condition;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.description.TextDescription;

/**
 * An extractor to be used with {@link org.assertj.core.api.Assertions#assertThat}.
 *
 * <p>Recursively extracts sub components from a Component, wrapping them in an {@link
 * InspectableComponent}.
 *
 * <p>Components are extracted in a depth-first way so that they match the hierarchy indices when
 * going from top to bottom.
 *
 * <p>In most cases you will want to use the {@link #deepSubComponentWith(ComponentContext,
 * Condition)} combinator.
 */
public final class SubComponentDeepExtractor
    implements Extractor<Component, List<InspectableComponent>> {

  private final ComponentContext mComponentContext;

  private SubComponentDeepExtractor(ComponentContext componentContext) {
    mComponentContext = componentContext;
  }

  @Override
  public List<InspectableComponent> extract(Component input) {
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

  /**
   * Verify that a component tree contains a component that matches the provided condition at any
   * level in its tree.
   *
   * <p><em>Note that asserting on indirect children breaks encapsulation and can lead to Change
   * Detector Tests[0]. Prefer {@link SubComponentExtractor#subComponentWith(ComponentContext,
   * Condition)} over this whenever possible.</em>
   *
   * <p>
   *
   * <h2>Example Use</h2>
   *
   * Suppose you have a <code>MyWrapperComponentSpec</code> that creates a {@link
   * com.facebook.litho.widget.Card} which contains a {@link com.facebook.litho.widget.Text} for a
   * <code>text</code> prop that your component accepts.
   *
   * <p>You want to ensure that the text truncation logic correctly applies to the text that sits
   * inside the card. Using {@link SubComponentExtractor#subComponentWith(ComponentContext,
   * Condition)} won't work here as it will only be able to make assertions on the {@link
   * com.facebook.litho.widget.Card} but not the {@link com.facebook.litho.widget.Text} sitting
   * inside.
   *
   * <p>
   *
   * <pre><code>
   * mComponent = MyWrapperComponent.create(c).text("I'm Mr. Meeseeks! Look at me!").build();
   * assertThat(c, mComponent)
   *   .has(deepSubComponentWith(c, textEquals("I'm Mr. Meeseeks!")))
   *   .doesNotHave(deepSubComponentWith(c, text(containsString("wubba lubba dub dub"))));
   * </code></pre>
   *
   * For more applicable combinators, see below:
   *
   * @see org.hamcrest.CoreMatchers
   * @see ComponentConditions
   * @param c The ComponentContext used to create the tree.
   * @param inner The condition that at least one sub component needs to match.
   *     <p>[0] https://testing.googleblog.com/2015/01/testing-on-toilet-change-detector-tests.html
   */
  public static Condition<? super Component> deepSubComponentWith(
      final ComponentContext c, final Condition<InspectableComponent> inner) {
    return new Condition<Component>(new TextDescription("Deep subcomponent with <%s>", inner)) {
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
