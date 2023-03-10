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

import android.view.View
import androidx.annotation.IntDef

/** Enumerates all the valid valid for [Component.Builder#layerType(int, Paint)] */
@IntDef(
    LayerType.LAYER_TYPE_NOT_SET,
    LayerType.LAYER_TYPE_NONE,
    LayerType.LAYER_TYPE_SOFTWARE,
    LayerType.LAYER_TYPE_HARDWARE)
annotation class LayerType {
  companion object {
    const val LAYER_TYPE_NOT_SET = -1
    const val LAYER_TYPE_NONE = View.LAYER_TYPE_NONE
    const val LAYER_TYPE_SOFTWARE = View.LAYER_TYPE_SOFTWARE
    const val LAYER_TYPE_HARDWARE = View.LAYER_TYPE_HARDWARE
  }
}
