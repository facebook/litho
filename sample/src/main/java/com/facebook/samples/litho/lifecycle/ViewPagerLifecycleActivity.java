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

package com.facebook.samples.litho.lifecycle;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.facebook.samples.litho.R;
import java.util.HashMap;

public class ViewPagerLifecycleActivity extends FragmentActivity {

  private static final int NUM_PAGES = 3;
  private ViewPager mViewPager;
  private PagerAdapter mPagerAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_viewpager_lifecycle);

    mViewPager = (ViewPager) findViewById(R.id.pager);
    mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
    mViewPager.setAdapter(mPagerAdapter);
  }

  @Override
  public void onBackPressed() {
    if (mViewPager.getCurrentItem() == 0) {
      // If the user is currently looking at the first step, allow the system to handle the
      // Back button. This calls finish() on this activity and pops the back stack.
      super.onBackPressed();
    } else {
      // Otherwise, select the previous step.
      mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  public static class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
    public ScreenSlidePagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      HashMap<Integer, Fragment> fragmentMap = new HashMap<>();
      fragmentMap.put(position, ScreenSlidePageFragment.newInstance(position));
      return fragmentMap.get(position);
    }

    @Override
    public int getCount() {
      return NUM_PAGES;
    }
  }
}
