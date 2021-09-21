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
  public static Map<String, List<ScopedComponentInfo>> getScopedComponentInfos(
      LithoView lithoView) {
    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos = new HashMap<>();
    addScopedComponentInfoForNode(
        lithoView.getComponentTree().getCommittedLayoutState().getLayoutRoot().getInternalNode(),
        scopedComponentInfos);

    return scopedComponentInfos;
  }

  public static void addScopedComponentInfoForNode(
      InternalNode node, Map<String, List<ScopedComponentInfo>> scopedComponentInfos) {
    for (int i = 0, size = node.getComponents().size(); i < size; i++) {
      final Component component = node.getComponents().get(i);
      final String componentType = component.getSimpleName();

      List<ScopedComponentInfo> sameTypeKeys = scopedComponentInfos.get(componentType);
      if (sameTypeKeys == null) {
        sameTypeKeys = new ArrayList<>();
        scopedComponentInfos.put(componentType, sameTypeKeys);
      }
      sameTypeKeys.add(node.getScopedComponentInfos().get(i));
    }

    for (int i = 0, size = node.getChildCount(); i < size; i++) {
      addScopedComponentInfoForNode(node.getChildAt(i), scopedComponentInfos);
    }
  }
}
