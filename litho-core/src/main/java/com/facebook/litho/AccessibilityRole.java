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
public enum AccessibilityRole {
  NONE(null),
  BUTTON(android.widget.Button.class.getName()),
  CHECK_BOX(android.widget.CompoundButton.class.getName()),
  DROP_DOWN_LIST(android.widget.Spinner.class.getName()),
  EDIT_TEXT(android.widget.EditText.class.getName()),
  GRID(android.widget.GridView.class.getName()),
  IMAGE(android.widget.ImageView.class.getName()),
  IMAGE_BUTTON(android.widget.ImageView.class.getName()),
  LIST(android.widget.AbsListView.class.getName()),
  // Hardcoded so that clients don't need to import all ViewPager's classes + methods if they
  // don't want to.
  PAGER("android.support.v4.view.ViewPager"),
  RADIO_BUTTON(android.widget.RadioButton.class.getName()),
  SEEK_CONTROL(android.widget.SeekBar.class.getName()),
  SWITCH(android.widget.Switch.class.getName()),
  TAB_BAR(android.widget.TabWidget.class.getName()),
  TOGGLE_BUTTON(android.widget.ToggleButton.class.getName()),
  VIEW_GROUP(android.view.ViewGroup.class.getName()),
  WEB_VIEW(android.webkit.WebView.class.getName());

  private final CharSequence mValue;

  AccessibilityRole(CharSequence type) {
    mValue = type;
  }

  public CharSequence getValue() {
    return mValue;
  }
}
