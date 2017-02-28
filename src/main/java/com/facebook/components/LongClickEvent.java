// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.View;

import com.facebook.components.annotations.Event;

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
