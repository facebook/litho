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

import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.EventHandler;
import com.facebook.litho.RenderCompleteEvent;
import com.facebook.litho.TreeProps;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;

/**
 * A wrapper around {@link RenderInfo} that also stores TreeProps used for rendering the component.
 */
public class TreePropsWrappedRenderInfo implements RenderInfo {
  private final RenderInfo mRenderInfo;
  private final TreeProps mTreeProps;

  public TreePropsWrappedRenderInfo(
      @Nullable RenderInfo renderInfo, @Nullable TreeProps treeProps) {
    mRenderInfo = renderInfo == null ? ComponentRenderInfo.createEmpty() : renderInfo;
    mTreeProps = treeProps;
  }

  @Override
  public Component getComponent() {
    return mRenderInfo.getComponent();
  }

  @Override
  @Nullable
  public EventHandler<RenderCompleteEvent> getRenderCompleteEventHandler() {
    return mRenderInfo.getRenderCompleteEventHandler();
  }

  @Override
  public boolean rendersComponent() {
    return mRenderInfo.rendersComponent();
  }

  @Nullable
  @Override
  public ComponentsLogger getComponentsLogger() {
    return mRenderInfo.getComponentsLogger();
  }

  @Nullable
  @Override
  public String getLogTag() {
    return mRenderInfo.getLogTag();
  }

  @Override
  public String getName() {
    return mRenderInfo.getName();
  }

  @Override
  public boolean isSticky() {
    return mRenderInfo.isSticky();
  }

  @Override
  public int getSpanSize() {
    return mRenderInfo.getSpanSize();
  }

  @Override
  public boolean isFullSpan() {
    return mRenderInfo.isFullSpan();
  }

  @Override
  @Nullable
  public Object getCustomAttribute(String key) {
    return mRenderInfo.getCustomAttribute(key);
  }

  @Override
  public void addCustomAttribute(String key, Object value) {
    mRenderInfo.addCustomAttribute(key, value);
  }

  /**
   * @return true, if {@link RenderInfo} was created through {@link ViewRenderInfo#create()}, or
   *     false otherwise. This should be queried before accessing view related methods, such as
   *     {@link #getViewBinder()}, {@link #getViewCreator()}, {@link #getViewType()} and {@link
   *     #setViewType(int)} from {@link RenderInfo} type.
   */
  @Override
  public boolean rendersView() {
    return mRenderInfo.rendersView();
  }

  /**
   * @return Valid {@link ViewBinder} if {@link RenderInfo} was created through {@link
   *     ViewRenderInfo#create()}, or otherwise it will throw {@link UnsupportedOperationException}.
   *     If this method is accessed from {@link RenderInfo} type, {@link #rendersView()} should be
   *     queried first before accessing.
   */
  @Override
  public ViewBinder getViewBinder() {
    return mRenderInfo.getViewBinder();
  }

  /**
   * @return Valid {@link ViewCreator} if {@link RenderInfo} was created through {@link
   *     ViewRenderInfo#create()}, or otherwise it will throw {@link UnsupportedOperationException}.
   *     If this method is accessed from {@link RenderInfo} type, {@link #rendersView()} should be
   *     queried first before accessing.
   */
  @Override
  public ViewCreator getViewCreator() {
    return mRenderInfo.getViewCreator();
  }

  /**
   * @return true, if a custom viewType was set for this {@link RenderInfo} and it was created
   *     through {@link ViewRenderInfo#create()}, or false otherwise.
   */
  @Override
  public boolean hasCustomViewType() {
    return mRenderInfo.hasCustomViewType();
  }

  /**
   * @return viewType of current {@link RenderInfo} if it was created through {@link
   *     ViewRenderInfo#create()} or otherwise it will throw {@link UnsupportedOperationException}.
   *     If this method is accessed from {@link RenderInfo} type, {@link #rendersView()} should be
   *     queried first before accessing.
   */
  @Override
  public int getViewType() {
    return mRenderInfo.getViewType();
  }

  @Override
  public void addDebugInfo(String key, Object value) {
    mRenderInfo.addDebugInfo(key, value);
  }

  @Override
  @javax.annotation.Nullable
  public Object getDebugInfo(String key) {
    return mRenderInfo.getDebugInfo(key);
  }

  /**
   * Set viewType of current {@link RenderInfo} if it was created through {@link
   * ViewRenderInfo#create()} and a custom viewType was not set, or otherwise it will throw {@link
   * UnsupportedOperationException}.
   */
  public void setViewType(int viewType) {
    mRenderInfo.setViewType(viewType);
  }

  public TreeProps getTreeProps() {
    return mTreeProps;
  }
}
