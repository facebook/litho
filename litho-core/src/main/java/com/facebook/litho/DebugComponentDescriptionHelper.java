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
   * @param debugComponent The {@link DebugComponent}
   * @param sb The {@link StringBuilder} to which the description is appended
   * @param leftOffset Offset of the parent component relative to litho view
   * @param topOffset Offset of the parent component relative to litho view
   * @param embedded Whether the call is embedded in "adb dumpsys activity"
   */
  @DoNotStrip
  public static void addViewDescription(
      DebugComponent debugComponent,
      StringBuilder sb,
      int leftOffset,
      int topOffset,
      boolean embedded) {
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

    // using position relative to litho view host to handle relative position issues
    // the offset is for the parent component to create proper relative coordinates
    final Rect bounds = debugComponent.getBoundsInLithoView();
    sb.append(bounds.left - leftOffset);
    sb.append(",");
    sb.append(bounds.top - topOffset);
    sb.append("-");
    sb.append(bounds.right - leftOffset);
    sb.append(",");
    sb.append(bounds.bottom - topOffset);

    final String testKey = debugComponent.getTestKey();
    if (testKey != null && !TextUtils.isEmpty(testKey)) {
      sb.append(String.format(" litho:id/%s", testKey.replace(' ', '_')));
    }

    String textContent = debugComponent.getTextContent();
    if (textContent != null && !TextUtils.isEmpty(textContent)) {
      textContent = textContent.replace("\n", "").replace("\"", "");
      if (textContent.length() > 200) {
        textContent = textContent.substring(0, 200) + "...";
      }
      sb.append(String.format(" text=\"%s\"", textContent));
    }

    if (!embedded && layout != null && layout.getClickHandler() != null) {
      sb.append(" [clickable]");
    }

    sb.append('}');
  }
}
