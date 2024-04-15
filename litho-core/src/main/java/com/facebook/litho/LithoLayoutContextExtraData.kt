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

import com.facebook.litho.LithoLayoutContextExtraData.LithoLayoutExtraData
import com.facebook.rendercore.LayoutContextExtraData
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaNode

class LithoLayoutContextExtraData(private val yogaNode: YogaNode) :
    LayoutContextExtraData<LithoLayoutExtraData?> {

  override val extraLayoutData: LithoLayoutExtraData
    get() = LithoLayoutExtraData(yogaNode)

  class LithoLayoutExtraData(private val yogaNode: YogaNode) {
    val layoutDirection: YogaDirection = YogaDirection.RTL // yogaNode.getLayoutDirection();
  }
}
