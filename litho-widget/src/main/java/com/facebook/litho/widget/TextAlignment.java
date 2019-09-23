/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.widget;

/**
 * Enumeration of text alignment values. These values differ from {@link
 * android.text.Layout.Alignment} and View.TEXT_ALIGNMENT_* because this full list of values is not
 * supported by either implementation.
 */
public enum TextAlignment {

  /**
   * Align the text to the start of the paragraph i.e. {@link
   * android.view.View#TEXT_ALIGNMENT_TEXT_START} or {@link
   * android.text.Layout.Alignment#ALIGN_NORMAL}.
   */
  TEXT_START,

  /**
   * Align the text to the end of the paragraph i.e. {@link
   * android.view.View#TEXT_ALIGNMENT_TEXT_END} or {@link
   * android.text.Layout.Alignment#ALIGN_OPPOSITE}/.
   */
  TEXT_END,

  /** Align the text to the center. */
  CENTER,

  /**
   * Align the text to the start of the view i.e. {@link
   * android.view.View#TEXT_ALIGNMENT_VIEW_START}.
   */
  LAYOUT_START,

  /**
   * Align the text to the start of the view i.e. {@link android.view.View#TEXT_ALIGNMENT_VIEW_END}.
   */
  LAYOUT_END,

  /** Align the text to the left, ignoring the text or locale preferences for LTR/RTL. */
  LEFT,

  /** Align the text to the right, ignoring the text or locale preferences for LTR/RTL. */
  RIGHT,
}
