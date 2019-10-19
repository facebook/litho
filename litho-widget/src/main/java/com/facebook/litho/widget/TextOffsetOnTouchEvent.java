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

import com.facebook.litho.annotations.Event;

/**
 * Text component should implement an event of this type in order to receive callback on what was
 * the text offset when text was touched initially. This event is fired only when motion event
 * action is ACTION_DOWN.
 *
 * An example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(TextOffsetOnTouchEvent.class)
 * static void textOffsetOnTouchEvent(
 *     ComponentContext c,
 *     @FromEvent CharSequence text,
 *     @FromEvent int textOffset) {
 *     ...
 * }
 * </pre>
 */
@Event
public class TextOffsetOnTouchEvent {
  public CharSequence text;
  public int textOffset;
}
