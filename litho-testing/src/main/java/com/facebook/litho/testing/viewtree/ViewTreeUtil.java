/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.viewtree;

import android.content.res.Resources.NotFoundException;
import org.robolectric.RuntimeEnvironment;

/**
 * Utility methods for {@link ViewTreeAssert}.
 */
public class ViewTreeUtil {

  /** @return the resource name or "<undefined>" */
  public static String getResourceName(final int resourceId) {
    try {
      return RuntimeEnvironment
          .application
          .getResources()
          .getResourceEntryName(resourceId);
    } catch (final NotFoundException notFoundException) {
      return "<undefined>";
    }
  }
}
