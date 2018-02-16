/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.subcomponents.InspectableComponent;
import org.assertj.core.api.Condition;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseMatcherTest {
  @Mock InspectableComponent mInspectableComponent;
  @Mock Component mComponent;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mInspectableComponent.getComponent()).thenReturn(mComponent);
    when(mComponent.getCommonProps()).thenReturn(mock(CommonProps.class));
  }

  @Test
  public void testMatcherCreation() {
    final TestBaseMatcher matcher =
        new TestBaseMatcher().clickHandler(IsNull.<EventHandler<ClickEvent>>nullValue(null));
    final Condition<InspectableComponent> condition =
        BaseMatcherBuilder.buildCommonMatcher(matcher);

    assertThat(condition.matches(mInspectableComponent)).isTrue();
  }

  @Test
  public void testMatcherFailureMessage() {
    final TestBaseMatcher matcher =
        new TestBaseMatcher().clickHandler(IsNull.<EventHandler<ClickEvent>>notNullValue(null));
    final Condition<InspectableComponent> condition =
        BaseMatcherBuilder.buildCommonMatcher(matcher);

    condition.matches(mInspectableComponent);
    assertThat(condition.description().toString())
        .isEqualTo("Click handler <not null> (doesn't match <null>)");
  }

  static class TestBaseMatcher extends BaseMatcher<TestBaseMatcher> {
    @Override
    protected TestBaseMatcher getThis() {
      return this;
    }
  }
}
