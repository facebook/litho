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

import static com.facebook.litho.testing.assertj.LithoViewAssert.times;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.assertj.LithoViewAssert;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.widget.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class MountStateTestItemTest {

  private static final String TEST_ID_1 = "test_id_1";
  private static final String TEST_ID_2 = "test_id_2";
  private static final String TEST_ID_3 = "test_id_3";
  private static final String MY_TEST_STRING_1 = "My test string";
  private static final String MY_TEST_STRING_2 = "My second test string";

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    ComponentsConfiguration.isEndToEndTestRun = true;
  }

  @After
  public void teardown() {
    ComponentsConfiguration.isEndToEndTestRun = false;
  }

  @Test
  public void testInnerComponentHostViewTags() {
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Column.create(c)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c))
                            .testKey(TEST_ID_1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c).testKey(TEST_ID_2))
                    .build();
              }
            });

    LithoViewAssert.assertThat(lithoView)
        .containsTestKey(TEST_ID_1)
        .containsTestKey(TEST_ID_2)
        .doesNotContainTestKey(TEST_ID_3);
  }

  @Test
  public void testMultipleIdenticalInnerComponentHostViewTags() {
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Column.create(c)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c))
                            .testKey(TEST_ID_1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c).testKey(TEST_ID_1))
                    .build();
              }
            });

    LithoViewAssert.assertThat(lithoView)
        .containsTestKey(TEST_ID_1, times(2))
        .doesNotContainTestKey(TEST_ID_2);
  }

  @Test
  public void testSkipInvalidTestKeys() {
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Column.create(c)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c))
                            .testKey(""))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c).testKey(null))
                    .child(TestDrawableComponent.create(c).testKey(TEST_ID_1))
                    .build();
              }
            });

    LithoViewAssert.assertThat(lithoView)
        .doesNotContainTestKey("")
        .doesNotContainTestKey(null)
        .containsTestKey(TEST_ID_1);
  }

  @Test
  public void testTextItemTextContent() {
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Text.create(c).text(MY_TEST_STRING_1).testKey(TEST_ID_1))
                    .build();
              }
            });

    LithoViewAssert.assertThat(lithoView).containsTestKey(TEST_ID_1);
    final TestItem item1 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_1);
    assertThat(item1.getTextContent()).isEqualTo(MY_TEST_STRING_1);
  }

  @Test
  public void testMultipleTextItemsTextContents() {
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Text.create(c).text(MY_TEST_STRING_1).testKey(TEST_ID_1))
                    .child(Text.create(c).text(MY_TEST_STRING_2).testKey(TEST_ID_2))
                    .build();
              }
            });

    LithoViewAssert.assertThat(lithoView).containsTestKey(TEST_ID_1);
    final TestItem item1 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_1);
    assertThat(item1.getTextContent()).isEqualTo(MY_TEST_STRING_1);
    final TestItem item2 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_2);
    assertThat(item2.getTextContent()).isEqualTo(MY_TEST_STRING_2);
  }

  @Test
  public void testTextItemsWithClickHandler() {
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Text.create(c)
                            .text(MY_TEST_STRING_1)
                            .clickHandler(mock(EventHandler.class))
                            .testKey(TEST_ID_1))
                    .child(Text.create(c).text(MY_TEST_STRING_2).testKey(TEST_ID_2))
                    .build();
              }
            });

    LithoViewAssert.assertThat(lithoView).containsTestKey(TEST_ID_1);
    final TestItem item1 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_1);
    assertThat(item1.getTextContent()).isEqualTo(MY_TEST_STRING_1);
    final TestItem item2 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_2);
    assertThat(item2.getTextContent()).isEqualTo(MY_TEST_STRING_2);
  }
}
