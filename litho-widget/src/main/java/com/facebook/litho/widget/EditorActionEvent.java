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

import android.view.KeyEvent;
import android.widget.TextView;
import com.facebook.litho.annotations.Event;

/**
 * Event sent by EditText when the return key is pressed or the IME signals an 'action'. Return true
 * if the handler consumed the event.
 */
@Event(returnType = boolean.class)
public class EditorActionEvent {
  public TextView view;
  public int actionId;
  public KeyEvent event;
}
