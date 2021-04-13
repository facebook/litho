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

import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNode;

public final class NullLayoutResult implements LithoLayoutResult {

  public static final NullLayoutResult INSTANCE = new NullLayoutResult();

  private NullLayoutResult() {}

  @Override
  public InternalNode getInternalNode() {
    return ComponentContext.NULL_LAYOUT;
  }

  @Override
  public boolean shouldDrawBorders() {
    return false;
  }

  @Override
  public int getLayoutBorder(YogaEdge edge) {
    return 0;
  }

  @Override
  public int getTouchExpansionBottom() {
    return 0;
  }

  @Override
  public int getTouchExpansionLeft() {
    return 0;
  }

  @Override
  public int getTouchExpansionRight() {
    return 0;
  }

  @Override
  public int getTouchExpansionTop() {
    return 0;
  }

  @Override
  public YogaDirection recursivelyResolveLayoutDirection() {
    return YogaDirection.INHERIT;
  }

  @Override
  public float getLastMeasuredHeight() {
    return 0;
  }

  @Override
  public float getLastMeasuredWidth() {
    return 0;
  }

  @Override
  public int getLastHeightSpec() {
    return 0;
  }

  @Override
  public int getLastWidthSpec() {
    return 0;
  }

  @Override
  public void setLastWidthSpec(int widthSpec) {}

  @Override
  public void setLastHeightSpec(int heightSpec) {}

  @Override
  public void setLastMeasuredHeight(float lastMeasuredHeight) {}

  @Override
  public void setLastMeasuredWidth(float lastMeasuredWidth) {}

  @Override
  public void addChild(LithoLayoutResult child) {}

  @Override
  public LithoLayoutResult getChildAt(int i) {
    return null;
  }

  @Override
  public int getChildCount() {
    return 0;
  }

  @Override
  public @Nullable LithoLayoutResult getParent() {
    return null;
  }

  @Override
  public void setParent(LithoLayoutResult parent) {}

  @Override
  public YogaNode getYogaNode() {
    return null;
  }

  @Override
  public int getX() {
    return 0;
  }

  @Override
  public int getY() {
    return 0;
  }

  @Override
  public int getWidth() {
    return 0;
  }

  @Override
  public int getHeight() {
    return 0;
  }

  @Override
  public int getPaddingTop() {
    return 0;
  }

  @Override
  public int getPaddingRight() {
    return 0;
  }

  @Override
  public int getPaddingBottom() {
    return 0;
  }

  @Override
  public int getPaddingLeft() {
    return 0;
  }

  @Override
  public boolean isPaddingSet() {
    return false;
  }

  @Nullable
  @Override
  public Drawable getBackground() {
    return null;
  }

  @Override
  public YogaDirection getResolvedLayoutDirection() {
    return YogaDirection.INHERIT;
  }
}
