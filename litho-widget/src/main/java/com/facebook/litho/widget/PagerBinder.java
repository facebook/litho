/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.support.v4.util.Pools.SimplePool;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.components.ComponentTree;
import com.facebook.components.ComponentView;
import com.facebook.components.SizeSpec;

/**
 * A component binder for {@link ViewPager}.
 */
public abstract class PagerBinder extends BaseBinder<
    ViewPager,
    PagerBinder.PagerWorkingRangeController> {

  private static final int ADDITIONAL_ADAPTER_PAGES = 1;
  private static final float DEFAULT_PAGE_WIDTH = 1f;
  private static final int DEFAULT_INITIAL_PAGE = 0;

  private final InternalAdapter mAdapter;
  private final ViewPager.OnPageChangeListener mOnPageChangeListener;

  private float mPageWidth;
  private int mPagerOffscreenLimit;

  private ViewPager mViewPager;
  private ViewPager.OnPageChangeListener mClientOnPageChangeListener;

  protected int mCurrentItem;

  public PagerBinder(Context context) {
    this(context, DEFAULT_INITIAL_PAGE);
  }

