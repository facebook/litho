/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional gr
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 */

package com.facebook.litho.testing.assertj;

import static com.facebook.litho.testing.assertj.ComponentConditions.typeIs;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class SubComponentDeepExtractorTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component mComponent;

  @Before
  public void setUp() {
    mComponent =
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Card.create(c).content(Text.create(c).text("test")))
                .build();
          }
        };
  }

  @Test
  public void testDeep() {
    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent)
        // We don't have a shallow Text component ...
        .doesNotHave(
            SubComponentExtractor.subComponentWith(
                mComponentsRule.getContext(), typeIs(Text.class)))
        // ... but we do have one deep down.
        .has(
            SubComponentDeepExtractor.deepSubComponentWith(
                mComponentsRule.getContext(), typeIs(Text.class)));
  }
}
