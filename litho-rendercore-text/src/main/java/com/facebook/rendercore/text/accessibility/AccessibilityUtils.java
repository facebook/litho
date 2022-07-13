/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore.text.accessibility;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.facebook.R;

public class AccessibilityUtils {

  private static final String ACCESSIBILITY_ROLE_BUTTON = "Button";
  private static final String ACCESSIBILITY_ROLE_IMAGE = "Image";
  private static final String ACCESSIBILITY_ROLE_IMAGE_BUTTON = "Image Button";
  private static final String ACCESSIBILITY_ROLE_HEADER = "Header";
  private static final String ACCESSIBILITY_ROLE_SELECTED_BUTTON = "Selected Button";
  private static final String ACCESSIBILITY_ROLE_TAB_WIDGET = "Tab Bar";
  private static final String ACCESSIBILITY_ROLE_LINK = "Link";

  private static final String ACCESSIBILITY_CLASS_BUTTON = "android.widget.Button";
  private static final String ACCESSIBILITY_CLASS_IMAGE = "android.widget.ImageView";
  private static final String ACCESSIBILITY_CLASS_TAB_WIDGET = "android.widget.TabWidget";

  public static void initializeAccessibilityLabel(
      @Nullable final String label, final AccessibilityNodeInfoCompat info) {
    if (label != null) {
      info.setContentDescription(label);
    }
  }

  public static void initializeAccessibilityRole(
      final Context context,
      @Nullable final String role,
      @Nullable final View view,
      final AccessibilityNodeInfoCompat info) {
    if (role != null) {
      switch (role) {
        case ACCESSIBILITY_ROLE_BUTTON:
        case ACCESSIBILITY_ROLE_IMAGE_BUTTON:
          info.setClassName(ACCESSIBILITY_CLASS_BUTTON);
          break;
        case ACCESSIBILITY_ROLE_HEADER:
          info.setHeading(true);
          if (view != null) {
            ViewCompat.setAccessibilityHeading(view, true);
          }
          break;
        case ACCESSIBILITY_ROLE_SELECTED_BUTTON:
          info.setClassName(ACCESSIBILITY_CLASS_BUTTON);
          // TODO 79642626: we should use the selected property here
          info.setSelected(true);
          break;
        case ACCESSIBILITY_ROLE_IMAGE:
          info.setClassName(ACCESSIBILITY_CLASS_IMAGE);
          info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SELECT);
          break;
        case ACCESSIBILITY_ROLE_TAB_WIDGET:
          info.setClassName(ACCESSIBILITY_CLASS_TAB_WIDGET);
          break;
        case ACCESSIBILITY_ROLE_LINK:
          info.setClassName(ACCESSIBILITY_CLASS_BUTTON);
          info.setRoleDescription(context.getString(R.string.accessibility_link_role));
          break;
      }
    }
  }
}
