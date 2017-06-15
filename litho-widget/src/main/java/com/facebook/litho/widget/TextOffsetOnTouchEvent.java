/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
