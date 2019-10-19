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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.EventHandler;
import com.facebook.litho.RenderCompleteEvent;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import javax.annotation.Nullable;

public interface RenderInfo {

  boolean isSticky();

  int getSpanSize();

  boolean isFullSpan();

  @Nullable
  Object getCustomAttribute(String key);

  boolean rendersComponent();

  Component getComponent();

  @Nullable
  ComponentsLogger getComponentsLogger();

  @Nullable
  String getLogTag();

  @Nullable
  EventHandler<RenderCompleteEvent> getRenderCompleteEventHandler();

  boolean rendersView();

  ViewBinder getViewBinder();

  ViewCreator getViewCreator();

  boolean hasCustomViewType();

  int getViewType();

  void addDebugInfo(String key, Object value);

  @Nullable
  Object getDebugInfo(String key);

  String getName();

  /**
   * Set viewType of current {@link RenderInfo} if it was created through {@link
   * ViewRenderInfo#create()} and a custom viewType was not set, or otherwise it will throw {@link
   * UnsupportedOperationException}.
   */
  void setViewType(int viewType);
}
