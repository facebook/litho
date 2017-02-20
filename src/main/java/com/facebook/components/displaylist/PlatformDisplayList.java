// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.displaylist;

import android.graphics.Canvas;

/**
 * An interface to provide a platform implementation to the methods exposed by {@link DisplayList}
 */
interface PlatformDisplayList {

  Canvas start(int width, int height) throws DisplayListException;

  void end(Canvas canvas) throws DisplayListException;

  void clear() throws DisplayListException;

  void print(Canvas canvas) throws DisplayListException;

  void draw(Canvas canvas) throws DisplayListException;

  void setBounds(int left, int top, int right, int bottom) throws DisplayListException;

  boolean isValid();
}
