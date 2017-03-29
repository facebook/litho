/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.res.TypedArray;
import android.util.TypedValue;

class TypedArrayUtils {
  private static final TypedValue sTmpTypedValue = new TypedValue();

  static boolean isColorAttribute(TypedArray a, int idx) {
    synchronized (sTmpTypedValue) {
      a.getValue(idx, sTmpTypedValue);
      return sTmpTypedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
          sTmpTypedValue.type <= TypedValue.TYPE_LAST_COLOR_INT;
