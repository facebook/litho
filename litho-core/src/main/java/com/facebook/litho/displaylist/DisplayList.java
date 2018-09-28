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

package com.facebook.litho.displaylist;

import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * A DisplayList is a cache for drawing commands. Calling {@link DisplayList#start(int, int)}
 * returns a {@link Canvas} that will be used to record a set of drawing commands. these drawing
 * commands can later be re-executed calling {@link DisplayList#draw(Canvas)}.
 * Recording a DisplayList allows for faster re-drawing of {@link android.view.View}s and
 * {@link android.graphics.drawable.Drawable}s since drawing a DisplayList instead of executing the
 * onDraw/draw methods avoids translating the {@link Canvas} commands into OpenGL calls.
 */
public class DisplayList {
  private final PlatformDisplayList mDisplayListImpl;
  private boolean mStarted;

  private DisplayList(PlatformDisplayList displayListImpl) {
    mDisplayListImpl = displayListImpl;
  }

  /**
   * Creates a new DisplayList for a specific Context with a Debug name.
   */
  @Nullable
  public static DisplayList createDisplayList(String name) {
    if (ComponentsConfiguration.forceNotToCacheDisplayLists) {
      throw new RuntimeException(
          "DisplayLists are not supposed to be used, this should never be called");
    }

    final PlatformDisplayList platformDisplayList;
    switch (Build.VERSION.SDK_INT) {
      case 25: // Nougat MR1
      case 24: // Nougat
        platformDisplayList = DisplayListNougat.createDisplayList(name);
        break;
      case Build.VERSION_CODES.M:
        platformDisplayList = DisplayListMarshmallow.createDisplayList(name);
        break;
      case Build.VERSION_CODES.LOLLIPOP_MR1:
      case Build.VERSION_CODES.LOLLIPOP:
        platformDisplayList = DisplayListLollipop.createDisplayList(name);
        break;
      case Build.VERSION_CODES.KITKAT_WATCH:
      case Build.VERSION_CODES.KITKAT:
      case Build.VERSION_CODES.JELLY_BEAN_MR2:
        platformDisplayList = DisplayListJBMR2.createDisplayList(name);
        break;
      case Build.VERSION_CODES.JELLY_BEAN_MR1:
      case Build.VERSION_CODES.JELLY_BEAN:
        platformDisplayList = DisplayListJB.createDisplayList(name);
        break;
      default:
        platformDisplayList = null;
        break;
    }

    if (platformDisplayList == null) {
      return null;
    }

    return new DisplayList(platformDisplayList);
  }

  /**
   * Starts recording a DisplayList with size of width-height.
   * @return a {@link Canvas} on which drawing commands can be recorded
   * @throws DisplayListException if the creation of the Canvas failed
   */
  public Canvas start(int width, int height) throws DisplayListException {
    if (mStarted) {
      throw new DisplayListException(
          new IllegalStateException("Can't start a DisplayList that is already started"));
    }

    final Canvas canvas =  mDisplayListImpl.start(width, height);
    mStarted = true;

    return canvas;
  }

  /**
   * Notifies this DisplayList that the list of drawing commands is complete.
   * @param canvas the canvas that was returned from {@link DisplayList#start(int, int)}
   * @throws DisplayListException if the creation of GL commands failed
   */
  public void end(Canvas canvas) throws DisplayListException {
    if (!mStarted) {
      throw new DisplayListException(
          new IllegalStateException("Can't end a DisplayList that is not started"));
    }

    mStarted = false;
    mDisplayListImpl.end(canvas);
  }

  /**
   * Clears all previous drawing commands from this DisplayList.
   * @throws DisplayListException if clearing the DisplayList failed
   */
  public void clear() throws DisplayListException {
    mDisplayListImpl.clear();
  }

  /**
   * Draws the content of this DisplayList on a Canvas.
   * @param canvas the canvas on which to draw the content of this DisplayList
   * @throws DisplayListException if the drawing failed
   */
  public void draw(Canvas canvas) throws DisplayListException {
    mDisplayListImpl.draw(canvas);
  }

  /**
   * Set the bounds in which this DisplayList will draw. The bounds are relative to the canvas
   * coordinates for which {@link DisplayList#draw(Canvas)} is called.
   * @throws DisplayListException if setting the bouds failed
   */
  public void setBounds(int left, int top, int right, int bottom) throws DisplayListException {
    mDisplayListImpl.setBounds(left, top, right, bottom);
  }

  public boolean isValid() {
    return mDisplayListImpl.isValid();
  }

  void print(Canvas canvas) throws DisplayListException {
    mDisplayListImpl.print(canvas);
  }
}
