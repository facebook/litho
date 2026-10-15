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

import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;
import androidx.customview.widget.ExploreByTouchHelper;

/**
 * Null-tolerant Java boundary for {@link ExploreByTouchHelper}.
 *
 * <p>This class must remain Java: under strict JSpecify, declaring these parameters nullable in a
 * Kotlin override fails to compile with "overrides nothing."
 */
abstract class NullTolerantExploreByTouchHelper extends ExploreByTouchHelper {

  protected NullTolerantExploreByTouchHelper(View host) {
    super(host);
  }

  @Override
  public final boolean onRequestSendAccessibilityEvent(
      @Nullable ViewGroup host, @Nullable View child, @Nullable AccessibilityEvent event) {
    if (host == null || child == null || event == null) {
      return false;
    }
    return onRequestSendAccessibilityEventSafe(host, child, event);
  }

  protected abstract boolean onRequestSendAccessibilityEventSafe(
      ViewGroup host, View child, AccessibilityEvent event);

  protected final boolean superOnRequestSendAccessibilityEvent(
      ViewGroup host, View child, AccessibilityEvent event) {
    return super.onRequestSendAccessibilityEvent(host, child, event);
  }
}
