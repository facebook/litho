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
