/*
 * Copyright 2014-present Facebook, Inc.
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

import android.view.KeyEvent;
import com.facebook.litho.annotations.Event;

/**
 * Event that corresponds to an underlying android.widget.EditText#onKeyPreIme(). If you handled the
 * event, return true. If you want to allow the event to be handled by the next receiver, return
 * false.
 */
@Event(returnType = boolean.class)
public class KeyPreImeEvent {
  public int keyCode;
  public KeyEvent keyEvent;
}
