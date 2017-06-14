/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.stetho;

import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.DebugComponent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.reference.Reference;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleRuleNameAccumulator;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaValue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.facebook.litho.FrameworkLogEvents.EVENT_STETHO_INSPECT_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_STETHO_UPDATE_COMPONENT;

public final class DebugComponentDescriptor
    extends AbstractChainedDescriptor<DebugComponent>
    implements HighlightableDescriptor<DebugComponent> {

  private Map<String, Class> mTypeMap = new HashMap<>();

  private final SimpleArrayMap<DebugComponent, SimpleArrayMap<String, Object>> mStyleOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<DebugComponent, SimpleArrayMap<String, Object>> mPropOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<DebugComponent, SimpleArrayMap<String, Object>> mStateOverrides =
      new SimpleArrayMap<>();

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
  public Object getElementToHighlightAtPosition(DebugComponent element, int x, int y, Rect bounds) {
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
  protected void onGetStyleRuleNames(DebugComponent element, StyleRuleNameAccumulator accumulator) {
    if (!getState(element).isEmpty()) {
      accumulator.store("state", element.isLayoutNode());
    }

    if (!getProps(element).isEmpty()) {
      accumulator.store("props", element.isLayoutNode());
    }

    if (!getStyles(element).isEmpty()) {
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
      final Map<String, Object> styles = getStyles(element);
      for (String key : styles.keySet()) {
        final Object value = styles.get(key);
        if (isSupportedType(value.getClass())) {
          mTypeMap.put(key, value.getClass());
          accumulator.store(key, styles.get(key).toString(), false);
        }
      }
    } else if ("props".equals(ruleName)) {
      final Map<String, Pair<Prop, Object>> props = getProps(element);
      for (String key : props.keySet()) {
        final Object value = props.get(key).second;
        if (isSupportedType(value.getClass())) {
          mTypeMap.put(key, value.getClass());
          accumulator.store(key, value.toString(), false);
        }
      }
    } else if ("state".equals(ruleName)) {
      final Map<String, Object> state = getState(element);
      for (String key : state.keySet()) {
        final Object value = state.get(key);
        if (isSupportedType(value.getClass())) {
          mTypeMap.put(key, value.getClass());
          accumulator.store(key, value.toString(), false);
        }
      }
    }
  }

  protected void onSetStyle(DebugComponent element, String ruleName, String name, String value) {
    element.setOverrider(mOverrider);

    if ("layout".equals(ruleName)) {
      setStyleOverride(element, name, cast(name, value));
      logStyleUpdate(element.getContext());
    } else if ("props".equals(ruleName)) {
      setPropOverride(element, name, cast(name, value));
      logStyleUpdate(element.getContext());
    } else if ("state".equals(ruleName)) {
      setStateOverride(element, name, cast(name, value));
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

  private static boolean isSupportedType(Class c) {
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

  private static Map<String, Object> getStyles(DebugComponent element) {
    if (!element.isLayoutNode()) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, Object> styles = new ArrayMap<>();
    final YogaNode yogaNode = element.getYogaNode();
    final ComponentContext context = element.getContext();

    styles.put("background", colorFromReference(context, element.getBackground()));
    styles.put("foreground", colorFromDrawable(element.getForeground()));

    styles.put("direction", yogaNode.getStyleDirection());
    styles.put("flex-direction", yogaNode.getFlexDirection());
    styles.put("justify-content", yogaNode.getJustifyContent());
    styles.put("align-items", yogaNode.getAlignItems());
    styles.put("align-self", yogaNode.getAlignSelf());
    styles.put("align-content", yogaNode.getAlignContent());
    styles.put("position", yogaNode.getPositionType());
    styles.put("flex-grow", yogaNode.getFlexGrow());
    styles.put("flex-shrink", yogaNode.getFlexShrink());
    styles.put("flex-basis", yogaNode.getFlexBasis());

    styles.put("width", yogaNode.getWidth());
    styles.put("min-width", yogaNode.getMinWidth());
    styles.put("max-width", yogaNode.getMaxWidth());
    styles.put("height", yogaNode.getHeight());
    styles.put("min-height", yogaNode.getMinHeight());
    styles.put("max-height", yogaNode.getMaxHeight());

    for (YogaEdge edge : YogaEdge.values()) {
      final String key = "margin-" + edge.toString().toLowerCase();
      styles.put(key, yogaNode.getMargin(edge));
    }

    for (YogaEdge edge : YogaEdge.values()) {
      final String key = "padding-" + edge.toString().toLowerCase();
      styles.put(key, yogaNode.getPadding(edge));
    }

    for (YogaEdge edge : YogaEdge.values()) {
      final String key = "position-" + edge.toString().toLowerCase();
      styles.put(key, yogaNode.getPosition(edge));
    }

    for (YogaEdge edge : YogaEdge.values()) {
      final String key = "border-" + edge.toString().toLowerCase();
      final float border = yogaNode.getBorder(edge);
      styles.put(key, Float.isNaN(border) ? 0 : border);
    }

    return styles;
  }

  private static Map<String, Pair<Prop, Object>> getProps(DebugComponent element) {
    final Component component = element.getComponent();
    if (component == null) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, Pair<Prop, Object>> props = new ArrayMap<>();
    final ComponentLifecycle.StateContainer stateContainer = element.getStateContainer();

    for (Field field : component.getClass().getDeclaredFields()) {
      try {
        field.setAccessible(true);
        final Prop propAnnotation = field.getAnnotation(Prop.class);
        if (propAnnotation != null) {
          final Object value = field.get(component);
          if (value != stateContainer && !(value instanceof ComponentLifecycle)) {
            props.put(field.getName(), new Pair<>(propAnnotation, value));
          }
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return props;
  }

  private static Map<String, Object> getState(DebugComponent element) {
    final Component component = element.getComponent();
    if (component == null) {
      return Collections.EMPTY_MAP;
    }

    final ComponentLifecycle.StateContainer stateContainer = element.getStateContainer();
    if (stateContainer == null) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, Object> state = new ArrayMap<>();

    for (Field field : stateContainer.getClass().getDeclaredFields()) {
      try {
        field.setAccessible(true);
        if (field.getAnnotation(State.class) != null) {
          final Object value = field.get(stateContainer);
          if (!(value instanceof ComponentLifecycle)) {
            state.put(field.getName(), value);
          }
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return state;
  }

  private synchronized void setStyleOverride(DebugComponent element, String key, Object value) {
    SimpleArrayMap<String, Object> styles = mStyleOverrides.get(element);
    if (styles == null) {
      styles = new SimpleArrayMap<>();
      mStyleOverrides.put(element, styles);
    }

    styles.put(key, value);
    element.rerender();
  }

  private synchronized void setPropOverride(DebugComponent element, String key, Object value) {
    SimpleArrayMap<String, Object> props = mPropOverrides.get(element);
    if (props == null) {
      props = new SimpleArrayMap<>();
      mPropOverrides.put(element, props);
    }

    props.put(key, value);
    element.rerender();
  }

  private synchronized void setStateOverride(DebugComponent element, String key, Object value) {
    SimpleArrayMap<String, Object> props = mStateOverrides.get(element);
    if (props == null) {
      props = new SimpleArrayMap<>();
      mStateOverrides.put(element, props);
    }

    props.put(key, value);
    element.rerender();
  }

  private static int colorFromDrawable(Drawable d) {
    if (d instanceof ColorDrawable) {
      return ((ColorDrawable) d).getColor();
    }
    return 0;
  }

  private static <T extends Drawable> int colorFromReference(ComponentContext c, Reference<T> r) {
    if (r == null) {
      return colorFromDrawable(null);
    }

    final T d  = Reference.acquire(c, r);
    final int v = colorFromDrawable(d);
    Reference.release(c, d, r);
    return v;
  }

  private final DebugComponent.Overrider mOverrider = new DebugComponent.Overrider() {
    @Override
    public void applyOverrides(DebugComponent node) {
      if (mStyleOverrides.containsKey(node)) {
        final SimpleArrayMap<String, Object> styles = mStyleOverrides.get(node);
        for (int i = 0, size = styles.size(); i < size; i++) {
          final String key = styles.keyAt(i);
          final Object value = styles.get(key);

          try {
            if (key.equals("background")) {
              node.setBackgroundColor((Integer) value);
            }

            if (key.equals("foreground")) {
              node.setForegroundColor((Integer) value);
            }

            if (key.equals("direction")) {
              node.setLayoutDirection(YogaDirection.valueOf(((String) value).toUpperCase()));
            }

            if (key.equals("flex-direction")) {
              node.setFlexDirection(YogaFlexDirection.valueOf(((String) value).toUpperCase()));
            }

            if (key.equals("justify-content")) {
              node.setJustifyContent(YogaJustify.valueOf(((String) value).toUpperCase()));
            }

            if (key.equals("align-items")) {
              node.setAlignItems(YogaAlign.valueOf(((String) value).toUpperCase()));
            }

            if (key.equals("align-self")) {
              node.setAlignSelf(YogaAlign.valueOf(((String) value).toUpperCase()));
            }

            if (key.equals("align-content")) {
              node.setAlignContent(YogaAlign.valueOf(((String) value).toUpperCase()));
            }

            if (key.equals("position")) {
              node.setPositionType(YogaPositionType.valueOf(((String) value).toUpperCase()));
            }

            if (key.equals("flex-grow")) {
              node.setFlexGrow((Float) value);
            }

            if (key.equals("flex-shrink")) {
              node.setFlexShrink((Float) value);
            }
          } catch (IllegalArgumentException ignored) {
            // ignore errors when the user suplied an invalid enum value
          }

          if (key.equals("flex-basis")) {
            final YogaValue flexBasis = YogaValue.parse(((String) value).toLowerCase());
            if (flexBasis != null) {
              node.setFlexBasis(flexBasis);
            }
          }

          if (key.equals("width")) {
            final YogaValue width = YogaValue.parse(((String) value).toLowerCase());
            if (width != null) {
              node.setWidth(width);
            }
          }

          if (key.equals("min-width")) {
            final YogaValue minWidth = YogaValue.parse(((String) value).toLowerCase());
            if (minWidth != null) {
              node.setMinWidth(minWidth);
            }
          }

          if (key.equals("max-width")) {
            final YogaValue maxWidth = YogaValue.parse(((String) value).toLowerCase());
            if (maxWidth != null) {
              node.setMaxWidth(maxWidth);
            }
          }

          if (key.equals("height")) {
            final YogaValue height = YogaValue.parse(((String) value).toLowerCase());
            if (height != null) {
              node.setHeight(height);
            }
          }

          if (key.equals("min-height")) {
            final YogaValue minHeight = YogaValue.parse(((String) value).toLowerCase());
            if (minHeight != null) {
              node.setMinHeight(minHeight);
            }
          }

          if (key.equals("max-height")) {
            final YogaValue maxHeight = YogaValue.parse(((String) value).toLowerCase());
            if (maxHeight != null) {
              node.setMaxHeight(maxHeight);
            }
          }

          for (YogaEdge edge : YogaEdge.values()) {
            if (key.equals("margin-" + edge.toString().toLowerCase())) {
              final YogaValue margin = YogaValue.parse(((String) value).toLowerCase());
              if (margin != null) {
                node.setMargin(edge, margin);
              }
            }
          }

          for (YogaEdge edge : YogaEdge.values()) {
            if (key.equals("padding-" + edge.toString().toLowerCase())) {
              final YogaValue padding = YogaValue.parse(((String) value).toLowerCase());
              if (padding != null) {
                node.setPadding(edge, padding);
              }
            }
          }

          for (YogaEdge edge : YogaEdge.values()) {
            if (key.equals("position-" + edge.toString().toLowerCase())) {
              final YogaValue position = YogaValue.parse(((String) value).toLowerCase());
              if (position != null) {
                node.setPosition(edge, position);
              }
            }
          }

          for (YogaEdge edge : YogaEdge.values()) {
            if (key.equals("border-" + edge.toString().toLowerCase())) {
              node.setBorderWidth(edge, (Float) value);
            }
          }
        }
      }

      final Component component = node.getComponent();

      if (mPropOverrides.containsKey(node)) {
        if (component != null) {
          final SimpleArrayMap<String, Object> props = mPropOverrides.get(node);
          for (int i = 0, size = props.size(); i < size; i++) {
            final String key = props.keyAt(i);
            applyReflectiveOverride(component, key, props.get(key));
          }
        }
      }

      if (mStateOverrides.containsKey(node)) {
        final ComponentLifecycle.StateContainer stateContainer = node.getStateContainer();
        if (stateContainer != null) {
          final SimpleArrayMap<String, Object> state = mStateOverrides.get(node);
          for (int i = 0, size = state.size(); i < size; i++) {
            final String key = state.keyAt(i);
            applyReflectiveOverride(stateContainer, key, state.get(key));
          }
        }
      }
    }

    private void applyReflectiveOverride(Object o, String key, Object value) {
      try {
        final Field field = o.getClass().getDeclaredField(key);
        field.setAccessible(true);
        field.set(o, value);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  };
}
