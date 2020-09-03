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

import android.graphics.Rect;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaValue;

public class LayoutStateUtils {

  public static Rect resolveMargins(YogaNode yogaNode, boolean isRTL) {
    final Rect margins = new Rect(0, 0, 0, 0);

    final YogaValue all = yogaNode.getMargin(YogaEdge.ALL);
    if (!YogaConstants.isUndefined(all)) {
      margins.set((int) all.value, (int) all.value, (int) all.value, (int) all.value);
    }

    final YogaValue horizontal = yogaNode.getMargin(YogaEdge.HORIZONTAL);
    if (!YogaConstants.isUndefined(horizontal)) {
      margins.left = (int) horizontal.value;
      margins.right = (int) horizontal.value;
    }

    final YogaValue vertical = yogaNode.getMargin(YogaEdge.VERTICAL);
    if (!YogaConstants.isUndefined(vertical)) {
      margins.top = (int) vertical.value;
      margins.bottom = (int) vertical.value;
    }

    final YogaValue start = yogaNode.getMargin(YogaEdge.START);
    if (!YogaConstants.isUndefined(start)) {
      if (!isRTL) {
        margins.left = (int) start.value;
      } else {
        margins.right = (int) start.value;
      }
    }

    final YogaValue end = yogaNode.getMargin(YogaEdge.END);
    if (!YogaConstants.isUndefined(end)) {
      if (!isRTL) {
        margins.right = (int) end.value;
      } else {
        margins.left = (int) end.value;
      }
    }

    final YogaValue left = yogaNode.getMargin(YogaEdge.LEFT);
    if (!YogaConstants.isUndefined(left)) {
      margins.left = (int) left.value;
    }

    final YogaValue right = yogaNode.getMargin(YogaEdge.RIGHT);
    if (!YogaConstants.isUndefined(right)) {
      margins.right = (int) right.value;
    }

    final YogaValue top = yogaNode.getMargin(YogaEdge.TOP);
    if (!YogaConstants.isUndefined(top)) {
      margins.top = (int) top.value;
    }

    final YogaValue bottom = yogaNode.getMargin(YogaEdge.BOTTOM);
    if (!YogaConstants.isUndefined(bottom)) {
      margins.bottom = (int) bottom.value;
    }

    return margins;
  }
}
