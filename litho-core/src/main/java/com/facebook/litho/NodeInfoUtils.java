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

import androidx.annotation.Nullable;

public class NodeInfoUtils {

  private NodeInfoUtils() {}

  public static boolean isEquivalentTo(@Nullable NodeInfo x, @Nullable NodeInfo y) {
    if (x == y) {
      return true;
    }

    if (x == null || y == null) {
      return false;
    }

    if (x.getFlags() != y.getFlags()) {
      return false;
    }

    if (!CommonUtils.equals(x.getAccessibilityRole(), y.getAccessibilityRole())) {
      return false;
    }

    if (x.getAlpha() != y.getAlpha()) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(x.getClickHandler(), y.getClickHandler())) {
      return false;
    }

    if (x.getClipToOutline() != y.getClipToOutline()) {
      return false;
    }

    if (x.getClipChildren() != y.getClipChildren()) {
      return false;
    }

    if (!CommonUtils.equals(x.getContentDescription(), y.getContentDescription())) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(
        x.getDispatchPopulateAccessibilityEventHandler(),
        y.getDispatchPopulateAccessibilityEventHandler())) {
      return false;
    }

    if (x.getEnabledState() != y.getEnabledState()) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(x.getFocusChangeHandler(), y.getFocusChangeHandler())) {
      return false;
    }

    if (x.getFocusState() != y.getFocusState()) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(x.getInterceptTouchHandler(), y.getInterceptTouchHandler())) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(x.getLongClickHandler(), y.getLongClickHandler())) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(
        x.getOnInitializeAccessibilityEventHandler(),
        y.getOnInitializeAccessibilityEventHandler())) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(
        x.getOnInitializeAccessibilityNodeInfoHandler(),
        y.getOnInitializeAccessibilityNodeInfoHandler())) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(
        x.getOnPopulateAccessibilityEventHandler(), y.getOnPopulateAccessibilityEventHandler())) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(
        x.getOnRequestSendAccessibilityEventHandler(),
        y.getOnRequestSendAccessibilityEventHandler())) {
      return false;
    }

    if (!CommonUtils.equals(x.getOutlineProvider(), y.getOutlineProvider())) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(
        x.getPerformAccessibilityActionHandler(), y.getPerformAccessibilityActionHandler())) {
      return false;
    }

    if (x.getRotation() != y.getRotation()) {
      return false;
    }

    if (x.getScale() != y.getScale()) {
      return false;
    }

    if (x.getSelectedState() != y.getSelectedState()) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(
        x.getSendAccessibilityEventHandler(), y.getSendAccessibilityEventHandler())) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(
        x.getSendAccessibilityEventUncheckedHandler(),
        y.getSendAccessibilityEventUncheckedHandler())) {
      return false;
    }

    if (x.getShadowElevation() != y.getShadowElevation()) {
      return false;
    }

    if (!CommonUtils.isEquivalentTo(x.getTouchHandler(), y.getTouchHandler())) {
      return false;
    }

    if (!CommonUtils.equals(x.getViewTag(), y.getViewTag())) {
      return false;
    }

    if (!CommonUtils.equals(x.getViewTags(), y.getViewTags())) {
      return false;
    }

    return true;
  }
}
