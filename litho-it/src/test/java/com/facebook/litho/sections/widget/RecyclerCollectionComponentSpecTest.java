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

package com.facebook.litho.sections.widget;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.LifecycleStep.getSteps;
import static com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.LoadingState.EMPTY;
import static com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.LoadingState.ERROR;
import static com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.LoadingState.LOADED;
import static com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.LoadingState.LOADING;
import static com.facebook.litho.testing.assertj.ComponentConditions.textEquals;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.LithoViewSubComponentDeepExtractor.deepSubComponentWith;
import static com.facebook.litho.widget.SnapUtil.SNAP_NONE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.condition.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.LithoView;
import com.facebook.litho.Row;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.state.StateUpdatesTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.testing.viewtree.ViewTree;
import com.facebook.litho.testing.viewtree.ViewTreeAssert;
import com.facebook.litho.widget.LayoutSpecWorkingRangeTester;
import com.facebook.litho.widget.MountSpecWorkingRangeTester;
import com.facebook.litho.widget.Text;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/** Tests {@link RecyclerCollectionComponentSpec} */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class RecyclerCollectionComponentSpecTest {

  @Rule public final ComponentsRule componentsRule = new ComponentsRule();
  @Rule public final LithoViewRule mLithoViewRule = new LithoViewRule();

  private ComponentContext mComponentContext;
  private Component mLoadingComponent;
  private Component mEmptyComponent;
  private Component mErrorComponent;
  private Component mRecyclerCollectionComponent;
  private Component mContentComponent;

  @Before
  public void assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Before
  public void setup() throws Exception {
    mComponentContext = new ComponentContext(getApplicationContext());

    mLoadingComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Text.create(c).text("loading").heightPx(100).widthPx(100).build();
          }
        };

    mEmptyComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Text.create(c).text("empty").heightPx(100).widthPx(100).build();
          }
        };
    mErrorComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Text.create(c).text("error").heightPx(100).widthPx(100).build();
          }
        };
    mContentComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Text.create(c).text("content").heightPx(100).widthPx(100).build();
          }
        };

    mRecyclerCollectionComponent =
        RecyclerCollectionComponent.create(mComponentContext)
            .emptyComponent(mEmptyComponent)
            .loadingComponent(mLoadingComponent)
            .errorComponent(mErrorComponent)
            .recyclerConfiguration(
                new ListRecyclerConfiguration(LinearLayoutManager.VERTICAL, false, SNAP_NONE, null))
            .section(
                SingleComponentSection.create(new SectionContext(mComponentContext))
                    .component(mContentComponent)
                    .build())
            .build();
  }

  @Test
  public void testNothingShown() throws Exception {
    mRecyclerCollectionComponent =
        RecyclerCollectionComponent.create(mComponentContext)
            .loadingComponent(mLoadingComponent)
            .errorComponent(mErrorComponent)
            .recyclerConfiguration(new ListRecyclerConfiguration())
            .section(
                SingleComponentSection.create(new SectionContext(mComponentContext))
                    .component(Text.create(mComponentContext).text("content").build())
                    .build())
            .build();

    assertThat(mComponentContext, mRecyclerCollectionComponent)
        .withStateUpdate(
            new StateUpdatesTestHelper.StateUpdater() {
              @Override
              public void performStateUpdate(ComponentContext c) {
                RecyclerCollectionComponent.updateLoadingState(c, EMPTY);
              }
            })
        .doesNotHave(
            deepSubComponentWith(
                anyOf(
                    textEquals("loading"),
                    textEquals("content"),
                    textEquals("empty"),
                    textEquals("error"))));
  }

  @Test
  public void testEmpty() throws Exception {
    LithoView view =
        StateUpdatesTestHelper.getViewAfterStateUpdate(
            mComponentContext,
            mRecyclerCollectionComponent,
            new StateUpdatesTestHelper.StateUpdater() {
              @Override
              public void performStateUpdate(ComponentContext context) {
                RecyclerCollectionComponent.updateLoadingState(context, EMPTY);
              }
            });

    ViewTreeAssert.assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .hasVisibleText("content")
        .hasVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testError() throws Exception {
    LithoView view =
        StateUpdatesTestHelper.getViewAfterStateUpdate(
            mComponentContext,
            mRecyclerCollectionComponent,
            new StateUpdatesTestHelper.StateUpdater() {
              @Override
              public void performStateUpdate(ComponentContext context) {
                RecyclerCollectionComponent.updateLoadingState(context, ERROR);
              }
            });

    ViewTreeAssert.assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .hasVisibleText("error");
  }

  @Test
  public void testLoaded() throws Exception {
    LithoView view =
        StateUpdatesTestHelper.getViewAfterStateUpdate(
            mComponentContext,
            mRecyclerCollectionComponent,
            new StateUpdatesTestHelper.StateUpdater() {
              @Override
              public void performStateUpdate(ComponentContext context) {
                RecyclerCollectionComponent.updateLoadingState(context, LOADED);
              }
            });

    ViewTreeAssert.assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testLoading() throws Exception {
    LithoView view =
        StateUpdatesTestHelper.getViewAfterStateUpdate(
            mComponentContext,
            mRecyclerCollectionComponent,
            new StateUpdatesTestHelper.StateUpdater() {
              @Override
              public void performStateUpdate(ComponentContext context) {
                RecyclerCollectionComponent.updateLoadingState(context, LOADING);
              }
            });

    ViewTreeAssert.assertThat(ViewTree.of(view))
        .hasVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testInitialState() throws Exception {
    LithoView view =
        ComponentTestHelper.mountComponent(mComponentContext, mRecyclerCollectionComponent);

    ViewTreeAssert.assertThat(ViewTree.of(view))
        .hasVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testNestedIncrementalMountDisabled() {
    LithoView view =
        ComponentTestHelper.mountComponent(
            mComponentContext,
            RecyclerCollectionComponent.create(mComponentContext)
                .section(
                    SingleComponentSection.create(new SectionContext(mComponentContext))
                        .component(
                            Row.create(mComponentContext)
                                .viewTag("rv_row")
                                .heightDip(100)
                                .widthDip(100))
                        .build())
                .build(),
            false,
            false);

    final LithoView childView = (LithoView) findViewWithTag(view, "rv_row");
    assertThat(childView).isNotNull();
    assertThat(childView.getComponentTree().isIncrementalMountEnabled()).isFalse();
  }

  @Test
  public void testNestedIncrementalMountNormal() {
    LithoView view =
        ComponentTestHelper.mountComponent(
            mComponentContext,
            RecyclerCollectionComponent.create(mComponentContext)
                .section(
                    SingleComponentSection.create(new SectionContext(mComponentContext))
                        .component(
                            Row.create(mComponentContext)
                                .viewTag("rv_row")
                                .heightDip(100)
                                .widthDip(100))
                        .build())
                .build(),
            true,
            true);

    final LithoView childView = (LithoView) findViewWithTag(view, "rv_row");
    assertThat(childView).isNotNull();
    assertThat(childView.getComponentTree().isIncrementalMountEnabled()).isTrue();
  }

  @Test
  public void rcc_insertLayoutSpecWorkingRangeTester_workingRangeIsRegisteredAndEntered() {
    final ComponentContext componentContext = mLithoViewRule.getContext();
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        LayoutSpecWorkingRangeTester.create(componentContext).steps(info).heightPx(100).build();
    final RecyclerCollectionComponent rcc =
        RecyclerCollectionComponent.create(componentContext)
            .recyclerConfiguration(ListRecyclerConfiguration.create().build())
            .section(
                SingleComponentSection.create(new SectionContext(componentContext))
                    .component(component)
                    .build())
            .build();
    mLithoViewRule
        .setRoot(rcc)
        .setSizeSpecs(makeMeasureSpec(100, EXACTLY), makeMeasureSpec(100, EXACTLY));

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should register and enter working range in expected order")
        .containsExactly(LifecycleStep.ON_REGISTER_RANGES, LifecycleStep.ON_ENTERED_RANGE);
  }

  @Test
  public void rcc_insertMountSpecWorkingRangeTester_workingRangeIsRegisteredAndEntered() {
    final ComponentContext componentContext = mLithoViewRule.getContext();
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecWorkingRangeTester.create(componentContext).steps(info).heightPx(100).build();
    final RecyclerCollectionComponent rcc =
        RecyclerCollectionComponent.create(componentContext)
            .recyclerConfiguration(ListRecyclerConfiguration.create().build())
            .section(
                SingleComponentSection.create(new SectionContext(componentContext))
                    .component(component)
                    .build())
            .build();
    mLithoViewRule
        .setRoot(rcc)
        .setSizeSpecs(makeMeasureSpec(100, EXACTLY), makeMeasureSpec(100, EXACTLY));

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should register and enter working range in expected order")
        .containsExactly(LifecycleStep.ON_REGISTER_RANGES, LifecycleStep.ON_ENTERED_RANGE);
  }

  @Nullable
  private static View findViewWithTag(@Nullable View root, @Nullable String tag) {
    if (root == null || TextUtils.isEmpty(tag)) {
      return null;
    }

    if (tag.equals(root.getTag())) {
      return root;
    }

    if (root instanceof ViewGroup) {
      ViewGroup vg = (ViewGroup) root;
      for (int i = 0; i < vg.getChildCount(); i++) {
        View v = findViewWithTag(vg.getChildAt(i), tag);
        if (v != null) {
          return v;
        }
      }
    }

    return null;
  }
}
