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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.graphics.Color;
import android.graphics.Rect;
import androidx.viewpager.widget.ViewPager;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ComponentTreeIncrementalMountLocalVisibleBoundsTest {
  private TestLithoView mLithoView;
  private ComponentTree mComponentTree;

  private final Rect mMountedRect = new Rect();

  @Before
  public void setup() {
    ComponentContext context = new ComponentContext(getApplicationContext());
    mComponentTree =
        ComponentTree.create(
                context, SimpleMountSpecTester.create(context).color(Color.BLACK).build())
            .layoutDiffing(false)
            .build();

    mLithoView = new TestLithoView(context);
    mLithoView.setComponentTree(mComponentTree);
    Whitebox.setInternalState(mComponentTree, "mMainThreadLayoutState", mock(LayoutState.class));
  }

  @Test
  public void testGetLocalVisibleBounds() {
    mLithoView.shouldRetundCorrectedVisibleRect = new Rect(10, 5, 20, 15);
    mLithoView.performIncrementalMountForVisibleBoundsChange();
    assertThat(mMountedRect).isEqualTo(new Rect(10, 5, 20, 15));
  }

  @Test
  public void testViewPagerInHierarchy() {
    mLithoView.shouldRetundCorrectedVisibleRect = null;
    final ViewPager.OnPageChangeListener[] listener = new ViewPager.OnPageChangeListener[1];
    final Runnable[] runnable = new Runnable[1];
    ViewPager viewPager =
        new ViewPager(mComponentTree.getContext().getAndroidContext()) {
          @Override
          public void addOnPageChangeListener(OnPageChangeListener l) {
            listener[0] = l;
            super.addOnPageChangeListener(l);
          }

          @Override
          public void postOnAnimation(Runnable action) {
            runnable[0] = action;
            super.postOnAnimation(action);
          }

          @Override
          public void removeOnPageChangeListener(OnPageChangeListener l) {
            listener[0] = null;
            super.removeOnPageChangeListener(l);
          }
        };
    viewPager.addView(mLithoView);

    mComponentTree.attach();

    // This is set to null by mComponentTree.attach(), so set it again here.
    Whitebox.setInternalState(mComponentTree, "mMainThreadLayoutState", mock(LayoutState.class));

    assertThat(listener[0]).isNotNull();
    mLithoView.shouldRetundCorrectedVisibleRect = new Rect(10, 5, 20, 15);
    listener[0].onPageScrolled(10, 10, 10);
    assertThat(mMountedRect).isEqualTo(new Rect(10, 5, 20, 15));
    mComponentTree.detach();
    assertThat(runnable[0]).isNotNull();
    runnable[0].run();
    assertThat(listener[0]).isNull();
  }

  /**
   * Required in order to ensure that {@link LithoView#mount(LayoutState, Rect, boolean)} is mocked
   * correctly (it needs protected access to be mocked).
   */
  public class TestLithoView extends LithoView {

    public TestLithoView(ComponentContext context) {
      super(context);
    }

    Rect shouldRetundCorrectedVisibleRect = null;

    protected void mount(
        LayoutState layoutState, Rect currentVisibleArea, boolean processVisibilityOutputs) {
      if (processVisibilityOutputs) {
        mMountedRect.set(currentVisibleArea);
      }
      // We don't actually call mount. LayoutState is a mock :(
    }

    @Override
    boolean getCorrectedLocalVisibleRect(Rect outRect) {
      if (shouldRetundCorrectedVisibleRect != null) {
        outRect.set(shouldRetundCorrectedVisibleRect);
        return true;
      }

      return false;
    }
  }
}
