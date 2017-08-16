/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v4.util.SimpleArrayMap;
import com.facebook.litho.Component;
import javax.annotation.Nullable;

/**
 * Keeps the list item information that will allow the framework to understand how to render it.
 *
 * <p>SpanSize will be defaulted to 1. It is the information that is required to calculate how much
 * of the SpanCount the component should occupy in a Grid layout.
 *
 * <p>IsSticky will be defaulted to false. It determines if the component should be a sticky header
 * or not
 */
public abstract class RenderInfo {

  public static final String CLIP_CHILDREN = "clip_children";

  private static final String IS_STICKY = "is_sticky";
  private static final String SPAN_SIZE = "span_size";

  private final @Nullable SimpleArrayMap<String, Object> mCustomAttributes;

  RenderInfo(Builder builder) {
    mCustomAttributes = builder.mCustomAttributes;
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

  /**
   * @return Valid {@link Component} if {@link RenderInfo} was created through {@link
   *     ComponentRenderInfo#create()}, otherwise it will throw {@link
   *     UnsupportedOperationException}. If this method is accessed from {@link RenderInfo} type,
   *     {@link #hasComponent()} should be queried first before accessing.
   */
  public Component getComponent() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return true, if {@link RenderInfo} was created through {@link ComponentRenderInfo#create()},
   *     or false otherwise. This should be queried before accessing {@link #getComponent() } from
   *     {@link RenderInfo} type.
   */
  public boolean hasComponent() {
    return false;
  }

  public abstract String getName();

  public abstract static class Builder<T> {

    private @Nullable SimpleArrayMap<String, Object> mCustomAttributes;

    public T isSticky(boolean isSticky) {
      return customAttribute(IS_STICKY, isSticky);
    }

    public T spanSize(int spanSize) {
      return customAttribute(SPAN_SIZE, spanSize);
    }

    public T customAttribute(String key, Object value) {
      if (mCustomAttributes == null) {
        mCustomAttributes = new SimpleArrayMap<>();
      }
      mCustomAttributes.put(key, value);

      return (T) this;
    }

    void release() {
      mCustomAttributes = null;
    }
  }
}
