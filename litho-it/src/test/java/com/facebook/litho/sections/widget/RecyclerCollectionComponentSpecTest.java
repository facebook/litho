/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.widget;

import static com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.LoadingState.EMPTY;
import static com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.LoadingState.ERROR;
import static com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.LoadingState.LOADED;
import static com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.LoadingState.LOADING;
import static com.facebook.litho.testing.assertj.ComponentConditions.textEquals;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.LithoViewSubComponentDeepExtractor.deepSubComponentWith;
import static com.facebook.litho.widget.SnapUtil.SNAP_NONE;
import static org.assertj.core.condition.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import androidx.recyclerview.widget.LinearLayoutManager;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.state.StateUpdatesTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.testing.viewtree.ViewTree;
import com.facebook.litho.testing.viewtree.ViewTreeAssert;
import com.facebook.litho.widget.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests {@link RecyclerCollectionComponentSpec}
 */
@RunWith(ComponentsTestRunner.class)
public class RecyclerCollectionComponentSpecTest {

  @Rule public ComponentsRule componentsRule = new ComponentsRule();

  private ComponentContext mComponentContext;
  private Component mLoadingComponent;
  private Component mEmptyComponent;
  private Component mErrorComponent;
  private Component mRecyclerCollectionComponent;
  private Component mContentComponent;

  @Before
  public void assumeDebug() {
    assumeThat("These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD, is(true));
  }

  @Before
  public void setup() throws Exception {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);

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

    mRecyclerCollectionComponent = RecyclerCollectionComponent.create(mComponentContext)
        .emptyComponent(mEmptyComponent)
        .loadingComponent(mLoadingComponent)
        .errorComponent(mErrorComponent)
        .recyclerConfiguration(new ListRecyclerConfiguration(
            LinearLayoutManager.VERTICAL,
            false,
            SNAP_NONE,
            null))
        .section(
            SingleComponentSection.create(new SectionContext(mComponentContext))
                .component(mContentComponent)
                .build())
        .build();
  }

  @Test
  public void testNothingShown() throws Exception {
    mRecyclerCollectionComponent = RecyclerCollectionComponent.create(mComponentContext)
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
    LithoView view = ComponentTestHelper.mountComponent(
        mComponentContext,
        mRecyclerCollectionComponent);

    ViewTreeAssert.assertThat(ViewTree.of(view))
        .hasVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }
}
