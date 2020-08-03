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

package com.facebook.rendercore.visibility;

import android.graphics.Rect;
import android.view.View;

/**
 * This is a temporary wrapper around VisibilityOutput, the plan is to make VisibilityOutput
 * implement this in a future diff, so we don't have two abstractions for the same thing. Right now
 * it only supports vertical incremental handling.
 */
public interface IncrementalModuleItem {
  String getId();

  Rect getBounds();

  /**
   * The minimum point from top of this item which would need to be visible so that this items
   * qualifies as "in range".
   */
  float getEnterRangeTop();

  /**
   * The minimum point from bottom of this item which would need to be visible so that this item
   * qualifies as "in range".
   */
  float getEnterRangeBottom();

  void onEnterVisibleRange();

  void onExitVisibleRange();

  /**
   * We might need to do some setup when the LithoView is available. Ex: for focused events we need
   * to know the size of the parent LithoView to decide how much the item needs to be visible to be
   * eligible.
   */
  void onLithoViewAvailable(View view);
}
