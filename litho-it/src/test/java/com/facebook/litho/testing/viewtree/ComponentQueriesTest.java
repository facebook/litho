/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.viewtree;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests {@link ComponentQueries}
 */
@RunWith(ComponentsTestRunner.class)
public class ComponentQueriesTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testNoTextOnComponent() {
    final LithoView view = ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text("goodbye")
        .build());

    assertThat(ComponentQueries.hasTextMatchingPredicate(view, Predicates.equalTo("hello")))
        .isFalse();
  }

  @Test
  public void testTextOnComponent() {
    final LithoView view = ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text("hello")
            .build());

    assertThat(ComponentQueries.hasTextMatchingPredicate(view, Predicates.equalTo("hello")))
        .isTrue();
  }

  @Test
  public void testExtractTextFromTextComponent() {
    final LithoView view = ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text("hello")
            .build());

    assertThat(view.getTextContent().getTextItems())
        .isEqualTo(ImmutableList.<CharSequence>of("hello"));
  }
}
