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

import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout;
import com.facebook.litho.testing.error.TestHasDelegateThatCrashesOnCreateLayout;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.OnErrorNotPresentChild;
import com.facebook.litho.widget.OnErrorPassUpChildTester;
import com.facebook.litho.widget.OnErrorPassUpParentTester;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests to make sure we wrap exceptions from common apis with the right metadata. */
@RunWith(LithoTestRunner.class)
public class LithoMetadataExceptionWrapperTest {

  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();
  @Rule public LithoViewRule mLithoViewRule = new LithoViewRule();
  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void onCreateLayout_deepComponentStack_exceptionShowsComponentStack() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage(
        "layout_stack: Wrapper(Row) -> Row -> Column -> TestHasDelegateThatCrashesOnCreateLayout -> TestCrasherOnCreateLayout");

    final ComponentContext c = mComponentsRule.getContext();

    mLithoViewRule
        .setRoot(
            Wrapper.create(c)
                .delegate(
                    Row.create(c)
                        .child(
                            Column.create(c)
                                .child(TestHasDelegateThatCrashesOnCreateLayout.create(c)))
                        .build()))
        .measure()
        .layout()
        .attachToWindow();
  }

  @Test
  public void onCreateLayout_onlyRootComponent_exceptionShowsComponentStack() throws Exception {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("layout_stack: TestCrasherOnCreateLayout");

    final ComponentContext c = mComponentsRule.getContext();

    mLithoViewRule.setRoot(TestCrasherOnCreateLayout.create(c)).measure().layout().attachToWindow();
  }

  @Test
  public void onCreateLayout_withReRaisedErrorFromErrorBoundary_showsRightComponentStack() {
    assumeThat(
        "Error boundary is enabled in debug builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));

    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage(
        "layout_stack: OnErrorPassUpParentTester -> OnErrorPassUpChildTester -> ThrowExceptionGrandChildTester");

    final List<String> info = new ArrayList<>();
    final ComponentContext context = mLithoViewRule.getContext();
    final Component component =
        OnErrorPassUpParentTester.create(context)
            .child(OnErrorPassUpChildTester.create(context).info(info).build())
            .info(info)
            .build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();
  }

  @Test
  public void onCreateLayout_withIndirectReRaisedErrorFromErrorBoundary_showsRightComponentStack() {
    assumeThat(
        "Error boundary is enabled in debug builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));

    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage(
        "layout_stack: OnErrorPassUpParentTester -> OnErrorNotPresentChild -> ThrowExceptionGrandChildTester");

    final List<String> info = new ArrayList<>();
    final ComponentContext context = mLithoViewRule.getContext();
    final Component component =
        OnErrorPassUpParentTester.create(context)
            .child(OnErrorNotPresentChild.create(context).build())
            .info(info)
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();
  }

  @Test
  public void onCreateLayout_withLogTag_showsLogTagInStack() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("log_tag: myLogTag");

    final ComponentContext c =
        new ComponentContext(RuntimeEnvironment.application, "myLogTag", null);

    mLithoViewRule
        .useComponentTree(ComponentTree.create(c).build())
        .setRoot(TestCrasherOnCreateLayout.create(c))
        .measure()
        .layout()
        .attachToWindow();
  }
}
