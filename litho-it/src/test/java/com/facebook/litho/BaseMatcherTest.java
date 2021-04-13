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

package com.facebook.litho;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.subcomponents.InspectableComponent;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.assertj.core.api.Condition;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(LithoTestRunner.class)
public class BaseMatcherTest {
  @Mock InspectableComponent mInspectableComponent;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mInspectableComponent.getComponent())
        .thenReturn(Row.create(new ComponentContext(getApplicationContext())).wrapInView().build());
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
