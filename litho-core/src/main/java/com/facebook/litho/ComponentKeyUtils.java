/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

public class ComponentKeyUtils {
  private static final String SEPARATOR = ",";

  /**
   * @param keyParts a list of objects that will be concatenated to form another component's key
   * @return a key formed by concatenating the key parts delimited by a separator.
   */
  public static String getKeyWithSeparator(Object... keyParts) {
    final StringBuilder sb = new StringBuilder();
    sb.append(keyParts[0]);
    for (int i = 1; i < keyParts.length; i++) {
      sb.append(SEPARATOR).append(keyParts[i]);
    }

    return sb.toString();
  }

  public static String getKeyWithSeparator(String parentGlobalKey, String key) {
    int parentLength = parentGlobalKey.length();
    int keyLength = key.length();
    return new StringBuilder(parentLength + keyLength + 1)
        .append(parentGlobalKey)
        .append(SEPARATOR)
        .append(key)
        .toString();
  }

  public static String getKeyForChildPosition(String currentKey, int index) {
    if (index == 0) {
      return currentKey;
    }

    // Index will almost always be under 3 digits
    final StringBuilder sb = new StringBuilder(currentKey.length() + 4);
    sb.append(currentKey).append('!').append(index);

    return sb.toString();
  }
}
