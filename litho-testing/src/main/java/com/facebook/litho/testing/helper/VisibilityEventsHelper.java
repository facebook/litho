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

package com.facebook.litho.testing.helper;

import com.facebook.litho.ComponentTree;
import com.facebook.litho.FocusedVisibleEvent;
import com.facebook.litho.FullImpressionVisibleEvent;
import com.facebook.litho.InvisibleEvent;
import com.facebook.litho.UnfocusedVisibleEvent;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.testing.Whitebox;

/**
 * Allows calling visibility events manually which is useful in automated tests
 *
 * <p>Since this requires a bunch of private APIs, and we haven't reached a conclusion of whether
 * they should be public we are making aggressive use of reflection through Whitebox to call them
 */
public class VisibilityEventsHelper {

  /**
   * Tries to trigger the requested visibility event on the given component tree on the first
   * matching visibility output
   *
   * @param componentTree the component tree to search
   * @param visibilityEventType the event to trigger
   * @return true if a matching object to trigger the event on was found
   */
  public static boolean triggerVisibilityEvent(
      ComponentTree componentTree, Class<?> visibilityEventType) {
    try {
      final Object layoutState = getGetMainThreadLayoutState(componentTree);
      for (int i = 0, size = getVisibilityOutputCount(layoutState); i < size; i++) {
        final Object visibilityOutput = getVisibilityOutputAt(layoutState, i);
        if (visibilityEventType == VisibleEvent.class
            && getEventHandler(visibilityOutput, "Visible") != null) {
          dispatch(getEventHandler(visibilityOutput, "Visible"), "Visible");
          return true;
        } else if (visibilityEventType == InvisibleEvent.class
            && getEventHandler(visibilityOutput, "Invisible") != null) {
          dispatch(getEventHandler(visibilityOutput, "Invisible"), "Invisible");
          return true;
        } else if (visibilityEventType == FocusedVisibleEvent.class
            && getEventHandler(visibilityOutput, "Focused") != null) {
          dispatch(getEventHandler(visibilityOutput, "Focused"), "Focused");
          return true;
        } else if (visibilityEventType == UnfocusedVisibleEvent.class
            && getEventHandler(visibilityOutput, "Unfocused") != null) {
          dispatch(getEventHandler(visibilityOutput, "Unfocused"), "Unfocused");
          return true;
        } else if (visibilityEventType == FullImpressionVisibleEvent.class
            && getEventHandler(visibilityOutput, "FullImpression") != null) {
          dispatch(getEventHandler(visibilityOutput, "FullImpression"), "FullImpression");
          return true;
        }
      }
      return false;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static Object getGetMainThreadLayoutState(ComponentTree componentTree) {
    return Whitebox.invokeMethod(componentTree, "getMainThreadLayoutState");
  }

  private static int getVisibilityOutputCount(Object layoutState) {
    return (int) Whitebox.invokeMethod(layoutState, "getVisibilityOutputCount");
  }

  private static Object getVisibilityOutputAt(Object layoutState, int i) {
    return Whitebox.invokeMethod(layoutState, "getVisibilityOutputAt", i);
  }

  private static Object getEventHandler(Object layoutState, String name) {
    return Whitebox.invokeMethod(layoutState, "get" + name + "EventHandler");
  }

  private static void dispatch(Object eventHandler, String name) throws ClassNotFoundException {
    Whitebox.invokeMethod(
        Class.forName("com.facebook.rendercore.visibility.VisibilityUtils"),
        "dispatchOn" + name,
        eventHandler);
  }
}
