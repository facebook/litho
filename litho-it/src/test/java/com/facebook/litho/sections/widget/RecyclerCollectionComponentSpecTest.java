/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import static com.facebook.litho.sections.LoadingEvent.LoadingState.FAILED;
import static com.facebook.litho.sections.LoadingEvent.LoadingState.LOADING;
import static com.facebook.litho.sections.LoadingEvent.LoadingState.SUCCEEDED;
import static com.facebook.litho.sections.widget.ListRecyclerConfiguration.SNAP_NONE;
import static com.facebook.litho.testing.viewtree.ViewTreeAssert.assertThat;

import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.testing.StateUpdatesTestHelper;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.testing.viewtree.ViewTree;
import com.facebook.litho.widget.Text;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

/**
 * Tests {@link RecyclerCollectionComponentSpec}
 */
@RunWith(ComponentsTestRunner.class)
public class RecyclerCollectionComponentSpecTest {

  private ComponentContext mComponentContext;
  private Component mLoadingComponent;
  private Component mEmptyComponent;
  private Component mErrorComponent;
  private Component mRecyclerCollectionComponent;
  private ShadowLooper mLayoutThreadShadowLooper;
  private Component mContentComponent;

  @Before
  public void setup() throws Exception {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);

    mLoadingComponent = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Text.create(c)
            .text("loading")
            .heightPx(100)
            .widthPx(100)
            .buildWithLayout();
      }
    };

    mEmptyComponent = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Text.create(c)
            .text("empty")
            .heightPx(100)
            .widthPx(100)
            .buildWithLayout();
      }
    };
    mErrorComponent = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Text.create(c)
            .text("error")
            .heightPx(100)
            .widthPx(100)
            .buildWithLayout();
      }
    };
    mContentComponent = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Text.create(c)
            .text("content")
            .heightPx(100)
            .widthPx(100)
            .buildWithLayout();
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

    mLayoutThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            ComponentTree.class,
            "getDefaultLayoutThreadLooper"));
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

    LithoView view = StateUpdatesTestHelper.getViewAfterStateUpdate(
        mComponentContext,
        mRecyclerCollectionComponent,
        new StateUpdatesTestHelper.StateUpdater() {
          @Override
          public void performStateUpdate(ComponentContext context) {
            RecyclerCollectionComponent.updateLoadingAndEmptyAsync(context, SUCCEEDED, true);
          }
        },
        mLayoutThreadShadowLooper);

    assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .doesNotHaveVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testNoEmptySucceeded() throws Exception {
    LithoView view = StateUpdatesTestHelper.getViewAfterStateUpdate(
        mComponentContext,
        mRecyclerCollectionComponent,
        new StateUpdatesTestHelper.StateUpdater() {
          @Override
          public void performStateUpdate(ComponentContext context) {
            RecyclerCollectionComponent.updateLoadingAndEmptyAsync(context, SUCCEEDED, false);
          }
        },
        mLayoutThreadShadowLooper);

    assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testEmptySucceeded() throws Exception {
    LithoView view = StateUpdatesTestHelper.getViewAfterStateUpdate(
        mComponentContext,
        mRecyclerCollectionComponent,
        new StateUpdatesTestHelper.StateUpdater() {
          @Override
          public void performStateUpdate(ComponentContext context) {
            RecyclerCollectionComponent.updateLoadingAndEmptyAsync(context, SUCCEEDED, true);
          }
        },
        mLayoutThreadShadowLooper);

    assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .hasVisibleText("content")
        .hasVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testNoEmptyError() throws Exception {
    LithoView view = StateUpdatesTestHelper.getViewAfterStateUpdate(
        mComponentContext,
        mRecyclerCollectionComponent,
        new StateUpdatesTestHelper.StateUpdater() {
          @Override
          public void performStateUpdate(ComponentContext context) {
            RecyclerCollectionComponent.updateLoadingAndEmptyAsync(context, FAILED, false);
          }
        },
        mLayoutThreadShadowLooper);

    assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testEmptyError() throws Exception {
    LithoView view = StateUpdatesTestHelper.getViewAfterStateUpdate(
        mComponentContext,
        mRecyclerCollectionComponent,
        new StateUpdatesTestHelper.StateUpdater() {
          @Override
          public void performStateUpdate(ComponentContext context) {
            RecyclerCollectionComponent.updateLoadingAndEmptyAsync(context, FAILED, true);
          }
        },
        mLayoutThreadShadowLooper);

    assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .hasVisibleText("error");
  }

  @Test
  public void testNoEmptyLoading() throws Exception {
    LithoView view = StateUpdatesTestHelper.getViewAfterStateUpdate(
        mComponentContext,
        mRecyclerCollectionComponent,
        new StateUpdatesTestHelper.StateUpdater() {
          @Override
          public void performStateUpdate(ComponentContext context) {
            RecyclerCollectionComponent.updateLoadingAndEmptyAsync(context, LOADING, false);
          }
        },
        mLayoutThreadShadowLooper);

    assertThat(ViewTree.of(view))
        .doesNotHaveVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }

  @Test
  public void testEmptyLoading() throws Exception {
    LithoView view = StateUpdatesTestHelper.getViewAfterStateUpdate(
        mComponentContext,
        mRecyclerCollectionComponent,
        new StateUpdatesTestHelper.StateUpdater() {
          @Override
          public void performStateUpdate(ComponentContext context) {
            RecyclerCollectionComponent.updateLoadingAndEmptyAsync(context, LOADING, true);
          }
        },
        mLayoutThreadShadowLooper);

    assertThat(ViewTree.of(view))
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

    assertThat(ViewTree.of(view))
        .hasVisibleText("loading")
        .hasVisibleText("content")
        .doesNotHaveVisibleText("empty")
        .doesNotHaveVisibleText("error");
  }
}
