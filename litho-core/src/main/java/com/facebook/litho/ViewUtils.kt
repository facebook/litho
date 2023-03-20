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

@file:JvmName("ViewUtils")

package com.facebook.litho

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import com.facebook.litho.drawable.ComparableColorDrawable

fun setViewForeground(view: View, foreground: Drawable?) {
  foreground?.let {
    check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ("MountState has a ViewNodeInfo with foreground however " +
          "the current Android version doesn't support foreground on Views")
    }
    view.foreground = it
  }
}

fun setViewForeground(view: View, foregroundColor: Int) {
  check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    ("MountState has a ViewNodeInfo with foreground however " +
        "the current Android version doesn't support foreground on Views")
  }
  view.foreground = ComparableColorDrawable.create(foregroundColor)
}
