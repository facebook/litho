package com.facebook.litho.widget;

import android.text.TextWatcher;
import android.widget.EditText;

/** Allows for creating a TextWatcher with an EditText reference. */
public abstract class TextWatcherWithEditText implements TextWatcher {

  private EditText editText;

  public EditText getEditText() {
    return editText;
  }

  public void setEditText(EditText editText) {
    this.editText = editText;
  }
}