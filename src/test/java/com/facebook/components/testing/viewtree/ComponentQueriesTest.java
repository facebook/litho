// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentView;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

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
    final ComponentView view = ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text("goodbye")
        .build());

    assertThat(ComponentQueries.hasTextMatchingPredicate(view, Predicates.equalTo("hello")))
        .isFalse();
  }

  @Test
  public void testTextOnComponent() {
    final ComponentView view = ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text("hello")
            .build());

    assertThat(ComponentQueries.hasTextMatchingPredicate(view, Predicates.equalTo("hello")))
        .isTrue();
  }

  @Test
  public void testExtractTextFromTextComponent() {
    final ComponentView view = ComponentTestHelper.mountComponent(
        mContext,
        Text.create(mContext)
            .text("hello")
            .build());

    assertThat(view.getTextContent().getTextItems())
        .isEqualTo(ImmutableList.<CharSequence>of("hello"));
  }
}
