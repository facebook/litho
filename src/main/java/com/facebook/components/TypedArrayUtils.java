// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.content.res.TypedArray;
import android.util.TypedValue;

class TypedArrayUtils {
  private static final TypedValue sTmpTypedValue = new TypedValue();

  static boolean isColorAttribute(TypedArray a, int idx) {
    synchronized (sTmpTypedValue) {
      a.getValue(idx, sTmpTypedValue);
      return sTmpTypedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
          sTmpTypedValue.type <= TypedValue.TYPE_LAST_COLOR_INT;
    }
  }
}
