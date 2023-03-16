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

package com.facebook.litho

import com.facebook.rendercore.primitives.utils.equals
import com.facebook.rendercore.primitives.utils.isEquivalentTo

object NodeInfoUtils {
  @JvmStatic
  fun isEquivalentTo(x: NodeInfo?, y: NodeInfo?): Boolean {
    if (x === y) {
      return true
    }
    if (x == null || y == null) {
      return false
    }
    if (x.flags != y.flags) {
      return false
    }
    if (!equals(x.accessibilityRole, y.accessibilityRole)) {
      return false
    }
    if (x.alpha != y.alpha) {
      return false
    }
    if (!isEquivalentTo(x.clickHandler, y.clickHandler)) {
      return false
    }
    if (x.clipToOutline != y.clipToOutline) {
      return false
    }
    if (x.clipChildren != y.clipChildren) {
      return false
    }
    if (!equals(x.contentDescription, y.contentDescription)) {
      return false
    }
    if (!isEquivalentTo(
        x.dispatchPopulateAccessibilityEventHandler, y.dispatchPopulateAccessibilityEventHandler)) {
      return false
    }
    if (x.enabledState != y.enabledState) {
      return false
    }
    if (!isEquivalentTo(x.focusChangeHandler, y.focusChangeHandler)) {
      return false
    }
    if (x.focusState != y.focusState) {
      return false
    }
    if (!isEquivalentTo(x.interceptTouchHandler, y.interceptTouchHandler)) {
      return false
    }
    if (!isEquivalentTo(x.longClickHandler, y.longClickHandler)) {
      return false
    }
    if (!isEquivalentTo(
        x.onInitializeAccessibilityEventHandler, y.onInitializeAccessibilityEventHandler)) {
      return false
    }
    if (!isEquivalentTo(
        x.onInitializeAccessibilityNodeInfoHandler, y.onInitializeAccessibilityNodeInfoHandler)) {
      return false
    }
    if (!isEquivalentTo(
        x.onPopulateAccessibilityEventHandler, y.onPopulateAccessibilityEventHandler)) {
      return false
    }
    if (!isEquivalentTo(
        x.onPopulateAccessibilityNodeHandler, y.onPopulateAccessibilityNodeHandler)) {
      return false
    }
    if (!isEquivalentTo(
        x.onRequestSendAccessibilityEventHandler, y.onRequestSendAccessibilityEventHandler)) {
      return false
    }
    if (!equals(x.outlineProvider, y.outlineProvider)) {
      return false
    }
    if (!isEquivalentTo(x.performAccessibilityActionHandler, y.performAccessibilityActionHandler)) {
      return false
    }
    if (x.rotation != y.rotation) {
      return false
    }
    if (x.scale != y.scale) {
      return false
    }
    if (x.selectedState != y.selectedState) {
      return false
    }
    if (!isEquivalentTo(x.sendAccessibilityEventHandler, y.sendAccessibilityEventHandler)) {
      return false
    }
    if (!isEquivalentTo(
        x.sendAccessibilityEventUncheckedHandler, y.sendAccessibilityEventUncheckedHandler)) {
      return false
    }
    if (x.shadowElevation != y.shadowElevation) {
      return false
    }
    if (x.ambientShadowColor != y.ambientShadowColor) {
      return false
    }
    if (x.spotShadowColor != y.spotShadowColor) {
      return false
    }
    if (!isEquivalentTo(x.touchHandler, y.touchHandler)) {
      return false
    }
    if (!equals(x.viewTag, y.viewTag)) {
      return false
    }

    if (!equals(x.viewId, y.viewId)) {
      return false
    }

    return equals(x.viewTags, y.viewTags)
  }
}
