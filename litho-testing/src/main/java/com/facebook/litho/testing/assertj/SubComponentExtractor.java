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

package com.facebook.litho.testing.assertj;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import java.util.List;
import org.assertj.core.api.Condition;
import org.assertj.core.api.iterable.Extractor;
import org.hamcrest.Matcher;

/**
 * An extractor to be used with {@link org.assertj.core.api.Assertions#assertThat}.
 *
 * <p>In most cases you will want to use the {@link #subComponentWith(ComponentContext, Condition)}
 * combinator.
 */
public class SubComponentExtractor implements Extractor<Component, List<InspectableComponent>> {

  private final ComponentContext mComponentContext;

  SubComponentExtractor(ComponentContext componentContext) {
    mComponentContext = componentContext;
  }

  @Override
  public List<InspectableComponent> extract(Component input) {
    final LithoView lithoView = ComponentTestHelper.mountComponent(mComponentContext, input);
    return LithoViewSubComponentExtractor.subComponents().extract(lithoView);
  }

  public static SubComponentExtractor subComponents(ComponentContext c) {
    return new SubComponentExtractor(c);
  }

  /**
   * This combinator allows you to make an assertion that applies to at least one sub-component
   * directly spun up by the component under test.
   *
   * <p><em>Note that this combinator only works on direct child-components, i.e. sub-components not
   * further than one level deep.</em>
   *
   * <p>If you want to make assertions over deeply nested sub-components, check out {@link
   * SubComponentDeepExtractor#deepSubComponentWith(ComponentContext, Condition)}.
   *
   * <p>
   *
   * <h2>Example Use</h2>
   *
   * Suppose you've got a <code>MyComponentSpec</code> which takes a <code>text</code> prop and
   * creates multiple children; one of them a {@link com.facebook.litho.widget.Text} with a
   * truncated version of your text. You want to verify that there is a direct child present that
   * contains the <code>text</code> that you have passed in:
   *
   * <p>
   *
   * <pre><code>
   * mComponent = MyComponent.create(c).text("Cells interlinked within cells interlinked").build();
   * assertThat(c, mComponent)
   *   .has(subComponentWith(c, textEquals("Cells interlink...")))
   *   .doesNotHave(subComponentWith(c, text(containsString("within cells"))));
   * </code></pre>
   *
   * For more applicable combinators, see below:
   *
   * @see org.hamcrest.CoreMatchers
   * @see ComponentConditions
   * @param c The ComponentContext used to create the tree.
   * @param inner The condition that at least one sub component needs to match.
   */
  public static Condition<? super Component> subComponentWith(
      final ComponentContext c, final Condition<InspectableComponent> inner) {
    return new Condition<Component>() {
      @Override
      public boolean matches(Component value) {
        as("sub component with <%s>", inner);
        for (InspectableComponent component : subComponents(c).extract(value)) {
          if (inner.matches(component)) {
            return true;
          }
        }

        return false;
      }
    };
  }

  /**
   * This combinator allows you to make an assertion on the number of sub-components directly spun
   * up by the component under test.
   *
   * @param c The ComponentContext used to create the tree.
   * @param matcher The Matcher that verifies the number of sub-components against a condition
   */
  public static Condition<? super Component> numOfSubComponents(
      final ComponentContext c, final Matcher<Integer> matcher) {
    return new Condition<Component>() {
      @Override
      public boolean matches(Component component) {
        final int num = subComponents(c).extract(component).size();
        as("number of sub components %s but was %d", matcher.toString(), num);
        return matcher.matches(num);
      }
    };
  }
}
