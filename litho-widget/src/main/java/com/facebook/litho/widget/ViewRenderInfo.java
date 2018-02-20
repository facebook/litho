/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v4.util.Pools;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;

/** {@link RenderInfo} that can render views. */
public class ViewRenderInfo extends RenderInfo {

  private static final Pools.Pool<Builder> sBuilderPool = new Pools.SynchronizedPool<>(2);

  private final ViewBinder mViewBinder;
  private final ViewCreator mViewCreator;

  private final boolean mCustomViewType;
  private int mViewType;

  public static Builder create() {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }

    return builder;
  }

  private ViewRenderInfo(Builder builder) {
    super(builder);
    mViewBinder = builder.viewBinder;
    mViewCreator = builder.viewCreator;
    mCustomViewType = builder.hasCustomViewType;
    if (mCustomViewType) {
      mViewType = builder.viewType;
    }
  }

  @Override
  public boolean rendersView() {
    return true;
  }

  @Override
  public ViewBinder getViewBinder() {
    return mViewBinder;
  }

  @Override
  public ViewCreator getViewCreator() {
    return mViewCreator;
  }

  @Override
  public boolean hasCustomViewType() {
    return mCustomViewType;
  }

  @Override
  void setViewType(int viewType) {
    if (mCustomViewType) {
      throw new UnsupportedOperationException("Cannot override custom view type.");
    }
    mViewType = viewType;
  }

  @Override
  public int getViewType() {
    return mViewType;
  }

  @Override
  public String getName() {
    return "View (viewType=" + mViewType + ")";
  }

  public static class Builder extends RenderInfo.Builder<Builder> {
    private ViewBinder viewBinder;
    private ViewCreator viewCreator;
    private boolean hasCustomViewType = false;
    private int viewType = 0;

    /**
     * Specify {@link ViewCreator} implementation that can be used to create a new view if such view
     * is absent in recycling cache. For the same type of views same {@link ViewCreator} instance
     * should be provided.
     */
    public Builder viewCreator(ViewCreator viewCreator) {
      this.viewCreator = viewCreator;
      return this;
    }

    /**
     * Specify {@link ViewBinder} implementation that can bind model to the view provided from
     * {@link #viewCreator(ViewCreator)}.
     */
    public Builder viewBinder(ViewBinder viewBinder) {
      this.viewBinder = viewBinder;
      return this;
    }

    /**
     * Specify a custom ViewType identifier for this View. This will be used instead of being
     * generated from the {@link ViewCreator} instance.
     */
    public Builder customViewType(int viewType) {
      hasCustomViewType = true;
      this.viewType = viewType;
      return this;
    }

    public ViewRenderInfo build() {
      if (viewCreator == null || viewBinder == null) {
        throw new IllegalStateException("Both viewCreator and viewBinder must be provided.");
      }

      final ViewRenderInfo viewRenderInfo = new ViewRenderInfo(this);
      release();

      return viewRenderInfo;
    }

    @Override
    void release() {
      super.release();
      viewBinder = null;
      viewCreator = null;
      hasCustomViewType = false;
      viewType = 0;
      sBuilderPool.release(this);
    }
  }
}
