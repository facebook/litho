/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaUnit;
import com.facebook.yoga.YogaValue;

import static com.facebook.yoga.YogaUnit.PERCENT;
import static com.facebook.yoga.YogaUnit.POINT;

/**
 * A DebugComponent represents a node in Litho's component hierarchy. DebugComponent removes the
 * need to worry about implementation details of whether a node is represented by a
 * {@link Component} or a {@link ComponentLayout}. The purpose of this class is for tools such as
 * Stetho's UI inspector to be able to easily visualize a component hierarchy without worrying about
 * implementation details of Litho.
 */
public final class DebugComponent {

  private static final YogaValue YOGA_VALUE_UNDEFINED =
      new YogaValue(YogaConstants.UNDEFINED, YogaUnit.UNDEFINED);
  private static final YogaValue YOGA_VALUE_AUTO =
      new YogaValue(YogaConstants.UNDEFINED, YogaUnit.AUTO);
  private final static YogaEdge[] edges = YogaEdge.values();
  private final static SimpleArrayMap<String, DebugComponent> mDebugNodes = new SimpleArrayMap<>();

  private String mKey;
  private InternalNode mNode;
  private int mComponentIndex;
  private final SimpleArrayMap<String, SimpleArrayMap<String, String>> mStyleOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, SimpleArrayMap<String, String>> mPropOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, SimpleArrayMap<String, String>> mStateOverrides =
      new SimpleArrayMap<>();

  private DebugComponent() {}

  static DebugComponent getInstance(InternalNode node, int componentIndex) {
    final String globalKey = createKey(node, componentIndex);
    DebugComponent debugComponent = mDebugNodes.get(globalKey);

    if (debugComponent == null) {
      debugComponent = new DebugComponent();
      mDebugNodes.put(globalKey, debugComponent);
    }

    debugComponent.mKey = globalKey;
    debugComponent.mNode = node;
    debugComponent.mComponentIndex = componentIndex;

    return debugComponent;
  }

  /**
   * @return The root {@link DebugComponent} of a LithoView. This should be the start of your
   * traversal.
   */
  public static DebugComponent getRootInstance(LithoView view) {
    final ComponentTree component = view.getComponentTree();
    final LayoutState layoutState = component == null ? null : component.getMainThreadLayoutState();
    final InternalNode root = layoutState == null ? null : layoutState.getLayoutRoot();
    if (root != null) {
      final int outerWrapperComponentIndex = Math.max(0, root.getComponents().size() - 1);
      return DebugComponent.getInstance(root, outerWrapperComponentIndex);
    }
    return null;
  }

  /**
   * @return A conanical name for this component. Suitable to present to the user.
   */
  public String getName() {
    if (mNode.getComponents().isEmpty()) {
      switch (mNode.mYogaNode.getFlexDirection()) {
        case COLUMN: return Column.class.getName();
        case COLUMN_REVERSE: return ColumnReverse.class.getName();
        case ROW: return Row.class.getName();
        case ROW_REVERSE: return RowReverse.class.getName();
      }
    }

    return mNode
        .getComponents()
        .get(mComponentIndex)
        .getLifecycle()
        .getClass()
        .getName();
  }

  /**
   * Get the list of components composed by this component. This will not include any {@link View}s
   * that are mounted by this component as those are not components.
   * Use {@link this#getMountedViews} for that.
   *
   * @return A list of child components.
   */
  public List<DebugComponent> getChildComponents() {
    if (mComponentIndex > 0) {
      final int wrappedComponentIndex = mComponentIndex - 1;
      return Arrays.asList(getInstance(mNode, wrappedComponentIndex));
    }

    final ArrayList<DebugComponent> children = new ArrayList<>();

    for (int i = 0, count = mNode.getChildCount(); i < count; i++) {
      final InternalNode childNode = mNode.getChildAt(i);
      final int outerWrapperComponentIndex = Math.max(0, childNode.getComponents().size() - 1);
      children.add(getInstance(childNode, outerWrapperComponentIndex));
    }

    if (mNode.hasNestedTree()) {
      final InternalNode nestedTree = mNode.getNestedTree();
      for (int i = 0, count = nestedTree.getChildCount(); i < count; i++) {
        final InternalNode childNode = nestedTree.getChildAt(i);
        children.add(getInstance(childNode, Math.max(0, childNode.getComponents().size() - 1)));
      }
    }

    return children;
  }

