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

import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.SizeSpec;

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

  public PagerBinder(Context context, int initialPage) {
    this(context, initialPage, DEFAULT_PAGE_WIDTH);
  }

  public PagerBinder(Context context, int initialPage, float pageWidth) {
    this(context, new PagerWorkingRangeController(), initialPage, pageWidth);
  }

  public PagerBinder(Context context, PagerWorkingRangeController rangeController, int initialPage,
      float pageWidth) {
    super(context, rangeController);

    mCurrentItem = initialPage;
    mPageWidth = pageWidth;
    mPagerOffscreenLimit = (int) Math.ceil(1 / mPageWidth);
    mAdapter = new InternalAdapter(this, context);
    mOnPageChangeListener = new InternalOnPageChangeListener(this);

    getRangeController().setPagerOffscreenLimit(mPagerOffscreenLimit);

    setListener(mAdapter);
  }

  /**
   * Set a listener for page change events.  DO NOT FORGET TO UNSET THIS LISTENER
   * @param clientOnPageChangeListener The listener to set
   */
  public void setOnPageChangeListener(
      ViewPager.OnPageChangeListener clientOnPageChangeListener) {
    mClientOnPageChangeListener = clientOnPageChangeListener;
  }

  public int getCurrentItem() {
    return mCurrentItem;
  }

  public int getPagerOffscreenLimit() {
    return mPagerOffscreenLimit;
  }

  @Override
  protected int getWidthSpec(int position) {
    int widthSpec = super.getWidthSpec(position);
    int width = (int) (SizeSpec.getSize(widthSpec) * mPageWidth);

    return SizeSpec.makeSizeSpec(width, SizeSpec.getMode(widthSpec));
  }

  /**
   * Overrides this method to provide a title for for the page at the given position.
   */
  protected CharSequence getPageTitle(int position) {
    return null;
  }

  @Override
  protected int getInitializeStartPosition() {
    return Math.max(0, mCurrentItem - getRangeController().getHalfWorkingRangeSize());
  }

  // TODO(12986103): Remove onBoundsDefined once the experiment proved to be ok.
  @Override
  public void onBoundsDefined() {
    getRangeController().notifyOnPageSelected(
        mCurrentItem,
        URFLAG_REFRESH_IN_RANGE | URFLAG_RELEASE_OUTSIDE_RANGE);
  }

  @Override
  public void onMount(ViewPager viewPager) {
    mViewPager = viewPager;

    viewPager.setOnPageChangeListener(mOnPageChangeListener);
    viewPager.setAdapter(mAdapter);
    viewPager.setCurrentItem(mCurrentItem);
    viewPager.setOffscreenPageLimit(mPagerOffscreenLimit);
  }

  @Override
  public void onBind(ViewPager viewPager) {
  }

  @Override
  public void onUnbind(ViewPager viewPager) {
  }

  @Override
  public void onUnmount(ViewPager viewPager) {
    if (mViewPager != null) { // Temporary workaround for misuse of this class; see #16387087
      mViewPager.setOnPageChangeListener(null);
      mViewPager.setAdapter(null);
      mViewPager = null;
    }
  }

  @Override
  protected boolean shouldContinueInitialization(
      int position,
      int itemWidth,
      int itemHeight) {

    // The end of the initialization range.
    final int endRange = mCurrentItem + getRangeController().getHalfWorkingRangeSize();

    // Check if the initial range is filled. The initial range consists of the viewport and the
    // the items that should be loaded before and after the currently visible ones.
    return position < endRange;
  }

  private static class InternalAdapter extends PagerAdapter implements BaseBinder.Listener {

    private static final int VIEW_POOL_SIZE = 5;

    private final PagerBinder mBinder;
    private final Context mContext;
    private final SimplePool<ComponentView> mComponentViewPool = new SimplePool<>(VIEW_POOL_SIZE);

    InternalAdapter(PagerBinder binder, Context context) {
      mBinder = binder;
      mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      ComponentView componentView = mComponentViewPool.acquire();
      if (componentView == null) {
        componentView = new ComponentView(mContext);
      }

      final ComponentTree component = mBinder.getComponentAt(position);
      // When the viewPager get bound, we cannot set the current item immediately but setAdapter()
      // will try to initialize the first few pages that will be null if the intended initial item
      // is != 0. However if we don't have a component while the currentItems of the viewPager
      // and of the binder are matching, there is probably a bug somewhere!
      if (component == null &&
          mBinder.mViewPager != null &&
          mBinder.getCurrentItem() == mBinder.mViewPager.getCurrentItem()) {
        throw new IllegalStateException("Null component while initializing a new page.");
      }

      componentView.setComponent(component);

