/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.EventHandler;
import com.facebook.litho.RenderCompleteEvent;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Keeps the list item information that will allow the framework to understand how to render it.
 *
 * <p>SpanSize will be defaulted to 1. It is the information that is required to calculate how much
 * of the SpanCount the component should occupy in a Grid layout.
 *
 * <p>IsSticky will be defaulted to false. It determines if the component should be a sticky header
 * or not
 *
 * <p>IsFullSpan will be defaulted to false. It is the information that determines if the component
 * should occupy all of the SpanCount in a StaggeredGrid layout.
 */
public abstract class BaseRenderInfo implements RenderInfo {

  private static final String IS_STICKY = "is_sticky";
  private static final String SPAN_SIZE = "span_size";
  private static final String IS_FULL_SPAN = "is_full_span";

  private final @Nullable Map<String, Object> mCustomAttributes;
  private @Nullable Map<String, Object> mDebugInfo;

  protected BaseRenderInfo(Builder builder) {
    mCustomAttributes = builder.mCustomAttributes;
    mDebugInfo = builder.mDebugInfo;
  }

  @Override
  public boolean isSticky() {
    if (mCustomAttributes == null || !mCustomAttributes.containsKey(IS_STICKY)) {
      return false;
    }

    return (boolean) mCustomAttributes.get(IS_STICKY);
  }

  @Override
  public int getSpanSize() {
    if (mCustomAttributes == null || !mCustomAttributes.containsKey(SPAN_SIZE)) {
      return 1;
    }

    return (int) mCustomAttributes.get(SPAN_SIZE);
  }

  @Override
  public boolean isFullSpan() {
    if (mCustomAttributes == null || !mCustomAttributes.containsKey(IS_FULL_SPAN)) {
      return false;
    }

    return (boolean) mCustomAttributes.get(IS_FULL_SPAN);
  }

  @Override
  public @Nullable Object getCustomAttribute(String key) {
    return mCustomAttributes == null ? null : mCustomAttributes.get(key);
  }

  /**
   * @return true, if {@link RenderInfo} was created through {@link ComponentRenderInfo#create()},
   *     or false otherwise. This should be queried before accessing {@link #getComponent() } from
   *     {@link RenderInfo} type.
   */
  @Override
  public boolean rendersComponent() {
    return false;
  }

  /**
   * @return Valid {@link Component} if {@link RenderInfo} was created through {@link
   *     ComponentRenderInfo#create()}, otherwise it will throw {@link
   *     UnsupportedOperationException}. If this method is accessed from {@link RenderInfo} type,
   *     {@link #rendersComponent()} should be queried first before accessing.
   */
  @Override
  public Component getComponent() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return Valid {@link EventHandler<RenderCompleteEvent>} if {@link RenderInfo} was created
   *     through {@link ComponentRenderInfo#create()}, otherwise it will throw {@link
   *     UnsupportedOperationException}.
   */
  @Override
  @Nullable
  public EventHandler<RenderCompleteEvent> getRenderCompleteEventHandler() {
    // TODO(T28620590): Support RenderCompleteEvent handler for ViewRenderInfo
    throw new UnsupportedOperationException();
  }

  /**
   * @return Optional {@link ComponentsLogger} if {@link RenderInfo} was created through {@link
   *     ComponentRenderInfo#create()}, null otherwise
   */
  @Override
  @Nullable
  public ComponentsLogger getComponentsLogger() {
    return null;
  }

  /**
   * @return Optional identifier for logging if {@link RenderInfo} was created through {@link
   *     ComponentRenderInfo#create()}, null otherwise
   */
  @Nullable
  @Override
  public String getLogTag() {
    return null;
  }

  /**
   * @return true, if {@link RenderInfo} was created through {@link ViewRenderInfo#create()}, or
   *     false otherwise. This should be queried before accessing view related methods, such as
   *     {@link #getViewBinder()}, {@link #getViewCreator()}, {@link #getViewType()} and {@link
   *     #setViewType(int)} from {@link RenderInfo} type.
   */
  @Override
  public boolean rendersView() {
    return false;
  }

  /**
   * @return Valid {@link ViewBinder} if {@link RenderInfo} was created through {@link
   *     ViewRenderInfo#create()}, or otherwise it will throw {@link UnsupportedOperationException}.
   *     If this method is accessed from {@link RenderInfo} type, {@link #rendersView()} should be
   *     queried first before accessing.
   */
  @Override
  public ViewBinder getViewBinder() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return Valid {@link ViewCreator} if {@link RenderInfo} was created through {@link
   *     ViewRenderInfo#create()}, or otherwise it will throw {@link UnsupportedOperationException}.
   *     If this method is accessed from {@link RenderInfo} type, {@link #rendersView()} should be
   *     queried first before accessing.
   */
  @Override
  public ViewCreator getViewCreator() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return true, if a custom viewType was set for this {@link RenderInfo} and it was created
   *     through {@link ViewRenderInfo#create()}, or false otherwise.
   */
  @Override
  public boolean hasCustomViewType() {
    return false;
  }

  /**
   * @return viewType of current {@link RenderInfo} if it was created through {@link
   *     ViewRenderInfo#create()} or otherwise it will throw {@link UnsupportedOperationException}.
   *     If this method is accessed from {@link RenderInfo} type, {@link #rendersView()} should be
   *     queried first before accessing.
   */
  @Override
  public int getViewType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addDebugInfo(String key, Object value) {
    if (mDebugInfo == null) {
      mDebugInfo = Collections.synchronizedMap(new HashMap<String, Object>());
    }

    mDebugInfo.put(key, value);
  }

  @Override
  public @Nullable Object getDebugInfo(String key) {
    if (mDebugInfo == null) {
      return null;
    }

    return mDebugInfo.get(key);
  }

  /**
   * Set viewType of current {@link RenderInfo} if it was created through {@link
   * ViewRenderInfo#create()} and a custom viewType was not set, or otherwise it will throw {@link
   * UnsupportedOperationException}.
   */
  public void setViewType(int viewType) {
    throw new UnsupportedOperationException();
  }

  public abstract static class Builder<T> {

    private @Nullable Map<String, Object> mCustomAttributes;
    private @Nullable Map<String, Object> mDebugInfo;

    public T isSticky(boolean isSticky) {
      return customAttribute(IS_STICKY, isSticky);
    }

    public T spanSize(int spanSize) {
      return customAttribute(SPAN_SIZE, spanSize);
    }

    public T isFullSpan(boolean isFullSpan) {
      return customAttribute(IS_FULL_SPAN, isFullSpan);
    }

    public T customAttribute(String key, Object value) {
      if (mCustomAttributes == null) {
        mCustomAttributes = Collections.synchronizedMap(new HashMap<String, Object>());
      }
      mCustomAttributes.put(key, value);

      return (T) this;
    }

    public T debugInfo(String key, Object value) {
      if (mDebugInfo == null) {
        mDebugInfo = Collections.synchronizedMap(new HashMap<String, Object>());
      }

      mDebugInfo.put(key, value);

      return (T) this;
    }
  }
}
