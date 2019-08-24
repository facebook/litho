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

package com.facebook.litho;

import android.content.res.TypedArray;
import android.util.TypedValue;

class TypedArrayUtils {
  private static final TypedValue sTmpTypedValue = new TypedValue();

  static boolean isColorAttribute(TypedArray a, int idx) {
    synchronized (sTmpTypedValue) {
      a.getValue(idx, sTmpTypedValue);
      return sTmpTypedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
          && sTmpTypedValue.type <= TypedValue.TYPE_LAST_COLOR_INT;
    }
  }
}
