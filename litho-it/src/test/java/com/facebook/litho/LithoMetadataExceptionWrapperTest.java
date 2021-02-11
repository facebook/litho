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

import static com.facebook.litho.TouchExpansionDelegateTest.emulateClickEvent;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import android.view.View;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout;
import com.facebook.litho.testing.error.TestHasDelegateThatCrashesOnCreateLayout;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.DebugMetadataTestComponent;
import com.facebook.litho.widget.OnClickCallbackComponent;
import com.facebook.litho.widget.OnErrorNotPresentChild;
import com.facebook.litho.widget.OnErrorPassUpChildTester;
import com.facebook.litho.widget.OnErrorPassUpParentTester;
import com.facebook.litho.widget.OnMeasureCallbackComponent;
import com.facebook.litho.widget.OnMeasureCallbackComponentSpec;
import com.facebook.litho.widget.TriggerCallbackComponent;
import com.facebook.litho.widget.TriggerCallbackComponentSpec;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/** Tests to make sure we wrap exceptions from common apis with the right metadata. */
@LooperMode(LooperMode.Mode.LEGACY)
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
        new ComponentContext(ApplicationProvider.getApplicationContext(), "myLogTag", null);

    mLithoViewRule
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
    mLithoViewRule
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
    mExpectedException.expectMessage("component_scope: OnClickCallbackComponent");

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

    mLithoViewRule
        .useComponentTree(ComponentTree.create(c).build())
        .setRoot(component)
        .attachToWindow()
        .measure()
        .layout();

    emulateClickEvent(mLithoViewRule.getLithoView(), 7, 7);
  }

  @Test
  public void onTrigger_withLogTag_showsLogTagInStack() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("log_tag: myLogTag");
    mExpectedException.expectMessage("component_scope: TriggerCallbackComponent");

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

    mLithoViewRule
        .useComponentTree(ComponentTree.create(c).build())
        .setRoot(component)
        .attachToWindow()
        .measure()
        .layout();

    TriggerCallbackComponent.doTrigger(mLithoViewRule.getComponentTree().getContext(), handle);
  }

  @Test
  public void onCreateLayoutCrashOfChild_withDebugMetadata_showsDebugMetadata() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("custom_key: custom_value");

    final ComponentContext c = mComponentsRule.getContext();
    mLithoViewRule
        .setRoot(
            Column.create(c)
                .child(
                    DebugMetadataTestComponent.create(c)
                        .metadataKey("custom_key")
                        .metadataValue("custom_value")
                        .child(TestHasDelegateThatCrashesOnCreateLayout.create(c)))
                .build())
        .measure()
        .layout()
        .attachToWindow();
  }

  @Test
  public void
      onCreateLayoutCrashOfChild_withDebugMetadataInMultipleComponents_showsDebugMetadata() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("custom_key: custom_value");
    mExpectedException.expectMessage("custom_key2: custom_value2");

    final ComponentContext c = mComponentsRule.getContext();
    mLithoViewRule
        .setRoot(
            Column.create(c)
                .child(
                    DebugMetadataTestComponent.create(c)
                        .metadataKey("custom_key")
                        .metadataValue("custom_value")
                        .child(
                            DebugMetadataTestComponent.create(c)
                                .metadataKey("custom_key2")
                                .metadataValue("custom_value2")
                                .child(TestHasDelegateThatCrashesOnCreateLayout.create(c))))
                .build())
        .measure()
        .layout()
        .attachToWindow();
  }

  @Test
  public void onClickEvent_withDebugMetadata_showsDebugMetadata() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("custom_key: custom_value");

    final ComponentContext c = mComponentsRule.getContext();
    final Component component =
        Column.create(c)
            .child(
                DebugMetadataTestComponent.create(c)
                    .metadataKey("custom_key")
                    .metadataValue("custom_value")
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
                                })))
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    emulateClickEvent(mLithoViewRule.getLithoView(), 7, 7);
  }

  @Test
  public void onMeasureCrash_showsDebugMetadata() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("component_scope: OnMeasureCallbackComponent");

    final ComponentContext c = mComponentsRule.getContext();
    final Component component =
        Column.create(c)
            .child(
                DebugMetadataTestComponent.create(c)
                    .metadataKey("custom_key")
                    .metadataValue("custom_value")
                    .child(
                        OnMeasureCallbackComponent.create(c)
                            .widthPx(10)
                            .callback(
                                new OnMeasureCallbackComponentSpec.Callback() {
                                  @Override
                                  public void onMeasure(int widthSpec, int heightSpec) {
                                    throw new RuntimeException("Expected Exception");
                                  }
                                })))
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();
  }

  @Test
  public void onCrash_withMultipleNestedExceptions_showsDeepestException() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("Real Cause => java.lang.RuntimeException: Exception Level 3");

    final ComponentContext c = mComponentsRule.getContext();
    final Component component =
        Column.create(c)
            .child(
                DebugMetadataTestComponent.create(c)
                    .metadataKey("custom_key")
                    .metadataValue("custom_value")
                    .child(
                        OnMeasureCallbackComponent.create(c)
                            .widthPx(10)
                            .callback(
                                new OnMeasureCallbackComponentSpec.Callback() {
                                  @Override
                                  public void onMeasure(int widthSpec, int heightSpec) {
                                    throw new RuntimeException(
                                        "Exception Level 1",
                                        new RuntimeException(
                                            "Exception Level 2",
                                            new RuntimeException("Exception Level 3")));
                                  }
                                })))
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();
  }
}
