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

import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;

/** {@link RenderInfo} that can render views. */
public class ViewRenderInfo extends BaseRenderInfo {

  private final ViewBinder mViewBinder;
  private final ViewCreator mViewCreator;

  private final boolean mHasCustomViewType;
  private int mViewType;

  public static Builder create() {
    return new Builder();
  }

  private ViewRenderInfo(Builder builder) {
    super(builder);
    mViewBinder = builder.viewBinder;
    mViewCreator = builder.viewCreator;
    mHasCustomViewType = builder.hasCustomViewType;
    if (mHasCustomViewType) {
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
    return mHasCustomViewType;
  }

  @Override
  public void setViewType(int viewType) {
    if (mHasCustomViewType) {
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

  public static class Builder extends BaseRenderInfo.Builder<Builder> {
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
      this.hasCustomViewType = true;
      this.viewType = viewType;
      return this;
    }

    public ViewRenderInfo build() {
      if (viewCreator == null || viewBinder == null) {
        throw new IllegalStateException("Both viewCreator and viewBinder must be provided.");
      }

      return new ViewRenderInfo(this);
    }

    @Override
    public Builder isFullSpan(boolean isFullSpan) {
      throw new UnsupportedOperationException("ViewRenderInfo does not support isFullSpan.");
    }
  }
}
