/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.displaylist;

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
