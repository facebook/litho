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

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

import com.facebook.yoga.YogaDirection;

/** This class hosts any extra mount data related to MountItem. */
public class LithoMountData {

  /** This mountItem represents the top-level root host (LithoView) which is always mounted. */
  static MountItem createRootHostMountItem(LithoView lithoView) {
    final ViewNodeInfo viewNodeInfo = new ViewNodeInfo();
    viewNodeInfo.setLayoutDirection(YogaDirection.INHERIT);
    LayoutOutput output =
        new LayoutOutput(
            null,
            viewNodeInfo,
            HostComponent.create(),
            lithoView.getPreviousMountBounds(),
            0,
            0,
            0,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            lithoView.getContext().getResources().getConfiguration().orientation,
            null);
    return new MountItem(lithoView, lithoView, LayoutOutput.create(output, null));
  }
}
