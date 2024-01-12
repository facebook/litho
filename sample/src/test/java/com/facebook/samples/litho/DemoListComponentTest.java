/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.samples.litho;

import static com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.numOfSubComponents;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.subcomponents.SubComponent;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class DemoListComponentTest {
  @Rule public LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();
  private Component mComponent;

  @Before
  public void assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Before
  public void setUp() {
    mComponent =
        DemoListRootComponent.create(mLegacyLithoViewRule.getContext())
            .demos(new ArrayList<Demos.DemoGrouping>())
            .previousIndices(null)
            .build();
  }

  @Test
  public void testSubComponents() {
    assertThat(mLegacyLithoViewRule.getContext(), mComponent)
        .containsOnlySubComponents(SubComponent.of(RecyclerCollectionComponent.class));
  }

  @Test
  public void testNumOfSubComponents() {
    assertThat(mLegacyLithoViewRule.getContext(), mComponent)
        .has(numOfSubComponents(mLegacyLithoViewRule.getContext(), is(1)));

    assertThat(mLegacyLithoViewRule.getContext(), mComponent)
        .has(numOfSubComponents(mLegacyLithoViewRule.getContext(), greaterThan(0)));
  }
}
