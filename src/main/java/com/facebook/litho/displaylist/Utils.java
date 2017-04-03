/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.displaylist;

import java.lang.reflect.Method;

class Utils {

  static Object safeInvoke (
      Method method,
      Object receiver,
      Object... args) throws DisplayListException {
    try {
      return method.invoke(receiver, args);
    } catch (Exception e) {
      throw new DisplayListException(e);
    }
  }
}
