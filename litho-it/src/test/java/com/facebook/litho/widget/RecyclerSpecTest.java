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

package com.facebook.litho.widget;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.SnapHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.StateContainer;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link RecyclerSpec} */
@RunWith(LithoTestRunner.class)
public class RecyclerSpecTest {

  private ComponentContext mComponentContext;
  private TestSectionsRecyclerView mSectionsRecyclerView;
  private TestLithoRecyclerView mRecyclerView;
  private ItemAnimator mAnimator;
  private OnItemTouchListener onItemTouchListener;

  @Before
  public void setup() {
    mComponentContext = new ComponentContext(getApplicationContext());
    mRecyclerView = new TestLithoRecyclerView(mComponentContext.getAndroidContext());
    mSectionsRecyclerView =
        new TestSectionsRecyclerView(mComponentContext.getAndroidContext(), mRecyclerView);
    mSectionsRecyclerView.setHasBeenDetachedFromWindow(true);
    mAnimator = mock(RecyclerView.ItemAnimator.class);
    mRecyclerView.setItemAnimator(mAnimator);
    onItemTouchListener = mock(OnItemTouchListener.class);
  }

  @Test
  public void testRecyclerSpecOnBind() {
    EventHandler refreshHandler = mock(EventHandler.class);
    Binder<RecyclerView> binder = mock(Binder.class);

    SnapHelper snapHelper = mock(SnapHelper.class);

    final int size = 3;
    List<RecyclerView.OnScrollListener> scrollListeners = createListOfScrollListeners(size);

    LithoRecylerView.TouchInterceptor touchInterceptor =
        mock(LithoRecylerView.TouchInterceptor.class);

    RecyclerSpec.onBind(
        mComponentContext,
        mSectionsRecyclerView,
        binder,
        null,
        scrollListeners,
        snapHelper,
        true,
        touchInterceptor,
        onItemTouchListener,
        refreshHandler);

    assertThat(mSectionsRecyclerView.isEnabled()).isTrue();

    assertThat(mSectionsRecyclerView.getRecyclerView()).isSameAs(mRecyclerView);

    verifyAddOnScrollListenerWasCalledNTimes(mRecyclerView, size);
    assertThat(mRecyclerView.getTouchInterceptor()).isSameAs(touchInterceptor);

    verify(binder).bind(mRecyclerView);
    assertThat(mRecyclerView.isLayoutRequested()).isTrue();
    assertThat(mSectionsRecyclerView.hasBeenDetachedFromWindow()).isFalse();
    verify(snapHelper).attachToRecyclerView(mRecyclerView);
  }

  @Test
  public void testRecyclerSpecOnUnbind() {
    mSectionsRecyclerView.setHasBeenDetachedFromWindow(true);

    Binder<RecyclerView> binder = mock(Binder.class);

    final int size = 3;
    List<RecyclerView.OnScrollListener> scrollListeners = createListOfScrollListeners(size);

    RecyclerSpec.onUnbind(
        mComponentContext,
        mSectionsRecyclerView,
        binder,
        null,
        onItemTouchListener,
        scrollListeners);

    verify(binder).unbind(mRecyclerView);
    verifyRemoveOnScrollListenerWasCalledNTimes(mRecyclerView, size);
    assertThat(mSectionsRecyclerView.getOnRefreshListener()).isNull();
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
    public void updateStateSync(StateContainer.StateUpdate stateUpdate, String attribution) {
      super.updateStateSync(stateUpdate, attribution);
      mIsUpdateStateAsync = false;
    }

    @Override
    public void updateStateAsync(StateContainer.StateUpdate stateUpdate, String attribution) {
      super.updateStateAsync(stateUpdate, attribution);
      mIsUpdateStateAsync = true;
    }

    public boolean isUpdateStateAsync() {
      return mIsUpdateStateAsync;
    }
  }

  private static void verifyRemoveOnScrollListenerWasCalledNTimes(
      TestLithoRecyclerView recyclerView, int times) {
    assertThat(recyclerView.getRemoveOnScrollListenersCount()).isEqualTo(times);
  }

  private static void verifyAddOnScrollListenerWasCalledNTimes(
      TestLithoRecyclerView recyclerView, int times) {
    assertThat(recyclerView.getAddOnScrollListenersCount()).isEqualTo(times);
  }
}
