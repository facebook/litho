/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.support.annotation.Nullable;
import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * Helper class in charge of dumping the component hierarchy related to a provided {@link
 * ComponentContext}
 *
 * <p>This class is intended to be used only in debug environments due to limitations with the
 * preservation of the view hierarchy
 *
 * <p>This will not provide a reliable representation of the hierarchy on non debug build of the app
 */
public class ComponentTreeDumpingHelper {

  /** Dumps the tree related to the provided component context */
  @Nullable
  public static String dumpContextTree(ComponentContext componentContext) {
    if (!ComponentsConfiguration.isDebugModeEnabled) {
      return "Dumping of the component" + " tree is not support on non-internal builds";
    }

    // Getting the base of the tree
    ComponentTree componentTree = componentContext.getComponentTree();
    DebugComponent rootComponent = DebugComponent.getRootInstance(componentTree);
    if (rootComponent == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    logComponent(rootComponent, 0, sb);

    return sb.toString();
  }

  /** Logs the content of a single debug component instance */
  private static void logComponent(
      @Nullable DebugComponent debugComponent, int depth, StringBuilder sb) {
    if (debugComponent == null) {
      return;
    }

    // Logging the component name
    sb.append(debugComponent.getComponent().getSimpleName());

    // Description of the component status (Visible, Has Click Handler)
    sb.append('{');

    final LithoView lithoView = debugComponent.getLithoView();
    final DebugLayoutNode layout = debugComponent.getLayoutNode();
    sb.append(lithoView != null && lithoView.getVisibility() == View.VISIBLE ? "V" : "H");
    if (layout != null && layout.getClickHandler() != null) {
      sb.append(" [clickable]");
    }

    sb.append('}');

    for (DebugComponent child : debugComponent.getChildComponents()) {
      sb.append("\n");
      for (int i = 0; i <= depth; i++) {
        sb.append("  ");
      }
      logComponent(child, depth + 1, sb);
    }
  }
}
