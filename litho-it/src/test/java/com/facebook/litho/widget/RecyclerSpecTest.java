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

package com.facebook.litho.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.SnapHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Output;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link RecyclerSpec} */
@RunWith(ComponentsTestRunner.class)
public class RecyclerSpecTest {

  private ComponentContext mComponentContext;
  private SectionsRecyclerView mSectionsRecyclerView;
  private LithoRecylerView mRecyclerView;
  private ItemAnimator mAnimator;

  @Before
  public void setup() {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    mSectionsRecyclerView = mock(SectionsRecyclerView.class);
    mRecyclerView = mock(LithoRecylerView.class);
    when(mSectionsRecyclerView.getRecyclerView()).thenReturn(mRecyclerView);
    when(mSectionsRecyclerView.hasBeenDetachedFromWindow()).thenReturn(true);

    mAnimator = mock(RecyclerView.ItemAnimator.class);
    when(mRecyclerView.getItemAnimator()).thenReturn(mAnimator);
  }

  @Test
  public void testRecyclerSpecOnBind() {
    OnRefreshListener onRefreshListener = mock(OnRefreshListener.class);
    Binder<RecyclerView> binder = mock(Binder.class);

    Output<ItemAnimator> oldAnimator = mock(Output.class);

    SnapHelper snapHelper = mock(SnapHelper.class);

    final int size = 3;
    List<RecyclerView.OnScrollListener> scrollListeners = createListOfScrollListeners(size);

    LithoRecylerView.TouchInterceptor touchInterceptor =
        mock(LithoRecylerView.TouchInterceptor.class);

    RecyclerSpec.onBind(
        mComponentContext,
        mSectionsRecyclerView,
        binder,
        mAnimator,
        null,
        scrollListeners,
        snapHelper,
        true,
        touchInterceptor,
        onRefreshListener,
        oldAnimator);

    verify(mSectionsRecyclerView).setEnabled(true);
    verify(mSectionsRecyclerView).setOnRefreshListener(onRefreshListener);
    verify(mSectionsRecyclerView, times(1)).getRecyclerView();
    verify(oldAnimator).set(mAnimator);
    verify(mRecyclerView).setItemAnimator(any(ItemAnimator.class));
    verify(mRecyclerView, times(size)).addOnScrollListener(any(OnScrollListener.class));
    verify(mRecyclerView).setTouchInterceptor(touchInterceptor);
    verify(binder).bind(mRecyclerView);
    verify(mRecyclerView, times(1)).requestLayout();
    verify(mSectionsRecyclerView).setHasBeenDetachedFromWindow(false);
    verify(snapHelper).attachToRecyclerView(mRecyclerView);
  }

  @Test
  public void testRecyclerSpecOnUnbind() {
    when(mSectionsRecyclerView.hasBeenDetachedFromWindow()).thenReturn(true);

    Binder<RecyclerView> binder = mock(Binder.class);

    final int size = 3;
    List<RecyclerView.OnScrollListener> scrollListeners = createListOfScrollListeners(size);

    RecyclerSpec.onUnbind(
        mComponentContext, mSectionsRecyclerView, binder, null, scrollListeners, mAnimator);

    verify(mRecyclerView).setItemAnimator(mAnimator);
    verify(binder).unbind(mRecyclerView);
    verify(mRecyclerView, times(size)).removeOnScrollListener(any(OnScrollListener.class));
    verify(mSectionsRecyclerView).setOnRefreshListener(null);
  }

  @Test
  public void testUpdateStateAsyncWithRemeasureEvent() {
    final Recycler recycler = Recycler.create(mComponentContext).binder(mock(Binder.class)).build();
    final TestComponentContext testComponentContext =
        new TestComponentContext(mComponentContext.getAndroidContext(), recycler);

    RecyclerSpec.onRemeasure(testComponentContext, 0);
    assertThat(testComponentContext.isUpdateStateAsync()).isTrue();
  }

  @Test
  public void testShouldAlwaysRemeasure() {
    final Binder<RecyclerView> binder = mock(Binder.class);
    final Recycler recycler = Recycler.create(mComponentContext).binder(binder).build();

    when(binder.isWrapContent()).thenReturn(false);
    assertThat(recycler.shouldAlwaysRemeasure()).isFalse();

    when(binder.isWrapContent()).thenReturn(true);
    assertThat(recycler.shouldAlwaysRemeasure()).isTrue();
  }

  private static List<RecyclerView.OnScrollListener> createListOfScrollListeners(int size) {
    List<RecyclerView.OnScrollListener> onScrollListeners = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      onScrollListeners.add(mock(RecyclerView.OnScrollListener.class));
    }

    return onScrollListeners;
  }

  private static class TestComponentContext extends ComponentContext {

    private Recycler mRecycler;
    private boolean mIsUpdateStateAsync = false;

    public TestComponentContext(Context context, Recycler recycler) {
      super(context);

      mRecycler = recycler;
    }

    @Override
    public Component getComponentScope() {
      return mRecycler;
    }

    @Override
    public void updateStateSync(ComponentLifecycle.StateUpdate stateUpdate, String attribution) {
      super.updateStateSync(stateUpdate, attribution);
      mIsUpdateStateAsync = false;
    }

    @Override
    public void updateStateAsync(ComponentLifecycle.StateUpdate stateUpdate, String attribution) {
      super.updateStateAsync(stateUpdate, attribution);
      mIsUpdateStateAsync = true;
    }

    public boolean isUpdateStateAsync() {
      return mIsUpdateStateAsync;
    }
  }
}
