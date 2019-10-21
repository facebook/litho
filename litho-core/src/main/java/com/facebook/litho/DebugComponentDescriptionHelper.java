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

import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import com.facebook.proguard.annotations.DoNotStrip;

/**
 * Describes {@link DebugComponent}s for use in testing and debugging. Note that {@link
 * com.facebook.litho.config.ComponentsConfiguration#isEndToEndTestRun} must be enabled in order for
 * this data to be collected.
 */
public class DebugComponentDescriptionHelper {

  /**
   * Appends a compact description of a {@link DebugComponent} for debugging purposes.
   *
   * @param left The left coordinate of the {@link DebugComponent}
   * @param top The top coordinate of the {@link DebugComponent}
   * @param debugComponent The {@link DebugComponent}
   * @param sb The {@link StringBuilder} to which the description is appended
   * @param embedded Whether the call is embedded in "adb dumpsys activity"
   */
  @DoNotStrip
  public static void addViewDescription(
      int left, int top, DebugComponent debugComponent, StringBuilder sb, boolean embedded) {
    sb.append("litho.");
    sb.append(debugComponent.getComponent().getSimpleName());

    sb.append('{');
    sb.append(Integer.toHexString(debugComponent.hashCode()));
    sb.append(' ');

    final LithoView lithoView = debugComponent.getLithoView();
    final DebugLayoutNode layout = debugComponent.getLayoutNode();
    sb.append(lithoView != null && lithoView.getVisibility() == View.VISIBLE ? "V" : ".");
    sb.append(layout != null && layout.getFocusable() ? "F" : ".");
    sb.append(lithoView != null && lithoView.isEnabled() ? "E" : ".");
    sb.append(".");
    sb.append(lithoView != null && lithoView.isHorizontalScrollBarEnabled() ? "H" : ".");
    sb.append(lithoView != null && lithoView.isVerticalScrollBarEnabled() ? "V" : ".");
    sb.append(layout != null && layout.getClickHandler() != null ? "C" : ".");
    sb.append(". .. ");

    final Rect bounds = debugComponent.getBounds();
    sb.append(left + bounds.left);
    sb.append(",");
    sb.append(top + bounds.top);
    sb.append("-");
    sb.append(left + bounds.right);
    sb.append(",");
    sb.append(top + bounds.bottom);

    final String testKey = debugComponent.getTestKey();
    if (testKey != null && !TextUtils.isEmpty(testKey)) {
      sb.append(String.format(" litho:id/%s", testKey.replace(' ', '_')));
    }

    final String textContent = debugComponent.getTextContent();
    if (textContent != null && !TextUtils.isEmpty(textContent)) {
      sb.append(String.format(" text=\"%s\"", textContent.replace("\n", "").replace("\"", "")));
    }

    if (!embedded && layout != null && layout.getClickHandler() != null) {
      sb.append(" [clickable]");
    }

    sb.append('}');
  }
}
