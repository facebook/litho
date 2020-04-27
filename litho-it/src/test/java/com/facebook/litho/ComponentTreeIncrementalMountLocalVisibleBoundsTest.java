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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import androidx.viewpager.widget.ViewPager;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeIncrementalMountLocalVisibleBoundsTest {
  private LithoView mLithoView;
  private ComponentTree mComponentTree;

  private final Rect mMountedRect = new Rect();

  @Before
  public void setup() {
    ComponentContext context = new ComponentContext(RuntimeEnvironment.application);
    mComponentTree =
        ComponentTree.create(
                context, TestDrawableComponent.create(context).color(Color.BLACK).build())
            .layoutDiffing(false)
            .build();

    mLithoView = mock(TestLithoView.class);
    when(mLithoView.getMountState()).thenReturn(mock(MountState.class));
    Whitebox.setInternalState(mComponentTree, "mLithoView", mLithoView);
    Whitebox.setInternalState(mComponentTree, "mMainThreadLayoutState", mock(LayoutState.class));

    // Can't use verify as the rect is reset when it is released back to the pool, which occurs
    // before we can check it.
    doAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                mMountedRect.set((Rect) invocation.getArguments()[1]);
                return null;
              }
            })
        .when(mLithoView)
        .mount((LayoutState) any(), (Rect) any(), eq(true));
  }

  @Test
  public void testGetLocalVisibleBounds() {
    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                rect.set(new Rect(10, 5, 20, 15));
                return true;
              }
            })
        .when(mLithoView)
        .getLocalVisibleRect((Rect) any());

    mComponentTree.incrementalMountComponent();
    assertThat(mMountedRect).isEqualTo(new Rect(10, 5, 20, 15));
  }

  @Test
  public void testViewPagerInHierarchy() {
    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return false;
              }
            })
        .when(mLithoView)
        .getLocalVisibleRect((Rect) any());

    ViewPager viewPager = mock(ViewPager.class);
    when(mLithoView.getParent()).thenReturn(viewPager);

    mComponentTree.attach();

    // This is set to null by mComponentTree.attach(), so set it again here.
    Whitebox.setInternalState(mComponentTree, "mMainThreadLayoutState", mock(LayoutState.class));

    ArgumentCaptor<ViewPager.OnPageChangeListener> listenerArgumentCaptor =
        ArgumentCaptor.forClass(ViewPager.OnPageChangeListener.class);
    verify(viewPager).addOnPageChangeListener(listenerArgumentCaptor.capture());

    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                rect.set(new Rect(10, 5, 20, 15));
                return true;
              }
            })
        .when(mLithoView)
        .getLocalVisibleRect((Rect) any());

    listenerArgumentCaptor.getValue().onPageScrolled(10, 10, 10);
    assertThat(mMountedRect).isEqualTo(new Rect(10, 5, 20, 15));

    mComponentTree.detach();

    ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(viewPager).postOnAnimation(runnableArgumentCaptor.capture());

    runnableArgumentCaptor.getValue().run();
    verify(viewPager).removeOnPageChangeListener(listenerArgumentCaptor.getValue());
  }

  /**
   * Required in order to ensure that {@link LithoView#mount(LayoutState, Rect, boolean)} is mocked
   * correctly (it needs protected access to be mocked).
   */
  public static class TestLithoView extends LithoView {

    public TestLithoView(Context context) {
      super(context);
    }

    protected void mount(
        LayoutState layoutState, Rect currentVisibleArea, boolean processVisibilityOutputs) {
      super.mount(layoutState, currentVisibleArea, processVisibilityOutputs);
    }
  }
}
