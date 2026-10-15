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

// Use this package so that we can access package-private fields of TextInputLayout.
package com.google.android.material.textfield

import android.content.Context
import android.view.View

open class MountableTextInputLayout(context: Context) : TextInputLayout(context, null) {

  private var onFocusChangeListener: View.OnFocusChangeListener? = null

  // Delegate the focus listener to the EditText
  override fun setOnFocusChangeListener(l: View.OnFocusChangeListener?) {
    onFocusChangeListener = l
    editText?.setOnFocusChangeListener(l)
  }

  override fun getOnFocusChangeListener(): View.OnFocusChangeListener? {
    return onFocusChangeListener
  }

  override fun setOnClickListener(l: View.OnClickListener?) {
    editText?.setOnClickListener(l)
  }
}
