/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
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

    if (!EquivalenceUtils.equals(x.getAccessibilityRole(), y.getAccessibilityRole())) {
      return false;
    }

    if (x.getAlpha() != y.getAlpha()) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(x.getClickHandler(), y.getClickHandler())) {
      return false;
    }

    if (x.getClipToOutline() != y.getClipToOutline()) {
      return false;
    }

    if (x.getClipChildren() != y.getClipChildren()) {
      return false;
    }

    if (!EquivalenceUtils.equals(x.getContentDescription(), y.getContentDescription())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
        x.getDispatchPopulateAccessibilityEventHandler(),
        y.getDispatchPopulateAccessibilityEventHandler())) {
      return false;
    }

    if (x.getEnabledState() != y.getEnabledState()) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(x.getFocusChangeHandler(), y.getFocusChangeHandler())) {
      return false;
    }

    if (x.getFocusState() != y.getFocusState()) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
        x.getInterceptTouchHandler(), y.getInterceptTouchHandler())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(x.getLongClickHandler(), y.getLongClickHandler())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
        x.getOnInitializeAccessibilityEventHandler(),
        y.getOnInitializeAccessibilityEventHandler())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
        x.getOnInitializeAccessibilityNodeInfoHandler(),
        y.getOnInitializeAccessibilityNodeInfoHandler())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
        x.getOnPopulateAccessibilityEventHandler(), y.getOnPopulateAccessibilityEventHandler())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
        x.getOnPopulateAccessibilityNodeHandler(), y.getOnPopulateAccessibilityNodeHandler())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
        x.getOnRequestSendAccessibilityEventHandler(),
        y.getOnRequestSendAccessibilityEventHandler())) {
      return false;
    }

    if (!EquivalenceUtils.equals(x.getOutlineProvider(), y.getOutlineProvider())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
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

    if (!EquivalenceUtils.isEquivalentTo(
        x.getSendAccessibilityEventHandler(), y.getSendAccessibilityEventHandler())) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(
        x.getSendAccessibilityEventUncheckedHandler(),
        y.getSendAccessibilityEventUncheckedHandler())) {
      return false;
    }

    if (x.getShadowElevation() != y.getShadowElevation()) {
      return false;
    }

    if (x.getAmbientShadowColor() != y.getAmbientShadowColor()) {
      return false;
    }

    if (x.getSpotShadowColor() != y.getSpotShadowColor()) {
      return false;
    }

    if (!EquivalenceUtils.isEquivalentTo(x.getTouchHandler(), y.getTouchHandler())) {
      return false;
    }

    if (!EquivalenceUtils.equals(x.getViewTag(), y.getViewTag())) {
      return false;
    }

    if (!EquivalenceUtils.equals(x.getViewTags(), y.getViewTags())) {
      return false;
    }

    return true;
  }
}
