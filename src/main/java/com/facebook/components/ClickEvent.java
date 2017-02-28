// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.View;

import com.facebook.components.annotations.Event;

/**
 * Components should implement an event of this type in order to receive Android click events. The
 * method is equivalent to the Android method {@link View.OnClickListener#onClick(View)}.
 * An example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(ClickEvent.class)
 * static void onClick(
 *     @FromEvent View view,
 *     @Param Param someParam,
 *     @Prop Prop someProp) {
 *   // Handle the click here.
 * }
 * </pre>
 */
@Event
public class ClickEvent {
  public View view;
}