  /**
   * @return A list of mounted views.
   */
  public List<View> getMountedViews() {
    final ComponentContext context = mNode.getContext();
    final ComponentTree tree = context == null ? null : context.getComponentTree();
    final LithoView view = tree == null ? null : tree.getLithoView();
    final MountState mountState = view == null ? null : view.getMountState();
    final ArrayList<View> children = new ArrayList<>();

    if (mountState != null) {
      for (int i = 0, count = mountState.getItemCount(); i < count; i++) {
        final MountItem mountItem = mountState.getItemAt(i);
        final Component component = mountItem == null ? null : mountItem.getComponent();

        if (component != null &&
            component == mNode.getRootComponent() &&
            Component.isMountViewSpec(component)) {
          children.add((View) mountItem.getContent());
        }
      }
    }

    return children;
  }

  /**
   * @return The litho view hosting this component.
   */
  public LithoView getLithoView() {
    final ComponentContext c = mNode.getContext();
    final ComponentTree tree = c == null ? null : c.getComponentTree();
    return tree == null ? null : tree.getLithoView();
  }

  /**
   * @return The bounds of this component relative to its hosting {@link LithoView}.
   */
  public Rect getBoundsInLithoView() {
    final int x = getXFromRoot(mNode);
    final int y = getYFromRoot(mNode);
    return new Rect(x, y, x + mNode.getWidth(), y + mNode.getHeight());
  }

  /**
   * @return The bounds of this component relative to its parent.
   */
  public Rect getBounds() {
    final int x = mNode.getX();
    final int y = mNode.getY();
    return new Rect(x, y, x + mNode.getWidth(), y + mNode.getHeight());
  }

  /**
   * @return Key-value mapping of this components layout styles.
   */
  public Map<String, String> getStyles() {
    if (!isLayoutNode()) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, String> styles = new ArrayMap<>();
    final YogaNode yogaNode = mNode.mYogaNode;
    final YogaNode defaults = ComponentsPools.acquireYogaNode(mNode.getContext());

    styles.put("background", "<drawable>");
    styles.put("foreground", "<drawable>");

    styles.put("direction", toCSSString(yogaNode.getStyleDirection()));
    styles.put("flex-direction", toCSSString(yogaNode.getFlexDirection()));
    styles.put("justify-content", toCSSString(yogaNode.getJustifyContent()));
    styles.put("align-items", toCSSString(yogaNode.getAlignItems()));
    styles.put("align-self", toCSSString(yogaNode.getAlignSelf()));
    styles.put("align-content", toCSSString(yogaNode.getAlignContent()));
    styles.put("position", toCSSString(yogaNode.getPositionType()));
    styles.put("flex-grow", Float.toString(yogaNode.getFlexGrow()));
    styles.put("flex-shrink", Float.toString(yogaNode.getFlexShrink()));
    styles.put("flex-basis", yogaNode.getFlexBasis().toString());

    styles.put("width", yogaNode.getWidth().toString());
    styles.put("min-width", yogaNode.getMinWidth().toString());
    styles.put("max-width", yogaNode.getMaxWidth().toString());
    styles.put("height", yogaNode.getHeight().toString());
    styles.put("min-height", yogaNode.getMinHeight().toString());
    styles.put("max-height", yogaNode.getMaxHeight().toString());

    for (YogaEdge edge : edges) {
      final String key = "margin-" + toCSSString(edge);
      styles.put(key, yogaNode.getMargin(edge).toString());
    }

    for (YogaEdge edge : edges) {
      final String key = "padding-" + toCSSString(edge);
      styles.put(key, yogaNode.getPadding(edge).toString());
    }

    for (YogaEdge edge : edges) {
      final String key = "position-" + toCSSString(edge);
      styles.put(key, yogaNode.getPosition(edge).toString());
    }

    for (YogaEdge edge : edges) {
      final String key = "border-" + toCSSString(edge);
      styles.put(key, Float.toString(yogaNode.getBorder(edge)));
    }

    ComponentsPools.release(defaults);
    return styles;
  }

