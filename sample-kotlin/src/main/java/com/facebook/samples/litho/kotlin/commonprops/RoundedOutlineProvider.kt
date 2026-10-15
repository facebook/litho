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

package com.facebook.samples.litho.kotlin.commonprops

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

class RoundedOutlineProvider(private val cornerRadius: Float) : ViewOutlineProvider() {
  override fun getOutline(view: View, outline: Outline) {
    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
  }
}
