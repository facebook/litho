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

import android.view.ViewParent;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;
import com.facebook.infer.annotation.Nullsafe;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Class that helps with incremental mounting. In particular, we need to account for ViewPagers as
 * we have no reliable hook for knowing when they are changing their content (and we should
 * therefore potentially be mounting).
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
class IncrementalMountHelper {
  private final ComponentTree mComponentTree;
  private final List<ViewPagerListener> mViewPagerListeners;
  private final List<ViewPager2Listener> mViewPager2Listeners;

  IncrementalMountHelper(ComponentTree componentTree) {
    mComponentTree = componentTree;
    mViewPagerListeners = new ArrayList<>(2);
    mViewPager2Listeners = new ArrayList<>(2);
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
      } else if (viewParent instanceof ViewPager2) {
        final ViewPager2 viewPager = (ViewPager2) viewParent;
        final IncrementalMountHelper.ViewPager2Listener viewPager2Listener =
            new ViewPager2Listener(mComponentTree, viewPager);

        try {
          viewPager.registerOnPageChangeCallback(viewPager2Listener);
        } catch (ConcurrentModificationException e) {
          ViewCompat.postOnAnimation(
              viewPager,
              new Runnable() {
                @Override
                public void run() {
                  viewPager.registerOnPageChangeCallback(viewPager2Listener);
                }
              });
        }
        mViewPager2Listeners.add(viewPager2Listener);
      }

      viewParent = viewParent.getParent();
    }
  }

  void onDetach() {
    for (int i = 0, size = mViewPagerListeners.size(); i < size; i++) {
      ViewPagerListener viewPagerListener = mViewPagerListeners.get(i);
      viewPagerListener.release();
    }
    mViewPagerListeners.clear();
    for (int i = 0, size = mViewPager2Listeners.size(); i < size; i++) {
      ViewPager2Listener viewPagerListener = mViewPager2Listeners.get(i);
      viewPagerListener.release();
    }
    mViewPager2Listeners.clear();
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
      mComponentTree.clear();
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

  private static class ViewPager2Listener extends ViewPager2.OnPageChangeCallback {

    private final WeakReference<ComponentTree> mComponentTree;
    private final WeakReference<ViewPager2> mViewPager;

    private ViewPager2Listener(ComponentTree componentTree, ViewPager2 viewPager) {
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
      mComponentTree.clear();
      final ViewPager2 viewPager = mViewPager.get();
      if (viewPager != null) {
        ViewCompat.postOnAnimation(
            viewPager,
            new Runnable() {
              @Override
              public void run() {
                viewPager.unregisterOnPageChangeCallback(ViewPager2Listener.this);
              }
            });
      }
    }
  }
}
