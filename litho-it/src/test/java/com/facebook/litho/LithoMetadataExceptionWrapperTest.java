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

package com.facebook.litho;

import static com.facebook.litho.TouchExpansionDelegateTest.emulateClickEvent;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import android.view.View;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout;
import com.facebook.litho.testing.error.TestHasDelegateThatCrashesOnCreateLayout;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.OnClickCallbackComponent;
import com.facebook.litho.widget.OnErrorNotPresentChild;
import com.facebook.litho.widget.OnErrorPassUpChildTester;
import com.facebook.litho.widget.OnErrorPassUpParentTester;
import com.facebook.litho.widget.TriggerCallbackComponent;
import com.facebook.litho.widget.TriggerCallbackComponentSpec;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/** Tests to make sure we wrap exceptions from common apis with the right metadata. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class LithoMetadataExceptionWrapperTest {

  @Rule public LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();
  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void onCreateLayout_deepComponentStack_exceptionShowsComponentStack() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage(
        new BaseMatcher<String>() {
          @Override
          public boolean matches(Object item) {
            if (item instanceof String) {
              String string = (String) item;
              return string.contains("Wrapper(Row)")
                  && string.contains("Row")
                  && string.contains("Column")
                  && string.contains("TestHasDelegateThatCrashesOnCreateLayout")
                  && string.contains("TestCrasherOnCreateLayout");
            }
            return false;
          }

          @Override
          public void describeTo(Description description) {}
        });

    final ComponentContext c = mLegacyLithoViewRule.getContext();

    mLegacyLithoViewRule
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

    final ComponentContext c = mLegacyLithoViewRule.getContext();

    mLegacyLithoViewRule
        .setRoot(TestCrasherOnCreateLayout.create(c))
        .measure()
        .layout()
        .attachToWindow();
  }

  @Test
  public void onCreateLayout_withReRaisedErrorFromErrorBoundary_showsRightComponentStack() {
    assumeThat(
        "Error boundary is enabled in debug builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));

    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage(
        new BaseMatcher<String>() {
          @Override
          public boolean matches(Object item) {
            if (item instanceof String) {
              String string = (String) item;
              return string.contains("OnErrorPassUpParentTester")
                  && string.contains("OnErrorPassUpChildTester")
                  && string.contains("ThrowExceptionGrandChildTester");
            }
            return false;
          }

          @Override
          public void describeTo(Description description) {}
        });

    final List<String> info = new ArrayList<>();
    final ComponentContext context = mLegacyLithoViewRule.getContext();
    final Component component =
        OnErrorPassUpParentTester.create(context)
            .child(OnErrorPassUpChildTester.create(context).info(info).build())
            .info(info)
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mLegacyLithoViewRule.attachToWindow().measure().layout();
  }

  @Test
  public void onCreateLayout_withIndirectReRaisedErrorFromErrorBoundary_showsRightComponentStack() {
    assumeThat(
        "Error boundary is enabled in debug builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));

    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage(
        new BaseMatcher<String>() {
          @Override
          public boolean matches(Object item) {
            if (item instanceof String) {
              String string = (String) item;
              return string.contains("OnErrorPassUpParentTester")
                  && string.contains("OnErrorNotPresentChild")
                  && string.contains("ThrowExceptionGrandChildTester");
            }
            return false;
          }

          @Override
          public void describeTo(Description description) {}
        });

    final List<String> info = new ArrayList<>();
    final ComponentContext context = mLegacyLithoViewRule.getContext();
    final Component component =
        OnErrorPassUpParentTester.create(context)
            .child(OnErrorNotPresentChild.create(context).build())
            .info(info)
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();
  }

  @Test
  public void onCreateLayout_withLogTag_showsLogTagInStack() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("log_tag: myLogTag");

    final ComponentContext c =
        new ComponentContext(ApplicationProvider.getApplicationContext(), "myLogTag", null);

    mLegacyLithoViewRule
        .useComponentTree(ComponentTree.create(c).build())
        .setRoot(TestCrasherOnCreateLayout.create(c))
        .measure()
        .layout()
        .attachToWindow();
  }

  @Test
  public void onMount_withLogTag_showsLogTagInStack() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("log_tag: myLogTag");

    final ComponentContext c =
        new ComponentContext(ApplicationProvider.getApplicationContext(), "myLogTag", null);
    mLegacyLithoViewRule
        .useComponentTree(ComponentTree.create(c).build())
        .setSizePx(100, 100)
        .setRoot(TestCrasherOnCreateLayout.create(c))
        .measure()
        .layout()
        .attachToWindow();
  }

  @Test
  public void onClickEvent_withLogTag_showsLogTagInStack() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("log_tag: myLogTag");
    mExpectedException.expectMessage(
        "<cls>com.facebook.litho.widget.OnClickCallbackComponent</cls>");

    final ComponentContext c =
        new ComponentContext(ApplicationProvider.getApplicationContext(), "myLogTag", null);
    final Component component =
        Column.create(c)
            .child(
                OnClickCallbackComponent.create(c)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(
                        new View.OnClickListener() {
                          @Override
                          public void onClick(View v) {
                            throw new RuntimeException("Expected test exception");
                          }
                        }))
            .build();

    mLegacyLithoViewRule
        .useComponentTree(ComponentTree.create(c).build())
        .setRoot(component)
        .attachToWindow()
        .measure()
        .layout();

    emulateClickEvent(mLegacyLithoViewRule.getLithoView(), 7, 7);
  }

  @Test
  public void onTrigger_withLogTag_showsLogTagInStack() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("log_tag: myLogTag");
    mExpectedException.expectMessage(
        "<cls>com.facebook.litho.widget.TriggerCallbackComponent</cls>");

    final ComponentContext c =
        new ComponentContext(ApplicationProvider.getApplicationContext(), "myLogTag", null);
    final Handle handle = new Handle();
    final Component component =
        Column.create(c)
            .child(
                TriggerCallbackComponent.create(c)
                    .widthPx(10)
                    .heightPx(10)
                    .handle(handle)
                    .callback(
                        new TriggerCallbackComponentSpec.TriggerListener() {
                          @Override
                          public void onTriggerCalled() {
                            throw new RuntimeException("Expected test exception");
                          }
                        }))
            .build();

    mLegacyLithoViewRule
        .useComponentTree(ComponentTree.create(c).build())
        .setRoot(component)
        .attachToWindow()
        .measure()
        .layout();

    TriggerCallbackComponent.doTrigger(
        mLegacyLithoViewRule.getComponentTree().getContext(), handle);
  }
}
