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
   * Align the text to the start of the view i.e. {@link
   * android.view.View#TEXT_ALIGNMENT_VIEW_END}.
   */
  LAYOUT_END,

  /** Align the text to the left, ignoring the text or locale preferences for LTR/RTL. */
  LEFT,

  /** Align the text to the right, ignoring the text or locale preferences for LTR/RTL. */
  RIGHT,
}