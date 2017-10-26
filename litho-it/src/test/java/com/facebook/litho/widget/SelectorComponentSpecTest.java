/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static com.facebook.litho.testing.assertj.ComponentConditions.typeIs;
import static com.facebook.litho.testing.assertj.SubComponentDeepExtractor.deepSubComponentWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.assertj.ComponentAssert;
import com.facebook.litho.testing.assertj.ComponentLayoutAssert;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link SelectorComponentSpec}. */
@RunWith(ComponentsTestRunner.class)
public class SelectorComponentSpecTest {

  private static final InlineLayoutSpec NULL_SPEC =
      new InlineLayoutSpec() {

        @Override
        protected ComponentLayout onCreateLayout(ComponentContext c) {
          return null;
        }
      };

  private ComponentContext mContext;

  @Before
  public void setUp() throws Exception {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testFirstComponentSelected() throws Exception {
    ComponentAssert.assertThat(
            SelectorComponent.create(mContext)
                .component(Image.create(mContext).drawable(null))
                .component(Text.create(mContext).text("Hello World")))
        .has(deepSubComponentWith(mContext, typeIs(Image.class)));
  }

  @Test
  public void testNullArgument() throws Exception {
    ComponentAssert.assertThat(
            SelectorComponent.create(mContext)
                .component((Component) null)
                .component(Image.create(mContext).drawable(null))
                .component(Text.create(mContext).text("Hello World")))
        .has(deepSubComponentWith(mContext, typeIs(Image.class)));
  }

  @Test
  public void testNullLayoutSkipped() throws Exception {
    ComponentAssert.assertThat(
            SelectorComponent.create(mContext)
                .component(NULL_SPEC)
                .component(Image.create(mContext).drawable(null))
                .component(Text.create(mContext).text("Hello World")))
        .has(deepSubComponentWith(mContext, typeIs(Image.class)));
  }

  @Test
  public void testEmpty() throws Exception {
    ComponentLayoutAssert.assertThat(
            SelectorComponent.create(mContext)
                .component(NULL_SPEC)
                .component((Component) null)
                .buildWithLayout())
        .willNotRender();
  }

  @Test
  public void testAllNull() throws Exception {
    ComponentLayoutAssert.assertThat(
            SelectorComponent.create(mContext)
                .component((Component) null)
                .component((Component) null)
                .buildWithLayout())
        .willNotRender();
  }
}
