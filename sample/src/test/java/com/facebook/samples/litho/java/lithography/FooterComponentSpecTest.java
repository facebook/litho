// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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

package com.facebook.samples.litho.java.lithography;

import static com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TestText;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class FooterComponentSpecTest {
  @Rule public LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void subComponentWithMatcherWithConsistentHierarchyExperiment() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component = FooterComponent.create(c).text("I hate Mumunmununsdays").build();

    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(subComponentWith(c, TestText.matcher(c).text("I hate Mumunmununsdays").build()));
  }
}
