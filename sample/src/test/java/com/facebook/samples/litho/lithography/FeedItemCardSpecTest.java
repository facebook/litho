/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.samples.litho.lithography;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.assertj.core.data.Index.atIndex;

import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.InspectableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.Text;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class FeedItemCardSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component<FeedItemCard> mComponent;

  @Before
  public void setUp() {
    final ComponentContext c = mComponentsRule.getContext();
    final RecyclerBinder binder =
        new RecyclerBinder.Builder()
            .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.HORIZONTAL, false))
            .build(c);

    mComponent =
        FeedItemCard.create(c)
            .binder(binder)
            .artist(new Artist("Sindre Sorhus", "JavaScript Rockstar", 2010))
            .build();
  }

  @Test
  public void testShallowSubComponents() {
    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent)
        .extractingSubComponents(c)
        .hasSize(1)
        .has(
            new Condition<InspectableComponent>() {
              @Override
              public boolean matches(InspectableComponent value) {
                return value.getComponentClass() == Card.class;
              }
            },
            atIndex(0));
  }

  @Test
  public void testDeepSubComponents() {
    final ComponentContext c = mComponentsRule.getContext();

    // N.B. This manual way of testing is not recommended and will be replaced by more high-level
    // matchers, but illustrates how it can be used in case more fine-grained assertions are
    // required.
    assertThat(c, mComponent)
        .extractingSubComponentsDeeply(c)
        .hasSize(14)
        .has(
            new Condition<InspectableComponent>() {
              @Override
              public boolean matches(InspectableComponent value) {
                describedAs(value.getComponentClass() + " with text " + value.getTextContent());
                return value.getComponentClass() == Text.class
                    && "JavaScript Rockstar".equals(value.getTextContent());
              }
            },
            atIndex(7));
  }
}
