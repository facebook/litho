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

import android.view.ViewParent;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Class that helps with incremental mounting. In particular, we need to account for ViewPagers as
 * we have no reliable hook for knowing when they are changing their content (and we should
 * therefore potentially be mounting).
 */
class IncrementalMountHelper {
  private final ComponentTree mComponentTree;
  private List<ViewPagerListener> mViewPagerListeners;

  IncrementalMountHelper(ComponentTree componentTree) {
    mComponentTree = componentTree;
    mViewPagerListeners = new ArrayList<>(2);
  }

  void onAttach(LithoView lithoView) {
    if (!mComponentTree.isIncrementalMountEnabled()) {
      return;
    }

    // ViewPager does not give its child views any callbacks when it moves content onto the screen,
    // so we need to attach a listener to give us the information that we require.
    ViewParent viewParent = lithoView.getParent();
    while (viewParent != null) {
      if (viewParent instanceof ViewPager) {
        final ViewPager viewPager = (ViewPager) viewParent;
        final IncrementalMountHelper.ViewPagerListener viewPagerListener =
            new ViewPagerListener(mComponentTree, viewPager);

        // We want to add the listener immediately, since otherwise we might navigate to a
        // new tab in the ViewPager in this frame, and not mount the content. However, it is
        // possible that we are adding a listener here because its parent is being mounted due to
        // the ViewPager being scrolled (imagine a Recycler that is now on the screen and has to
        // mount a child view). In those cases adding the listener for the child will get a
        // ConcurrentModificationException, so we post it instead.
        try {
          viewPager.addOnPageChangeListener(viewPagerListener);
        } catch (ConcurrentModificationException e) {
          ViewCompat.postOnAnimation(
              viewPager,
              new Runnable() {
                @Override
                public void run() {
                  viewPager.addOnPageChangeListener(viewPagerListener);
                }
              });
        }

        mViewPagerListeners.add(viewPagerListener);
      }

      viewParent = viewParent.getParent();
    }
  }

  void onDetach(LithoView lithoView) {
    for (int i = 0, size = mViewPagerListeners.size(); i < size; i++) {
      ViewPagerListener viewPagerListener = mViewPagerListeners.get(i);
      viewPagerListener.release();
    }

    mViewPagerListeners.clear();
  }

  private static class ViewPagerListener extends ViewPager.SimpleOnPageChangeListener {
    private final WeakReference<ComponentTree> mComponentTree;
    private final WeakReference<ViewPager> mViewPager;

    private ViewPagerListener(ComponentTree componentTree, ViewPager viewPager) {
      mComponentTree = new WeakReference<>(componentTree);
      mViewPager = new WeakReference<>(viewPager);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      final ComponentTree componentTree = mComponentTree.get();
      if (componentTree != null) {
        componentTree.incrementalMountComponent();
      }
    }

    private void release() {
      final ViewPager viewPager = mViewPager.get();
      if (viewPager != null) {
        ViewCompat.postOnAnimation(
            viewPager,
            new Runnable() {
              @Override
              public void run() {
                viewPager.removeOnPageChangeListener(ViewPagerListener.this);
              }
            });
      }
    }
  }
}
