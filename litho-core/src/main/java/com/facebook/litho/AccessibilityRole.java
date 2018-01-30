/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

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

  public static final String NONE = null;
  public static final String BUTTON = android.widget.Button.class.getName();
  public static final String CHECK_BOX = android.widget.CompoundButton.class.getName();
  public static final String DROP_DOWN_LIST = android.widget.Spinner.class.getName();
  public static final String EDIT_TEXT = android.widget.EditText.class.getName();
  public static final String GRID = android.widget.GridView.class.getName();
  public static final String IMAGE = android.widget.ImageView.class.getName();
  public static final String IMAGE_BUTTON = android.widget.ImageView.class.getName();
  public static final String LIST = android.widget.AbsListView.class.getName();
  // Hardcoded so that clients don't need to import all ViewPager's classes + methods if they
  // don't want to.
  public static final String PAGER = "android.support.v4.view.ViewPager";
  public static final String RADIO_BUTTON = android.widget.RadioButton.class.getName();
  public static final String SEEK_CONTROL = android.widget.SeekBar.class.getName();
  public static final String SWITCH = android.widget.Switch.class.getName();
  public static final String TAB_BAR = android.widget.TabWidget.class.getName();
  public static final String TOGGLE_BUTTON = android.widget.ToggleButton.class.getName();
  public static final String VIEW_GROUP = android.view.ViewGroup.class.getName();
  public static final String WEB_VIEW = android.webkit.WebView.class.getName();
}
