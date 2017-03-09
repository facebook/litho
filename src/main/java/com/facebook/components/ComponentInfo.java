// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.v4.util.Pools;
import android.support.v4.util.Pools.Pool;
import android.support.v4.util.SimpleArrayMap;

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
public class ComponentInfo {

  private static final Pool<Builder> sBuilderPool = new Pools.SynchronizedPool<>(2);
  private static final Pool<ComponentInfo> sComponentInfoPool = new Pools.SynchronizedPool<>(8);

  private Component mComponent;
  private boolean mIsSticky;
  private int mSpanSize;

  private SimpleArrayMap<String, Object> mCustomAttributes;

  public static Builder create() {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }

    return builder;
  }

  private ComponentInfo() {
    mComponent = null;
    mIsSticky = false;
    mSpanSize = 1;
  }

  public Component getComponent() {
    return mComponent;
  }

  public boolean isSticky() {
    return mIsSticky;
  }

  public int getSpanSize() {
    return mSpanSize;
  }

  public Object getCustomAttribute(String key) {
    return mCustomAttributes == null ? null : mCustomAttributes.get(key);
  }

  public void release() {
    mComponent = null;
    mIsSticky = false;
    mSpanSize = 1;
    sComponentInfoPool.release(this);
  }

  private void init(Builder builder) {
    mComponent = builder.mComponent;
    mIsSticky = builder.mIsSticky;
    mSpanSize = builder.mSpanSize;
    mCustomAttributes = builder.mCustomAttributes;
  }

  public static class Builder {

    private Component mComponent;
    private boolean mIsSticky;
    private int mSpanSize;
    private SimpleArrayMap<String, Object> mCustomAttributes;

    private Builder() {
      mComponent = null;
      mIsSticky = false;
      mSpanSize = 1;
    }

    public Builder component(Component component) {
      mComponent = component;
      return this;
    }

    public Builder isSticky(boolean isSticky) {
      mIsSticky = isSticky;
      return this;
    }

    public Builder spanSize(int spanSize) {
      mSpanSize = spanSize;
      return this;
    }

    public Builder customAttribute(String key, Object value) {
      if (mCustomAttributes == null) {
        mCustomAttributes = new SimpleArrayMap<>();
      }
      mCustomAttributes.put(key, value);

      return this;
    }

    public ComponentInfo build() {
      ComponentInfo componentInfo = sComponentInfoPool.acquire();
      if (componentInfo == null) {
        componentInfo = new ComponentInfo();
      }
      componentInfo.init(this);

      release();

      return componentInfo;
    }

    private void release() {
      mComponent = null;
      mCustomAttributes = null;
      mIsSticky = false;
      mSpanSize = 1;
      sBuilderPool.release(this);
    }
  }
}
