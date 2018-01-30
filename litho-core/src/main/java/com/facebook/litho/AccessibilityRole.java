/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.support.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A list of all roles used for accessibility. There is no API for these in Android yet, so roles
 * are currently based off of the class being rendered. These roles are defined by Google's TalkBack
 * screen reader, and this list should be kept up to date with their implementation.
 *
 * @see <a
 *     href="https://github.com/google/talkback/blob/master/src/main/java/com/android/utils/Role.java">
 *     https://github.com/google/talkback/blob/master/src/main/java/com/android/utils/Role.java </a>
 */
public class AccessibilityRole {

  private AccessibilityRole() {}

  @Retention(RetentionPolicy.SOURCE)
  @StringDef({
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
  })
  public @interface AccessibilityRoleType {}

  public static final String BUTTON = "android.widget.Button";
  public static final String CHECK_BOX = "android.widget.CompoundButton";
  public static final String DROP_DOWN_LIST = "android.widget.Spinner";
  public static final String EDIT_TEXT = "android.widget.EditText";
  public static final String GRID = "android.widget.GridView";
  public static final String IMAGE = "android.widget.ImageView";
  public static final String IMAGE_BUTTON = "android.widget.ImageView";
  public static final String LIST = "android.widget.AbsListView";
  public static final String PAGER = "android.support.v4.view.ViewPager";
  public static final String RADIO_BUTTON = "android.widget.RadioButton";
  public static final String SEEK_CONTROL = "android.widget.SeekBar";
  public static final String SWITCH = "android.widget.Switch";
  public static final String TAB_BAR = "android.widget.TabWidget";
  public static final String TOGGLE_BUTTON = "android.widget.ToggleButton";
  public static final String VIEW_GROUP = "android.view.ViewGroup";
  public static final String WEB_VIEW = "android.webkit.WebView";
}
