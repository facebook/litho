// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.displaylist;

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
