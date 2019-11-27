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

package com.facebook.litho;

import java.util.List;

/**
 * A lightweight representation of a layout node, used to cache measurements between two Layout tree
 * calculations.
 */
public interface DiffNode extends Cloneable {

  int UNSPECIFIED = -1;

  int getChildCount();

  DiffNode getChildAt(int i);

  Component getComponent();

  void setComponent(Component component);

  float getLastMeasuredWidth();

  void setLastMeasuredWidth(float lastMeasuredWidth);

  float getLastMeasuredHeight();

  void setLastMeasuredHeight(float lastMeasuredHeight);

  int getLastWidthSpec();

  int getLastHeightSpec();

  void setLastWidthSpec(int widthSpec);

  void setLastHeightSpec(int heightSpec);

  List<DiffNode> getChildren();

  void addChild(DiffNode node);

  LayoutOutput getContent();

  void setContent(LayoutOutput content);

  VisibilityOutput getVisibilityOutput();

  void setVisibilityOutput(VisibilityOutput visibilityOutput);

  LayoutOutput getBackgroundOutput();

  void setBackground(LayoutOutput background);

  LayoutOutput getForegroundOutput();

  void setForeground(LayoutOutput foreground);

  LayoutOutput getBorder();

  void setBorder(LayoutOutput border);

  LayoutOutput getHost();

  void setHost(LayoutOutput host);
}
