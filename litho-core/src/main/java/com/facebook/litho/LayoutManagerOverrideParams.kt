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

/**
 * LayoutParams that override the LayoutManager.
 *
 * If you set LayoutParams on a LithoView that implements this interface, the view will completely
 * ignore the layout specs given to it by its LayoutManager and use these specs instead. To use, set
 * the LayoutParams height and width to [android.view.ViewGroup.LayoutParams.WRAP_CONTENT] and then
 * provide a width and height measure spec though this interface.
 *
 * This is helpful for implementing [android.view.View.MeasureSpec.AT_MOST] support since Android
 * LayoutManagers don't support an AT_MOST concept as part of
 * [android.view.ViewGroup.LayoutParams]'s special values.
 */
interface LayoutManagerOverrideParams {
  val widthMeasureSpec: Int
  val heightMeasureSpec: Int
}
