/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.view.ViewPager;
import android.view.ViewParent;
import com.facebook.litho.config.ComponentsConfiguration;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that helps with incremental mounting. In particular, we need to account for ViewPagers as
 * we have no reliable hook for knowing when they are changing their content (and we should
 * therefore potentially be mounting).
 */
class IncrementalMountHelper {
  private final ComponentTree mComponentTree;
  private final List<ViewPagerListener> mViewPagerListeners = new ArrayList<>(2);

  IncrementalMountHelper(ComponentTree componentTree) {
    mComponentTree = componentTree;
  }

  void onAttach(LithoView lithoView) {
    if (!mComponentTree.isIncrementalMountEnabled()) {
      return;
    }

    // ViewPager does not give its child views any callbacks when it moves content onto the screen,
    // so we need to attach a listener to give us the information that we require.
    if (ComponentsConfiguration.incrementalMountUsesLocalVisibleBounds) {
      ViewParent viewParent = lithoView.getParent();
      while (viewParent != null) {
        if (viewParent instanceof ViewPager) {
          ViewPager viewPager = (ViewPager) viewParent;
          IncrementalMountHelper.ViewPagerListener viewPagerListener =
              new ViewPagerListener(mComponentTree, viewPager);
          viewPager.addOnPageChangeListener(viewPagerListener);
          mViewPagerListeners.add(viewPagerListener);
        }

        viewParent = viewParent.getParent();
      }
    }
  }

  void onDetach() {
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
        viewPager.removeOnPageChangeListener(this);
      }
    }
  }
}