  /**
   * @return Key-value mapping of this components props.
   */
  public Map<String, String> getProps() {
    final Component component = mNode.getComponents().isEmpty()
        ? null
        : mNode.getComponents().get(mComponentIndex);
    if (component == null) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, String> props = new ArrayMap<>();
    final ComponentLifecycle.StateContainer stateContainer = component.getStateContainer();

    for (Field field : component.getClass().getDeclaredFields()) {
      try {
        field.setAccessible(true);
        final Prop propAnnotation = field.getAnnotation(Prop.class);
        if (isPrimitiveField(field) && propAnnotation != null) {
          final Object value = field.get(component);
          if (value != stateContainer && !(value instanceof ComponentLifecycle)) {
            if (value == null) {
              props.put(field.getName(), "null");
            } else if (propAnnotation.resType() == ResType.COLOR) {
              final int i = (Integer) value;
              props.put(field.getName(), String.format("#%06X", i & 0xFFFFF).toUpperCase());
            } else {
              props.put(field.getName(), value.toString());
            }
          }
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return props;
  }

  /**
   * @return Key-value mapping of this components state.
   */
  public Map<String, String> getState() {
    final Component component = mNode.getComponents().isEmpty()
        ? null
        : mNode.getComponents().get(mComponentIndex);
    if (component == null) {
      return Collections.EMPTY_MAP;
    }

    final ComponentLifecycle.StateContainer stateContainer = component.getStateContainer();
    if (stateContainer == null) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, String> state = new ArrayMap<>();

    for (Field field : stateContainer.getClass().getDeclaredFields()) {
      try {
        field.setAccessible(true);
        if (isPrimitiveField(field) && field.getAnnotation(State.class) != null) {
          final Object value = field.get(stateContainer);
          if (!(value instanceof ComponentLifecycle)) {
            state.put(field.getName(), value == null ? "null" : value.toString());
          }
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return state;
  }

  /**
   * @return Registed an override for a style key with a certain value. This override will be used
   * The next time this component is rendered.
   */
  public void setStyleOverride(String key, String value) {
    SimpleArrayMap<String, String> styles = mStyleOverrides.get(mKey);
    if (styles == null) {
      styles = new SimpleArrayMap<>();
      mStyleOverrides.put(mKey, styles);
    }

    styles.put(key, value);
    getLithoView().forceRelayout();
  }

  /**
   * @return Registed an override for a prop key with a certain value. This override will be used
   * The next time this component is rendered.
   */
  public void setPropOverride(String key, String value) {
    SimpleArrayMap<String, String> props = mPropOverrides.get(mKey);
    if (props == null) {
      props = new SimpleArrayMap<>();
      mPropOverrides.put(mKey, props);
    }

    props.put(key, value);
    getLithoView().forceRelayout();
  }

  /**
   * @return Registed an override for a state key with a certain value. This override will be used
   * The next time this component is rendered.
   */
  public void setStateOverride(String key, String value) {
    SimpleArrayMap<String, String> props = mStateOverrides.get(mKey);
    if (props == null) {
      props = new SimpleArrayMap<>();
      mStateOverrides.put(mKey, props);
    }

    props.put(key, value);
    getLithoView().forceRelayout();
  }

  /**
   * @return the {@link ComponentContext} for this component.
   */
  public ComponentContext getContext() {
    return mNode.getContext();
  }

  /**
   * @return True if this not has layout information attached to it (backed by a Yoga node)
   */
  public boolean isLayoutNode() {
    return mNode.getComponents().isEmpty() || mComponentIndex == 0;
  }

  /**
   * @return This component's testKey or null if none is set.
   */
  public String getTestKey() {
    return isLayoutNode() ? mNode.getTestKey() : null;
  }

  /**
   * @return This component's key or null if none is set.
   */
  public String getKey() {
    if (!mNode.getComponents().isEmpty()) {
      final Component component = mNode.getComponents().get(mComponentIndex);
      return component == null ? null : component.getKey();
    }
    return null;
  }

  void applyOverrides() {
    if (mStyleOverrides.containsKey(mKey)) {
      final SimpleArrayMap<String, String> styles = mStyleOverrides.get(mKey);
      for (int i = 0, size = styles.size(); i < size; i++) {
        final String key = styles.keyAt(i);
        final String value = styles.get(key);

        try {
          if (key.equals("background")) {
            mNode.backgroundColor(parseColor(value));
          }

          if (key.equals("foreground")) {
            mNode.foregroundColor(parseColor(value));
          }

          if (key.equals("direction")) {
            mNode.layoutDirection(YogaDirection.valueOf(toEnumString(value)));
          }

          if (key.equals("flex-direction")) {
            mNode.flexDirection(YogaFlexDirection.valueOf(toEnumString(value)));
          }

          if (key.equals("justify-content")) {
            mNode.justifyContent(YogaJustify.valueOf(toEnumString(value)));
          }

          if (key.equals("align-items")) {
            mNode.alignItems(YogaAlign.valueOf(toEnumString(value)));
          }

          if (key.equals("align-self")) {
            mNode.alignSelf(YogaAlign.valueOf(toEnumString(value)));
          }

          if (key.equals("align-content")) {
            mNode.alignContent(YogaAlign.valueOf(toEnumString(value)));
          }

          if (key.equals("position")) {
            mNode.positionType(YogaPositionType.valueOf(toEnumString(value)));
          }

          if (key.equals("flex-grow")) {
            mNode.flexGrow(parseFloat(value));
          }

          if (key.equals("flex-shrink")) {
            mNode.flexShrink(parseFloat(value));
          }
        } catch (IllegalArgumentException ignored) {
          // ignore errors when the user suplied an invalid enum value
        }

        if (key.equals("flex-basis")) {
          final YogaValue flexBasis = yogaValueFromString(value);
          if (flexBasis == null) {
            continue;
          }
          switch (flexBasis.unit) {
            case AUTO:
              mNode.flexBasisAuto();
              break;
            case UNDEFINED:
            case POINT:
              mNode.flexBasisPx(FastMath.round(flexBasis.value));
              break;
            case PERCENT:
              mNode.flexBasisPercent(FastMath.round(flexBasis.value));
              break;
          }
        }

        if (key.equals("width")) {
          final YogaValue width = yogaValueFromString(value);
          if (width == null) {
            continue;
          }
          switch (width.unit) {
            case AUTO:
              mNode.widthAuto();
              break;
            case UNDEFINED:
            case POINT:
              mNode.widthPx(FastMath.round(width.value));
              break;
            case PERCENT:
              mNode.widthPercent(FastMath.round(width.value));
              break;
          }
        }

        if (key.equals("min-width")) {
          final YogaValue minWidth = yogaValueFromString(value);
          if (minWidth == null) {
            continue;
          }
          switch (minWidth.unit) {
            case UNDEFINED:
            case POINT:
              mNode.minWidthPx(FastMath.round(minWidth.value));
              break;
            case PERCENT:
              mNode.minWidthPercent(FastMath.round(minWidth.value));
              break;
          }
        }

        if (key.equals("max-width")) {
          final YogaValue maxWidth = yogaValueFromString(value);
          if (maxWidth == null) {
            continue;
          }
          switch (maxWidth.unit) {
            case UNDEFINED:
            case POINT:
              mNode.maxWidthPx(FastMath.round(maxWidth.value));
              break;
            case PERCENT:
              mNode.maxWidthPercent(FastMath.round(maxWidth.value));
              break;
          }
        }

        if (key.equals("height")) {
          final YogaValue height = yogaValueFromString(value);
          if (height == null) {
            continue;
          }
          switch (height.unit) {
            case AUTO:
              mNode.heightAuto();
              break;
            case UNDEFINED:
            case POINT:
              mNode.heightPx(FastMath.round(height.value));
              break;
            case PERCENT:
              mNode.heightPercent(FastMath.round(height.value));
              break;
          }
        }

        if (key.equals("min-height")) {
          final YogaValue minHeight = yogaValueFromString(value);
          if (minHeight == null) {
            continue;
          }
          switch (minHeight.unit) {
            case UNDEFINED:
            case POINT:
              mNode.minHeightPx(FastMath.round(minHeight.value));
              break;
            case PERCENT:
              mNode.minHeightPercent(FastMath.round(minHeight.value));
              break;
          }
        }

        if (key.equals("max-height")) {
          final YogaValue maxHeight = yogaValueFromString(value);
          if (maxHeight == null) {
            continue;
          }
          switch (maxHeight.unit) {
            case UNDEFINED:
            case POINT:
              mNode.maxHeightPx(FastMath.round(maxHeight.value));
              break;
            case PERCENT:
              mNode.maxHeightPercent(FastMath.round(maxHeight.value));
              break;
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("margin-" + toCSSString(edge))) {
            final YogaValue margin = yogaValueFromString(value);
            if (margin == null) {
              continue;
            }
            switch (margin.unit) {
              case UNDEFINED:
              case POINT:
                mNode.marginPx(edge, FastMath.round(margin.value));
                break;
              case AUTO:
                mNode.marginAuto(edge);
                break;
              case PERCENT:
                mNode.marginPercent(edge, FastMath.round(margin.value));
                break;
            }
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("padding-" + toCSSString(edge))) {
            final YogaValue padding = yogaValueFromString(value);
            if (padding == null) {
              continue;
            }
            switch (padding.unit) {
              case UNDEFINED:
              case POINT:
                mNode.paddingPx(edge, FastMath.round(padding.value));
                break;
              case PERCENT:
                mNode.paddingPercent(edge, FastMath.round(padding.value));
                break;
            }
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("position-" + toCSSString(edge))) {
            final YogaValue position = yogaValueFromString(value);
            if (position == null) {
              continue;
            }
            switch (position.unit) {
              case UNDEFINED:
              case POINT:
                mNode.positionPx(edge, FastMath.round(position.value));
                break;
              case PERCENT:
                mNode.positionPercent(edge, FastMath.round(position.value));
                break;
            }
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("border-" + toCSSString(edge))) {
            final float border = parseFloat(value);
            mNode.borderWidthPx(edge, FastMath.round(border));
          }
        }
      }
    }

    if (mPropOverrides.containsKey(mKey)) {
      final Component component = mNode.getRootComponent();
      if (component != null) {
        final SimpleArrayMap<String, String> props = mPropOverrides.get(mKey);
        for (int i = 0, size = props.size(); i < size; i++) {
          final String key = props.keyAt(i);
          applyReflectiveOverride(component, key, props.get(key));
        }
      }
    }

    if (mStateOverrides.containsKey(mKey)) {
      final Component component = mNode.getRootComponent();
      final ComponentLifecycle.StateContainer stateContainer =
          component == null ? null : component.getStateContainer();
      if (stateContainer != null) {
        final SimpleArrayMap<String, String> state = mStateOverrides.get(mKey);
        for (int i = 0, size = state.size(); i < size; i++) {
          final String key = state.keyAt(i);
          applyReflectiveOverride(stateContainer, key, state.get(key));
        }
      }
    }
  }

  private InternalNode parent(InternalNode node) {
    final InternalNode parent = node.getParent();
    return parent != null ? parent : node.getNestedTreeHolder();
  }

  private int getXFromRoot(InternalNode node) {
    if (node == null) {
      return 0;
    }
    return node.getX() + getXFromRoot(parent(node));
  }

  private int getYFromRoot(InternalNode node) {
    if (node == null) {
      return 0;
    }
    return node.getY() + getYFromRoot(parent(node));
  }

  private static String toCSSString(Object obj) {
    final String str = obj.toString();
    final StringBuilder builder = new StringBuilder(str.length());
    builder.append(str);
    for (int i = 0, length = builder.length(); i < length; ++i) {
      final char oldChar = builder.charAt(i);
      final char lowerChar = Character.toLowerCase(oldChar);
      final char newChar = lowerChar == '_' ? '-' : lowerChar;
      builder.setCharAt(i, newChar);
    }
    return builder.toString();
  }

  private static boolean isPrimitiveField(Field field) {
    return field.getType().isPrimitive() ||
        CharSequence.class.isAssignableFrom(field.getType());
  }

  private static String toEnumString(String str) {
    final StringBuilder builder = new StringBuilder(str.length());
    builder.append(str);
    for (int i = 0, length = builder.length(); i < length; ++i) {
      final char oldChar = builder.charAt(i);
      final char upperChar = Character.toUpperCase(oldChar);
      final char newChar = upperChar == '-' ? '_' : upperChar;
      builder.setCharAt(i, newChar);
    }
    return builder.toString();
  }

  private static float parseFloat(@Nullable String s) {
    if (s == null) {
      return 0;
    }

    try {
      return Float.parseFloat(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  @ColorInt
  private static int parseColor(String color) {
    if (color == null || color.length() == 0) {
      return Color.TRANSPARENT;
    }

    // Color.parse does not handle hax code with 3 ints e.g. #123
    if (color.length() == 4) {
      final char r = color.charAt(1);
      final char g = color.charAt(2);
      final char b = color.charAt(3);
      color = "#" + r + r + g + g + b + b;
    }

    return Color.parseColor(color);
  }

  private void applyReflectiveOverride(Object o, String key, String value) {
    try {
      final Field field = o.getClass().getDeclaredField(key);
      final Class type = field.getType();
      field.setAccessible(true);

      if (type.equals(short.class)) {
        field.set(o, Short.parseShort(value));
      } else if (type.equals(int.class)) {
        field.set(o, Integer.parseInt(value));
      } else if (type.equals(long.class)) {
        field.set(o, Long.parseLong(value));
      } else if (type.equals(float.class)) {
        field.set(o, Float.parseFloat(value));
      } else if (type.equals(double.class)) {
        field.set(o, Double.parseDouble(value));
      } else if (type.equals(boolean.class)) {
        field.set(o, Boolean.parseBoolean(value));
      } else if (type.equals(byte.class)) {
        field.set(o, Byte.parseByte(value));
      } else if (type.equals(char.class)) {
        field.set(o, value.charAt(0));
      } else if (CharSequence.class.isAssignableFrom(type)) {
        field.set(o, value);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static YogaValue yogaValueFromString(String s) {
    if (s == null) {
      return null;
    }

    if ("undefined".equals(s)) {
      return YOGA_VALUE_UNDEFINED;
    }

    if ("auto".equals(s)) {
      return YOGA_VALUE_AUTO;
    }

    if (s.endsWith("%")) {
      return new YogaValue(parseFloat(s.substring(0, s.length() - 1)), PERCENT);
    }

    return new YogaValue(parseFloat(s), POINT);
  }

  private static String createKey(InternalNode node, int componentIndex) {
    final InternalNode parent = node.getParent();
    final InternalNode nestedTreeHolder = node.getNestedTreeHolder();

    String key;
    if (parent != null) {
      key = createKey(parent, 0) + "." + parent.getChildIndex(node);
    } else if (nestedTreeHolder != null) {
      key = "nested";
    } else {
      final ComponentContext c = node.getContext();
      final ComponentTree tree = c == null ? null : c.getComponentTree();
      key = tree == null ? "null" : tree.toString();
    }

    return key + "(" + componentIndex + ")";
  }

  public String getId() {
    return mKey;
  }
}
