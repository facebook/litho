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

import javax.annotation.Nullable;

public class ComponentKeyUtils {
  private static final String DUPLICATE_MANUAL_KEY = "ComponentKeyUtils:DuplicateManualKey";
  private static final String NULL_PARENT_KEY = "ComponentKeyUtils:NullParentKey";

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

  /**
   * Generate a global key for the given component that is unique among all of this component's
   * children of the same type. If a manual key has been set on the child component using the .key()
   * method, return the manual key.
   *
   * <p>TODO: (T38237241) remove the usage of the key handler post the nested tree experiment
   *
   * @param parentComponent parent component within the layout context
   * @param childComponent child component with the parent context
   * @return a unique global key for this component relative to its siblings.
   */
  static String generateGlobalKey(
      final @Nullable ComponentContext parentContext,
      final @Nullable Component parentComponent,
      final Component childComponent) {
    final String key = childComponent.getKey();
    final String globalKey;

    if (parentComponent == null) {
      globalKey = key;
    } else {
      if (Component.getGlobalKey(parentContext, parentComponent) == null) {
        logParentHasNullGlobalKey(parentComponent, childComponent);

        globalKey = "null" + key;

      } else {
        final String childKey =
            getKeyWithSeparator(Component.getGlobalKey(parentContext, parentComponent), key);
        final int index;

        if (childComponent.hasManualKey()) {
          index =
              Component.getManualKeyUsagesCountAndIncrement(
                  parentContext, parentComponent, childKey);

          if (index != 0) {
            logDuplicateManualKeyWarning(childComponent, key);
          }

        } else {
          index =
              Component.getChildCountAndIncrement(parentContext, parentComponent, childComponent);
        }

        globalKey = getKeyForChildPosition(childKey, index);
      }
    }

    return globalKey;
  }

  private static void logParentHasNullGlobalKey(
      Component parentComponent, Component childComponent) {
    ComponentsReporter.emitMessage(
        ComponentsReporter.LogLevel.ERROR,
        NULL_PARENT_KEY,
        "Trying to generate parent-based key for component "
            + childComponent.getSimpleName()
            + " , but parent "
            + parentComponent.getSimpleName()
            + " has a null global key \"."
            + " This is most likely a configuration mistake,"
            + " check the value of ComponentsConfiguration.useGlobalKeys.");
  }

  private static void logDuplicateManualKeyWarning(Component component, String key) {
    ComponentsReporter.emitMessage(
        ComponentsReporter.LogLevel.WARNING,
        DUPLICATE_MANUAL_KEY,
        "The manual key "
            + key
            + " you are setting on this "
            + component.getSimpleName()
            + " is a duplicate and will be changed into a unique one. "
            + "This will result in unexpected behavior if you don't change it.");
  }
}
