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

import android.view.View;

/**
 * Defines a tooltip that can be passed to the ComponentTree to be anchored to a component. The
 * framework takes care of finding the position where the tooltip needs to anchored.
 */
public interface DeprecatedLithoTooltip {

  /**
   * Display the content view in a popup window anchored to the bottom-left corner of the anchor
   * view offset by the specified x and y coordinates.
   */
  void showBottomLeft(View anchor, int x, int y);
}
