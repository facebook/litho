/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

public class ComponentKeyUtils {

  /**
   * @param keyParts a list of objects that will be concatenated to form another component's key
   * @return a key formed by concatenating the key parts delimited by a separator.
   */
  public static String getKeyWithSeparator(Object... keyParts) {
    final StringBuilder sb = new StringBuilder();
    sb.append(keyParts[0]);
    for (int i = 1; i < keyParts.length; i++) {
      sb.append(',').append(keyParts[i]);
    }

    return sb.toString();
  }

  public static String getKeyWithSeparator(String parentGlobalKey, String key) {
    final StringBuilder sb = new StringBuilder();
    sb.append(parentGlobalKey).append(',').append(key);

    return sb.toString();
  }

  public static String getKeyForChildPosition(String currentKey, int index) {
    final StringBuilder sb = new StringBuilder();
    sb.append(currentKey).append('!').append(index);

    return sb.toString();
  }
}
