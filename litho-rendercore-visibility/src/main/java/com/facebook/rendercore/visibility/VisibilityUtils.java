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

package com.facebook.rendercore.visibility;

import androidx.annotation.Nullable;
import com.facebook.litho.FocusedVisibleEvent;
import com.facebook.litho.FullImpressionVisibleEvent;
import com.facebook.litho.InvisibleEvent;
import com.facebook.litho.UnfocusedVisibleEvent;
import com.facebook.litho.VisibilityChangedEvent;
import com.facebook.litho.VisibleEvent;
import com.facebook.rendercore.Function;
import com.facebook.rendercore.RenderCoreSystrace;

public class VisibilityUtils {

  private static VisibleEvent sVisibleEvent;
  private static InvisibleEvent sInvisibleEvent;
  private static FocusedVisibleEvent sFocusedVisibleEvent;
  private static UnfocusedVisibleEvent sUnfocusedVisibleEvent;
  private static FullImpressionVisibleEvent sFullImpressionVisibleEvent;
  private static VisibilityChangedEvent sVisibleRectChangedEvent;

  public static void dispatchOnVisible(Function<Void> visibleHandler) {
    RenderCoreSystrace.beginSection("VisibilityUtils.dispatchOnVisible");

    if (sVisibleEvent == null) {
      sVisibleEvent = new VisibleEvent();
    }
    visibleHandler.call(sVisibleEvent);

    RenderCoreSystrace.endSection();
  }

  public static void dispatchOnFocused(Function<Void> focusedHandler) {
    if (sFocusedVisibleEvent == null) {
      sFocusedVisibleEvent = new FocusedVisibleEvent();
    }
    focusedHandler.call(sFocusedVisibleEvent);
  }

  public static void dispatchOnUnfocused(Function<Void> unfocusedHandler) {
    if (sUnfocusedVisibleEvent == null) {
      sUnfocusedVisibleEvent = new UnfocusedVisibleEvent();
    }
    unfocusedHandler.call(sUnfocusedVisibleEvent);
  }

  public static void dispatchOnFullImpression(Function<Void> fullImpressionHandler) {
    if (sFullImpressionVisibleEvent == null) {
      sFullImpressionVisibleEvent = new FullImpressionVisibleEvent();
    }
    fullImpressionHandler.call(sFullImpressionVisibleEvent);
  }

  public static void dispatchOnInvisible(Function<Void> invisibleHandler) {
    if (sInvisibleEvent == null) {
      sInvisibleEvent = new InvisibleEvent();
    }
    invisibleHandler.call(sInvisibleEvent);
  }

  public static void dispatchOnVisibilityChanged(
      @Nullable Function<Void> visibilityChangedHandler,
      int visibleWidth,
      int visibleHeight,
      float percentVisibleWidth,
      float percentVisibleHeight) {

    if (visibilityChangedHandler == null) {
      return;
    }

    if (sVisibleRectChangedEvent == null) {
      sVisibleRectChangedEvent = new VisibilityChangedEvent();
    }

    sVisibleRectChangedEvent.visibleHeight = visibleHeight;
    sVisibleRectChangedEvent.visibleWidth = visibleWidth;
    sVisibleRectChangedEvent.percentVisibleHeight = percentVisibleHeight;
    sVisibleRectChangedEvent.percentVisibleWidth = percentVisibleWidth;

    visibilityChangedHandler.call(sVisibleRectChangedEvent);
  }
}
