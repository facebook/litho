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

import android.annotation.SuppressLint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LithoKeyTestingUtil {

  /**
   * Map each component type to a list of the global keys of the components of that type in the
   * order in which they are added to the hierarchy.
   */
  @SuppressLint("RestrictedApi")
  public static Map<String, List<String>> getGlobalKeysInfo(LithoView lithoView) {
    final Map<String, List<String>> globalKeys = new HashMap<>();
    addKeysForNode(
        lithoView.getComponentTree().getCommittedLayoutState().getLayoutRoot().getInternalNode(),
        globalKeys);

    return globalKeys;
  }

  public static void addKeysForNode(InternalNode node, Map<String, List<String>> globalKeys) {
    for (int i = 0, size = node.getComponents().size(); i < size; i++) {
      final Component component = node.getComponents().get(i);
      final String globalKey = node.getComponentKeys().get(i);
      final String componentType = component.getSimpleName();

      List<String> sameTypeKeys = globalKeys.get(componentType);
      if (sameTypeKeys == null) {
        sameTypeKeys = new ArrayList<>();
        globalKeys.put(componentType, sameTypeKeys);
      }
      sameTypeKeys.add(globalKey);
    }

    for (int i = 0, size = node.getChildCount(); i < size; i++) {
      addKeysForNode(node.getChildAt(i), globalKeys);
    }
  }
}
