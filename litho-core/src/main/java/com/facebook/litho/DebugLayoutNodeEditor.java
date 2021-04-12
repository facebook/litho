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

import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaValue;

public class DebugLayoutNodeEditor {

  private final InternalNode mNode;

  public DebugLayoutNodeEditor(InternalNode node) {
    mNode = node;
  }

  public void setForegroundColor(int color) {
    mNode.foregroundColor(color);
  }

  public void setBackgroundColor(int color) {
    mNode.backgroundColor(color);
  }

  public void setRotation(float value) {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      nodeInfo.setRotation(value);
    }
  }

  public void setAlpha(float value) {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      nodeInfo.setAlpha(value);
    }
  }

  public void setScale(float value) {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      nodeInfo.setScale(value);
    }
  }

  public void setImportantForAccessibility(int importantForAccessibility) {
    mNode.importantForAccessibility(importantForAccessibility);
  }

  public void setFocusable(boolean focusable) {
    mNode.getOrCreateNodeInfo().setFocusable(focusable);
  }

  public void setContentDescription(CharSequence contentDescription) {
    mNode.getOrCreateNodeInfo().setContentDescription(contentDescription);
  }

  public void setLayoutDirection(YogaDirection yogaDirection) {
    mNode.getDebugLayoutEditor().layoutDirection(yogaDirection);
  }

  public void setFlexDirection(YogaFlexDirection direction) {
    mNode.flexDirection(direction);
  }

  public void setJustifyContent(YogaJustify yogaJustify) {
    mNode.justifyContent(yogaJustify);
  }

  public void setAlignItems(YogaAlign yogaAlign) {
    mNode.alignItems(yogaAlign);
  }

  public void setAlignSelf(YogaAlign yogaAlign) {
    mNode.getDebugLayoutEditor().alignSelf(yogaAlign);
  }

  public void setAlignContent(YogaAlign yogaAlign) {
    mNode.alignContent(yogaAlign);
  }

  public void setPositionType(YogaPositionType yogaPositionType) {
    mNode.getDebugLayoutEditor().positionType(yogaPositionType);
  }

  public void setFlexGrow(float value) {
    mNode.getDebugLayoutEditor().flexGrow(value);
  }

  public void setFlexShrink(float value) {
    mNode.getDebugLayoutEditor().flexShrink(value);
  }

  public void setFlexBasis(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().flexBasisAuto();
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().flexBasisPercent(value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().flexBasisPx((int) value.value);
        break;
    }
  }

  public void setWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().widthAuto();
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().widthPercent(value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().widthPx((int) value.value);
        break;
    }
  }

  public void setMinWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().minWidthPx(Integer.MIN_VALUE);
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().minWidthPercent(value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().minWidthPx((int) value.value);
        break;
    }
  }

  public void setMaxWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().maxWidthPx(Integer.MAX_VALUE);
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().maxWidthPercent(value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().maxWidthPx((int) value.value);
        break;
    }
  }

  public void setHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().heightAuto();
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().heightPercent(value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().heightPx((int) value.value);
        break;
    }
  }

  public void setMinHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().minHeightPx(Integer.MIN_VALUE);
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().minHeightPercent(value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().minHeightPx((int) value.value);
        break;
    }
  }

  public void setMaxHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().maxHeightPx(Integer.MAX_VALUE);
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().maxHeightPercent(value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().maxHeightPx((int) value.value);
        break;
    }
  }

  public void setAspectRatio(float aspectRatio) {
    mNode.getDebugLayoutEditor().aspectRatio(aspectRatio);
  }

  public void setMargin(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
        mNode.getDebugLayoutEditor().marginPx(edge, 0);
        break;
      case AUTO:
        mNode.getDebugLayoutEditor().marginAuto(edge);
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().marginPercent(edge, value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().marginPx(edge, (int) value.value);
        break;
    }
  }

  public void setPadding(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().paddingPx(edge, 0);
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().paddingPercent(edge, value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().paddingPx(edge, (int) value.value);
        break;
    }
  }

  public void setPosition(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.getDebugLayoutEditor().positionPercent(edge, YogaConstants.UNDEFINED);
        break;
      case PERCENT:
        mNode.getDebugLayoutEditor().positionPercent(edge, value.value);
        break;
      case POINT:
        mNode.getDebugLayoutEditor().positionPx(edge, (int) value.value);
        break;
    }
  }

  public void isReferenceBaseline(boolean isReferenceBaseline) {
    mNode.getDebugLayoutEditor().isReferenceBaseline(isReferenceBaseline);
  }

  public void setBorderWidth(YogaEdge edge, float value) {
    mNode.getDebugLayoutEditor().setBorderWidth(edge, (int) value);
  }
}
