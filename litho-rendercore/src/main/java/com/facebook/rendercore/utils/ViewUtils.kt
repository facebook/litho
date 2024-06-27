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

package com.facebook.rendercore.utils

import android.os.Build
import android.view.View

@Suppress("DEPRECATION")
fun View.clearCommonViewListeners() {
  onFocusChangeListener = null
  setOnClickListener(null)
  setOnLongClickListener(null)
  setOnCreateContextMenuListener(null)
  setOnKeyListener(null)
  setOnTouchListener(null)
  setOnHoverListener(null)
  setOnGenericMotionListener(null)
  setOnDragListener(null)
  setOnSystemUiVisibilityChangeListener(null)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    setOnApplyWindowInsetsListener(null)
  }
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    setOnScrollChangeListener(null)
    setOnContextClickListener(null)
  }
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    setOnCapturedPointerListener(null)
  }
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    setWindowInsetsAnimationCallback(null)
  }
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    setScrollCaptureCallback(null)
    setOnReceiveContentListener(null, null)
  }
}
