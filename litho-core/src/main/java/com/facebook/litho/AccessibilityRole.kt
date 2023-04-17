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

import androidx.annotation.StringDef

/**
 * A list of all roles used for accessibility. There is no API for these in Android yet, so roles
 * are currently based off of the class being rendered. These roles are defined by Google's TalkBack
 * screen reader, and this list should be kept up to date with their implementation.
 *
 * @see
 *   [https://github.com/google/talkback/blob/master/utils/src/main/java/Role.java](https://github.com/google/talkback/blob/master/utils/src/main/java/Role.java)
 */
object AccessibilityRole {

  const val NONE = ""
  const val BUTTON = "android.widget.Button"
  const val CHECK_BOX = "android.widget.CompoundButton"
  const val DROP_DOWN_LIST = "android.widget.Spinner"
  const val EDIT_TEXT = "android.widget.EditText"
  const val GRID = "android.widget.GridView"
  const val IMAGE = "android.widget.ImageView"
  const val IMAGE_BUTTON = "android.widget.ImageView"
  const val LIST = "android.widget.AbsListView"
  const val PAGER = "androidx.viewpager.widget.ViewPager"
  const val RADIO_BUTTON = "android.widget.RadioButton"
  const val SEEK_CONTROL = "android.widget.SeekBar"
  const val SWITCH = "android.widget.Switch"
  const val TAB_BAR = "android.widget.TabWidget"
  const val TOGGLE_BUTTON = "android.widget.ToggleButton"
  const val VIEW_GROUP = "android.view.ViewGroup"
  const val WEB_VIEW = "android.webkit.WebView"
  const val CHECKED_TEXT_VIEW = "android.widget.CheckedTextView"
  const val PROGRESS_BAR = "android.widget.ProgressBar"
  const val ACTION_BAR_TAB = "android.app.ActionBar\$Tab"
  const val DRAWER_LAYOUT = "androidx.drawerlayout.widget.DrawerLayout"
  const val SLIDING_DRAWER = "android.widget.SlidingDrawer"
  const val ICON_MENU = "com.android.internal.view.menu.IconMenuView"
  const val TOAST = "android.widget.Toast\$TN"
  const val DATE_PICKER_DIALOG = "android.app.DatePickerDialog"
  const val TIME_PICKER_DIALOG = "android.app.TimePickerDialog"
  const val DATE_PICKER = "android.widget.DatePicker"
  const val TIME_PICKER = "android.widget.TimePicker"
  const val NUMBER_PICKER = "android.widget.NumberPicker"
  const val SCROLL_VIEW = "android.widget.ScrollView"
  const val HORIZONTAL_SCROLL_VIEW = "android.widget.HorizontalScrollView"
  const val KEYBOARD_KEY = "android.inputmethodservice.Keyboard\$Key"

  @Retention(AnnotationRetention.SOURCE)
  @StringDef(
      NONE,
      BUTTON,
      CHECK_BOX,
      DROP_DOWN_LIST,
      EDIT_TEXT,
      GRID,
      IMAGE,
      IMAGE_BUTTON,
      LIST,
      PAGER,
      RADIO_BUTTON,
      SEEK_CONTROL,
      SWITCH,
      TAB_BAR,
      TOGGLE_BUTTON,
      VIEW_GROUP,
      WEB_VIEW,
      CHECKED_TEXT_VIEW,
      PROGRESS_BAR,
      ACTION_BAR_TAB,
      DRAWER_LAYOUT,
      SLIDING_DRAWER,
      ICON_MENU,
      TOAST,
      DATE_PICKER_DIALOG,
      TIME_PICKER_DIALOG,
      DATE_PICKER,
      TIME_PICKER,
      NUMBER_PICKER,
      SCROLL_VIEW,
      HORIZONTAL_SCROLL_VIEW,
      KEYBOARD_KEY)
  annotation class AccessibilityRoleType
}
