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

// Use this package so that we can access package-private fields of TextInputLayout.
package com.google.android.material.textfield;

import android.content.Context;
import android.view.View;

public class MountableTextInputLayout extends TextInputLayout {

  private View.OnFocusChangeListener onFocusChangeListener = null;

  public MountableTextInputLayout(Context context) {
    super(context, null);
  }

  // Delegate the focus listener to the EditText
  @Override
  public void setOnFocusChangeListener(View.OnFocusChangeListener l) {
    onFocusChangeListener = l;
    if (editText == null) {
      return;
    }
    editText.setOnFocusChangeListener(l);
  }

  @Override
  public View.OnFocusChangeListener getOnFocusChangeListener() {
    return onFocusChangeListener;
  }

  @Override
  public void setOnClickListener(View.OnClickListener l) {
    if (editText != null) {
      editText.setOnClickListener(l);
    }
  }
}
