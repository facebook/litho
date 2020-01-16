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

package com.facebook.litho.widget;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.facebook.litho.annotations.Event;

/**
 * Event that corresponds to an underlying
 * android.widget.EditText#onCreateInputConnection(EditorInfo editorInfo). If you handled the event,
 * return either the input connection passed through the parameter or a Wrapper using
 * androidx.core.view.inputmethod.InputConnectionCompat#createWrapper.
 */
@Event(returnType = InputConnection.class)
public class InputConnectionEvent {
  public InputConnection inputConnection;
  public EditorInfo editorInfo;
}
