/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.samples.litho.lithography;

import static com.facebook.litho.testing.SubComponent.legacySubComponent;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.SubComponent;
import com.facebook.litho.testing.assertj.ComponentAssert;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class FeedItemComponentSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component<FeedItemComponent> mComponent;

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
    final ComponentContext c = mComponentsRule.getContext();

    final RecyclerBinder imageRecyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.HORIZONTAL, false))
            .build(c);
    mComponent =
        FeedItemComponent.create(c)
            .artist(new Artist("Sindre Sorhus", "Rockstar Developer", 2010))
            .binder(imageRecyclerBinder)
            .build();
  }

  @Test
  public void testRecursiveSubComponentExists() {
    final ComponentContext c = mComponentsRule.getContext();

    ComponentAssert.assertThat(c, mComponent).extractingSubComponents(c).hasSize(2);
  }

  @Test
  public void testSubComponentLegacyBridge() {
    final ComponentContext c = mComponentsRule.getContext();

    ComponentAssert.assertThat(c, mComponent)
        .has(
            subComponentWith(
                c,
                legacySubComponent(
                    SubComponent.of(
                        FooterComponent.create(c).text("Rockstar Developer").build()))));
  }
}
