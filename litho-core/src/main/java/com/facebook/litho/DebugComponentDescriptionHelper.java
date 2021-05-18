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
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.annotations.Prop;
import com.facebook.proguard.annotations.DoNotStrip;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import javax.annotation.Nullable;
import org.json.JSONObject;

/**
 * Describes {@link DebugComponent}s for use in testing and debugging. Note that {@link
 * com.facebook.litho.config.ComponentsConfiguration#isEndToEndTestRun} must be enabled in order for
 * this data to be collected.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DebugComponentDescriptionHelper {

  /**
   * An interface for callsite to append extra description into {@link StringBuilder} by given
   * {@link DebugComponent}.
   */
  public interface ExtraDescription {
    void applyExtraDescription(DebugComponent debugComponent, StringBuilder sb);
  }

  /** Fields to ignore when dumping extra props */
  private static final HashSet<String> IGNORE_PROP_FIELDS =
      new HashSet<>(
          Arrays.asList(
              "delegate",
              "feedPrefetcher",
              "parentFeedContextChain",
              "child",
              "children",
              "childComponent",
              "trackingCode",
              "eventsController",
              "itemAnimator",
              "onScrollListeners",
              "recyclerConfiguration",
              "threadTileViewData",
              "textColorStateList",
              "typeface",
              "text",
              "params"));

  @DoNotStrip
  public static void addViewDescription(
      DebugComponent debugComponent,
      StringBuilder sb,
      int leftOffset,
      int topOffset,
      boolean embedded,
      boolean withProps) {
    addViewDescription(debugComponent, sb, leftOffset, topOffset, embedded, withProps, null);
  }

  /**
   * Appends a compact description of a {@link DebugComponent} for debugging purposes.
   *
   * @param debugComponent The {@link DebugComponent}
   * @param sb The {@link StringBuilder} to which the description is appended
   * @param leftOffset Offset of the parent component relative to litho view
   * @param topOffset Offset of the parent component relative to litho view
   * @param embedded Whether the call is embedded in "adb dumpsys activity"
   * @param extraDescription An interface for callsite to append extra description.
   */
  @DoNotStrip
  public static void addViewDescription(
      DebugComponent debugComponent,
      StringBuilder sb,
      int leftOffset,
      int topOffset,
      boolean embedded,
      boolean withProps,
      @Nullable ExtraDescription extraDescription) {
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
      sb.append(" litho:id/").append(testKey.replace(' ', '_'));
    }

    String textContent = debugComponent.getTextContent();
    if (textContent != null && !TextUtils.isEmpty(textContent)) {
      sb.append(" text=\"").append(fixString(textContent, 200)).append("\"");
    }

    if (withProps) {
      addExtraProps(debugComponent.getComponent(), sb);
    }

    if (extraDescription != null) {
      extraDescription.applyExtraDescription(debugComponent, sb);
    }

    if (!embedded && layout != null && layout.getClickHandler() != null) {
      sb.append(" [clickable]");
    }

    sb.append('}');
  }

  private static void addExtraProps(Object node, StringBuilder sb) {
    JSONObject props = getExtraProps(node);
    if (props.length() > 0) {
      sb.append(" props=\"").append(props.toString()).append("\"");
    }
  }

  public static JSONObject getExtraProps(Object node) {
    JSONObject props = new JSONObject();
    for (Field field : node.getClass().getDeclaredFields()) {
      try {
        if (IGNORE_PROP_FIELDS.contains(field.getName())) {
          continue;
        }
        final Prop annotation = field.getAnnotation(Prop.class);
        if (annotation == null) {
          continue;
        }
        field.setAccessible(true);
        switch (annotation.resType()) {
          case COLOR:
          case DRAWABLE:
          case DIMEN_SIZE:
          case DIMEN_OFFSET:
            // ignore
            break;
          case STRING:
            String strValue = fixString(field.get(node), 50);
            if (!TextUtils.isEmpty(strValue)) {
              props.put(field.getName(), strValue);
            }
            break;
          default:
            Object value = field.get(node);
            if (value != null) {
              props.put(field.getName(), value);
            }
            break;
        }
      } catch (Exception e) {
        try {
          props.put("DUMP-ERROR", fixString(e.getMessage(), 50));
        } catch (Exception ex) {
          // ignore
        }
      }
    }
    return props;
  }

  private static String fixString(@Nullable Object str, int maxLength) {
    if (str == null) {
      return "";
    }
    String fixed = str.toString().replace(" \n", " ").replace("\n", " ").replace("\"", "");
    if (fixed.length() > maxLength) {
      fixed = fixed.substring(0, maxLength) + "...";
    }
    return fixed;
  }
}
