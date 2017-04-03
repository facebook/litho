/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.displaylist;

import android.app.Activity;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.Nullable;

/**
 * A DisplayList is a cache for drawing commands. Calling {@link DisplayList#start(int, int)}
 * returns a {@link Canvas} that will be used to record a set of drawing commands. these drawing
 * commands can later be re-executed calling {@link DisplayList#draw(Canvas)}.
 * Recording a DisplayList allows for faster re-drawing of {@link android.view.View}s and
 * {@link android.graphics.drawable.Drawable}s since drawing a DisplayList instead of executing the
 * onDraw/draw methods avoids translating the {@link Canvas} commands into OpenGL calls.
 */
public class DisplayList {
  private PlatformDisplayList mDisplayListImpl;
  private boolean mStarted;

  private DisplayList(PlatformDisplayList displayListImpl) {
    mDisplayListImpl = displayListImpl;
  }

  /**
   * Creates a new DisplayList for a specific Context with a Debug name.
   */
  @Nullable
  public static DisplayList createDisplayList(String name) {
    final PlatformDisplayList platformDisplayList;

    if (Build.VERSION.SDK_INT >= 24) {
      platformDisplayList = DisplayListNougat.createDisplayList(name);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      platformDisplayList = DisplayListMarshmallow.createDisplayList(name);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      platformDisplayList = DisplayListLollipop.createDisplayList(name);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      platformDisplayList = DisplayListJBMR2.createDisplayList(name);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      platformDisplayList = DisplayListJB.createDisplayList(name);
    } else {
      platformDisplayList = null;
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
