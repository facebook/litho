/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.view.View;

import com.facebook.litho.annotations.Event;

/**
 * Components should implement an event of this type in order to receive Android long click events.
 * The method is equivalent to the Android method {@link View.OnLongClickListener#onLongClick(View)}
 * - implementations should return true if they consumed the long click and false otherwise.
 * An example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(LongClickEvent.class)
 * static boolean onLongClick(
 *     @FromEvent View view,
 *     @Param Param someParam
 *     @Prop Prop someProp) {
 *   if (shouldHandleLongClick(someParam, someProp)) {
 *     handleLongClick(view);
 *     return true;
 *   }
 *
 *   return false;
 * }
 * </pre>
 */
@Event(returnType = boolean.class)
public class LongClickEvent {
  public View view;
}
