// Copyright 2004-present Facebook. All Rights Reserved.

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
