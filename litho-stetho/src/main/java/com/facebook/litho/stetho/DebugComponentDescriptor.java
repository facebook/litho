/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.stetho;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Rect;
import android.support.v4.util.Pair;
import android.view.View;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.DebugComponent;
import com.facebook.litho.annotations.Prop;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleRuleNameAccumulator;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;
import com.facebook.yoga.YogaValue;

import static com.facebook.litho.FrameworkLogEvents.EVENT_STETHO_INSPECT_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_STETHO_UPDATE_COMPONENT;

public final class DebugComponentDescriptor
    extends AbstractChainedDescriptor<DebugComponent>
    implements HighlightableDescriptor<DebugComponent> {

  private Map<String, Class> mTypeMap = new HashMap<>();

  @Override
  protected String onGetNodeName(DebugComponent element) {
    return element.getName();
  }

  @Override
  protected void onGetChildren(DebugComponent element, Accumulator<Object> children) {
    final View mountedView = element.getMountedView();
    if (mountedView != null) {
      children.store(mountedView);
    } else {
      for (DebugComponent child : element.getChildComponents()) {
        children.store(child);
      }
    }
  }

  @Override
  protected void onGetAttributes(DebugComponent element, AttributeAccumulator attributes) {
    final String testKey = element.getTestKey();
    if (testKey != null) {
      attributes.store("testKey", testKey);
    }

    final String key = element.getKey();
    if (key != null) {
      attributes.store("key", key);
    }

    if (element.isClickable()) {
      attributes.store("clickable", "true");
    }
  }

  @Override
  public View getViewAndBoundsForHighlighting(DebugComponent element, Rect bounds) {
    bounds.set(element.getBoundsInLithoView());
    return element.getLithoView();
  }

  @Override
  public Object getElementToHighlightAtPosition(
      DebugComponent element,
      int x,
      int y,
      Rect bounds) {
    final StethoHitTestAccumulator hitTestAccumulator = new StethoHitTestAccumulator(bounds, x, y);
    getChildren(element, hitTestAccumulator);

    final Object childElement = hitTestAccumulator.getElement();
    if (childElement != null) {
      return childElement;
    } else {
      return element;
    }
  }

  @Override
  protected void onGetStyleRuleNames(
      DebugComponent element,
      StyleRuleNameAccumulator accumulator) {

    if (!element.getState().isEmpty()) {
      accumulator.store("state", element.isLayoutNode());
    }

    if (!element.getProps().isEmpty()) {
      accumulator.store("props", element.isLayoutNode());
    }

    if (!element.getStyles().isEmpty()) {
      accumulator.store("layout", element.isLayoutNode());
    }

    // This method is called once a node is inspected and not during tree creation like many of the
    // other lifecycle methods.
    logInspected(element.getContext());
  }

  @Override
  protected void onGetStyles(
      DebugComponent element,
      String ruleName,
      StyleAccumulator accumulator) {

    if ("layout".equals(ruleName)) {
      final Map<String, Object> styles = element.getStyles();
      for (String key : styles.keySet()) {
        final Object value = styles.get(key);
        if (isSupportedType(value.getClass())) {
          mTypeMap.put(key, value.getClass());
          accumulator.store(key, styles.get(key).toString(), false);
        }
      }
    } else if ("props".equals(ruleName)) {
      final Map<String, Pair<Prop, Object>> props = element.getProps();
      for (String key : props.keySet()) {
        final Object value = props.get(key).second;
        if (isSupportedType(value.getClass())) {
          mTypeMap.put(key, value.getClass());
          accumulator.store(key, value.toString(), false);
        }
      }
    } else if ("state".equals(ruleName)) {
      final Map<String, Object> state = element.getState();
      for (String key : state.keySet()) {
        final Object value = state.get(key);
        if (isSupportedType(value.getClass())) {
          mTypeMap.put(key, value.getClass());
          accumulator.store(key, value.toString(), false);
        }
      }
    }
  }

  protected void onSetStyle(
      DebugComponent element,
      String ruleName,
      String name,
      String value) {
    if ("layout".equals(ruleName)) {
      element.setStyleOverride(name, cast(name, value));
      logStyleUpdate(element.getContext());
    } else if ("props".equals(ruleName)) {
      element.setPropOverride(name, cast(name, value));
      logStyleUpdate(element.getContext());
    } else if ("state".equals(ruleName)) {
      element.setStateOverride(name, cast(name, value));
      logStyleUpdate(element.getContext());
    }
  }

  private Object cast(String name, String value) {
    final Class type = mTypeMap.get(name);
    if (type == int.class || type == Integer.class) {
      return Integer.parseInt(value);
    }
    if (type == long.class || type == Long.class) {
      return Long.parseLong(value);
    }
    if (type == float.class || type == Float.class) {
      return Float.parseFloat(value);
    }
    if (type == double.class || type == Double.class) {
      return Double.parseDouble(value);
    }
    if (type == boolean.class || type == Boolean.class) {
      return Boolean.parseBoolean(value);
    }
    return value;
  }

  private void logStyleUpdate(ComponentContext context) {
    final ComponentsLogger logger = context.getLogger();
    if (logger != null) {
      logger.log(logger.newEvent(EVENT_STETHO_UPDATE_COMPONENT));
    }
  }

  private void logInspected(ComponentContext context) {
    final ComponentsLogger logger = context.getLogger();
    if (logger != null) {
      logger.log(logger.newEvent(EVENT_STETHO_INSPECT_COMPONENT));
    }
  }

  public boolean isSupportedType(Class c) {
    return c == YogaValue.class ||
        c == int.class ||
        c == Integer.class ||
        c == long.class ||
        c == Long.class ||
        c == float.class ||
        c == Float.class ||
        c == double.class ||
        c == Double.class ||
        c == boolean.class ||
        c == Boolean.class ||
        Enum.class.isAssignableFrom(c) ||
        CharSequence.class.isAssignableFrom(c);
  }
}
