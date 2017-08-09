/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.util.Pools;
import android.support.v4.util.Pools.Pool;
import android.support.v4.util.SimpleArrayMap;
import javax.annotation.Nullable;

/**
 * Keeps the {@link Component} and its information that will allow the framework
 * to understand how to render it.
 *
 * SpanSize will be defaulted to 1. It is the information that is required to calculate
 * how much of the SpanCount the component should occupy in a Grid layout.
 *
 * IsSticky will be defaulted to false. It determines if the component should be
 * a sticky header or not
 */
public class RenderInfo {

  public static final String CLIP_CHILDREN = "clip_children";

  private static final Pool<Builder> sBuilderPool = new Pools.SynchronizedPool<>(2);
  private static final String IS_STICKY = "is_sticky";
  private static final String SPAN_SIZE = "span_size";

  private final @Nullable Component mComponent;
  private final @Nullable SimpleArrayMap<String, Object> mCustomAttributes;

  public static Builder create() {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }

    return builder;
  }

  public static RenderInfo createEmpty() {
    return new RenderInfo();
  }

  private RenderInfo(Builder builder) {
    mComponent = builder.mComponent;
    mCustomAttributes = builder.mCustomAttributes;
  }

  private RenderInfo() {
    mComponent = null;
    mCustomAttributes = null;
  }

  public @Nullable Component getComponent() {
    return mComponent;
  }

  public boolean isSticky() {
    if (mCustomAttributes == null || !mCustomAttributes.containsKey(IS_STICKY)) {
      return false;
    }

    return (boolean) mCustomAttributes.get(IS_STICKY);
  }

  public int getSpanSize() {
    if (mCustomAttributes == null || !mCustomAttributes.containsKey(SPAN_SIZE)) {
      return 1;
    }

    return (int) mCustomAttributes.get(SPAN_SIZE);
  }

  public @Nullable Object getCustomAttribute(String key) {
    return mCustomAttributes == null ? null : mCustomAttributes.get(key);
  }

  public static class Builder {

    private @Nullable Component mComponent;
    private @Nullable SimpleArrayMap<String, Object> mCustomAttributes;

    private Builder() {
      mComponent = null;
    }

    public Builder component(Component component) {
      mComponent = component;
      return this;
    }

    public Builder isSticky(boolean isSticky) {
      return customAttribute(IS_STICKY, isSticky);
    }

    public Builder spanSize(int spanSize) {
      return customAttribute(SPAN_SIZE, spanSize);
    }

    public Builder customAttribute(String key, Object value) {
      if (mCustomAttributes == null) {
        mCustomAttributes = new SimpleArrayMap<>();
      }
      mCustomAttributes.put(key, value);

      return this;
    }

    public RenderInfo build() {
      RenderInfo renderInfo =  new RenderInfo(this);
      release();

      return renderInfo;
    }

    private void release() {
      mComponent = null;
      mCustomAttributes = null;
      sBuilderPool.release(this);
    }
  }
}
