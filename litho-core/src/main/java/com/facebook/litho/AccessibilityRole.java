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

import androidx.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A list of all roles used for accessibility. There is no API for these in Android yet, so roles
 * are currently based off of the class being rendered. These roles are defined by Google's TalkBack
 * screen reader, and this list should be kept up to date with their implementation.
 *
 * @see <a
 *     href="https://github.com/google/talkback/blob/master/utils/src/main/java/Role.java">https://github.com/google/talkback/blob/master/utils/src/main/java/Role.java</a>
 */
public class AccessibilityRole {

  private AccessibilityRole() {}

  @Retention(RetentionPolicy.SOURCE)
  @StringDef({
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
    KEYBOARD_KEY,
  })
  public @interface AccessibilityRoleType {}

  public static final String NONE = "";
  public static final String BUTTON = "android.widget.Button";
  public static final String CHECK_BOX = "android.widget.CompoundButton";
  public static final String DROP_DOWN_LIST = "android.widget.Spinner";
  public static final String EDIT_TEXT = "android.widget.EditText";
  public static final String GRID = "android.widget.GridView";
  public static final String IMAGE = "android.widget.ImageView";
  public static final String IMAGE_BUTTON = "android.widget.ImageView";
  public static final String LIST = "android.widget.AbsListView";
  public static final String PAGER = "androidx.viewpager.widget.ViewPager";
  public static final String RADIO_BUTTON = "android.widget.RadioButton";
  public static final String SEEK_CONTROL = "android.widget.SeekBar";
  public static final String SWITCH = "android.widget.Switch";
  public static final String TAB_BAR = "android.widget.TabWidget";
  public static final String TOGGLE_BUTTON = "android.widget.ToggleButton";
  public static final String VIEW_GROUP = "android.view.ViewGroup";
  public static final String WEB_VIEW = "android.webkit.WebView";
  public static final String CHECKED_TEXT_VIEW = "android.widget.CheckedTextView";
  public static final String PROGRESS_BAR = "android.widget.ProgressBar";
  public static final String ACTION_BAR_TAB = "android.app.ActionBar$Tab";
  public static final String DRAWER_LAYOUT = "androidx.drawerlayout.widget.DrawerLayout";
  public static final String SLIDING_DRAWER = "android.widget.SlidingDrawer";
  public static final String ICON_MENU = "com.android.internal.view.menu.IconMenuView";
  public static final String TOAST = "android.widget.Toast$TN";
  public static final String DATE_PICKER_DIALOG = "android.app.DatePickerDialog";
  public static final String TIME_PICKER_DIALOG = "android.app.TimePickerDialog";
  public static final String DATE_PICKER = "android.widget.DatePicker";
  public static final String TIME_PICKER = "android.widget.TimePicker";
  public static final String NUMBER_PICKER = "android.widget.NumberPicker";
  public static final String SCROLL_VIEW = "android.widget.ScrollView";
  public static final String HORIZONTAL_SCROLL_VIEW = "android.widget.HorizontalScrollView";
  public static final String KEYBOARD_KEY = "android.inputmethodservice.Keyboard$Key";
}
