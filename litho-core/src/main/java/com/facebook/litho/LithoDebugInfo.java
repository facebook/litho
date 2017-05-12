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
import java.util.List;
import java.util.Map;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;
import android.util.SparseArray;

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
 * A set of methods which expose internals of the framework. These APIs should not be considered
 * public and should never be used in production. They are however useful when debugging and
 * building debugging tools.
 */
public class LithoDebugInfo {

  private static final YogaValue YOGA_VALUE_UNDEFINED =
      new YogaValue(YogaConstants.UNDEFINED, YogaUnit.UNDEFINED);
  private static final YogaValue YOGA_VALUE_AUTO =
      new YogaValue(YogaConstants.UNDEFINED, YogaUnit.AUTO);
  private final static YogaEdge[] edges = YogaEdge.values();
  private final SimpleArrayMap<String, SimpleArrayMap<String, String>> mStyleOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, SimpleArrayMap<String, String>> mPropOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, SimpleArrayMap<String, String>> mStateOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, DebugComponent> mComponentsStethoNodes =
      new SimpleArrayMap<>();

  /**
   * @return A list of active recycling pools used within Litho.
   */
  public static List<RecyclePool> getPools() {
    List<RecyclePool> pools = new ArrayList<>();
    
    for (SparseArray<RecyclePool> contentPools :
        ComponentsPools.sMountContentPoolsByContext.values()) {
      for (int i = 0, count = contentPools.size(); i < count; i++) {
        pools.add(contentPools.valueAt(i));
      }
    }

    pools.add(ComponentsPools.sLayoutStatePool);
    pools.add(ComponentsPools.sInternalNodePool);
    pools.add(ComponentsPools.sNodeInfoPool);
    pools.add(ComponentsPools.sViewNodeInfoPool);
    pools.add(ComponentsPools.sYogaNodePool);
    pools.add(ComponentsPools.sMountItemPool);
    pools.add(ComponentsPools.sLayoutOutputPool);
    pools.add(ComponentsPools.sVisibilityOutputPool);
    pools.add(ComponentsPools.sVisibilityItemPool);
    pools.add(ComponentsPools.sOutputPool);
    pools.add(ComponentsPools.sDiffNodePool);
    pools.add(ComponentsPools.sDiffPool);
    pools.add(ComponentsPools.sComponentTreeBuilderPool);
    pools.add(ComponentsPools.sStateHandlerPool);
    pools.add(ComponentsPools.sMountItemScrapArrayPool);
    pools.add(ComponentsPools.sTouchableScrapArrayPool);
    pools.add(ComponentsPools.sRectFPool);
    pools.add(ComponentsPools.sRectPool);
    pools.add(ComponentsPools.sEdgesPool);
    pools.add(ComponentsPools.sTransitionContextPool);
    pools.add(ComponentsPools.sDisplayListDrawablePool);
    pools.add(ComponentsPools.sTreePropsMapPool);
    pools.add(ComponentsPools.sArraySetPool);
    pools.add(ComponentsPools.sArrayDequePool);
    pools.add(ComponentsPools.sLogEventPool);

    if (ComponentsPools.sTestOutputPool != null) {
      pools.add(ComponentsPools.sTestOutputPool);
    }

    if (ComponentsPools.sTestItemPool != null) {
      pools.add(ComponentsPools.sTestItemPool);
    }

    if (ComponentsPools.sBorderColorDrawablePool != null) {
      pools.add(ComponentsPools.sBorderColorDrawablePool);
    }

    return pools;
  }

  private static String toCSSString(String str) {
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

  private static String toCSSString(Object obj) {
    return toCSSString(obj.toString());
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

  static float parseFloat(@Nullable String s) {
    if (s == null) {
      return 0;
    }

    try {
      return Float.parseFloat(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static void storeEnum(
      Map<String, String> styles,
      SimpleArrayMap<String, String> overrides,
      String key,
      Object value) {
    if (overrides.containsKey(key)) {
      styles.put(key, overrides.get(key));
    } else {
      styles.put(key, toCSSString(value));
    }
  }

  private static void storeFloat(
      Map<String, String> styles,
      SimpleArrayMap<String, String> overrides,
      String key,
      float value) {
    if (overrides.containsKey(key)) {
      styles.put(key, overrides.get(key));
    } else {
      styles.put(key, Float.toString(value));
    }
  }

  private static void storeYogaValue(
      Map<String, String> styles,
      SimpleArrayMap<String, String> overrides,
      String key,
      YogaValue value) {
    if (overrides.containsKey(key)) {
      styles.put(key, overrides.get(key));
    } else {
      final String valueString;
      switch (value.unit) {
        case UNDEFINED:
          valueString = "undefined";
          break;
        case POINT:
          valueString = Float.toString(value.value);
          break;
        case PERCENT:
          valueString = value.value + "%";
          break;
        case AUTO:
          valueString = "auto";
          break;
        default:
          throw new IllegalStateException();
      }
      styles.put(key, valueString);
    }
  }

  private static void storeDrawable(
      Map<String, String> styles,
      SimpleArrayMap<String, String> overrides,
      String key) {
    if (overrides.containsKey(key)) {
      styles.put(key, overrides.get(key));
    } else {
      styles.put(key, "<drawable>");
    }
  }

  Map<String, String> getStyles(DebugComponent stethoNode) {
    final Map<String, String> styles = new ArrayMap<>();
    final YogaNode yogaNode = stethoNode.node.mYogaNode;
    final YogaNode defaults = ComponentsPools.acquireYogaNode(stethoNode.node.getContext());

    SimpleArrayMap<String, String> overrides = mStyleOverrides.get(stethoNode.key);
    if (overrides == null) {
      overrides = new SimpleArrayMap<>();
      mStyleOverrides.put(stethoNode.key, overrides);
    }

    storeDrawable(styles, overrides, "background");
    storeDrawable(styles, overrides, "foreground");

    storeEnum(styles, overrides, "direction", yogaNode.getStyleDirection());
    storeEnum(styles, overrides, "flex-direction", yogaNode.getFlexDirection());
    storeEnum(styles, overrides, "justify-content", yogaNode.getJustifyContent());
    storeEnum(styles, overrides, "align-items", yogaNode.getAlignItems());
    storeEnum(styles, overrides, "align-self", yogaNode.getAlignSelf());
    storeEnum(styles, overrides, "align-content", yogaNode.getAlignContent());
    storeEnum(styles, overrides, "position", yogaNode.getPositionType());
    storeFloat(styles, overrides, "flex-grow", yogaNode.getFlexGrow());
    storeFloat(styles, overrides, "flex-shrink", yogaNode.getFlexShrink());
    storeYogaValue(styles, overrides, "flex-basis", yogaNode.getFlexBasis());

    storeYogaValue(styles, overrides, "width", yogaNode.getWidth());
    storeYogaValue(styles, overrides, "min-width", yogaNode.getMinWidth());
    storeYogaValue(styles, overrides, "max-width", yogaNode.getMaxWidth());
    storeYogaValue(styles, overrides, "height", yogaNode.getHeight());
    storeYogaValue(styles, overrides, "min-height", yogaNode.getMinHeight());
    storeYogaValue(styles, overrides, "max-height", yogaNode.getMaxHeight());

    for (YogaEdge edge : edges) {
      final String key = "margin-" + toCSSString(edge);
      storeYogaValue(styles, overrides, key, yogaNode.getMargin(edge));
    }

    for (YogaEdge edge : edges) {
      final String key = "padding-" + toCSSString(edge);
      storeYogaValue(styles, overrides, key, yogaNode.getPadding(edge));
    }

    for (YogaEdge edge : edges) {
      final String key = "position-" + toCSSString(edge);
      storeYogaValue(styles, overrides, key, yogaNode.getPosition(edge));
    }

    for (YogaEdge edge : edges) {
      final String key = "border-" + toCSSString(edge);
      storeFloat(styles, overrides, key, yogaNode.getBorder(edge));
    }

    ComponentsPools.release(defaults);
    return styles;
  }

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

  public void applyOverrides(InternalNode node) {
    final String nodeKey = getGlobalKey(node, 0); // We only override the root

    if (mStyleOverrides.containsKey(nodeKey)) {
      final SimpleArrayMap<String, String> styles = mStyleOverrides.get(nodeKey);
      for (int i = 0, size = styles.size(); i < size; i++) {
        final String key = styles.keyAt(i);
        final String value = styles.get(key);

        try {
          if (key.equals("background")) {
            node.backgroundColor(parseColor(value));
          }

          if (key.equals("foreground")) {
            node.foregroundColor(parseColor(value));
          }

          if (key.equals("direction")) {
            node.layoutDirection(YogaDirection.valueOf(toEnumString(value)));
          }

          if (key.equals("flex-direction")) {
            node.flexDirection(YogaFlexDirection.valueOf(toEnumString(value)));
          }

          if (key.equals("justify-content")) {
            node.justifyContent(YogaJustify.valueOf(toEnumString(value)));
          }

          if (key.equals("align-items")) {
            node.alignItems(YogaAlign.valueOf(toEnumString(value)));
          }

          if (key.equals("align-self")) {
            node.alignSelf(YogaAlign.valueOf(toEnumString(value)));
          }

          if (key.equals("align-content")) {
            node.alignContent(YogaAlign.valueOf(toEnumString(value)));
          }

          if (key.equals("position")) {
            node.positionType(YogaPositionType.valueOf(toEnumString(value)));
          }

          if (key.equals("flex-grow")) {
            node.flexGrow(parseFloat(value));
          }

          if (key.equals("flex-shrink")) {
            node.flexShrink(parseFloat(value));
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
              node.flexBasisAuto();
              break;
            case UNDEFINED:
            case POINT:
              node.flexBasisPx(FastMath.round(flexBasis.value));
              break;
            case PERCENT:
              node.flexBasisPercent(FastMath.round(flexBasis.value));
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
              node.widthAuto();
              break;
            case UNDEFINED:
            case POINT:
              node.widthPx(FastMath.round(width.value));
              break;
            case PERCENT:
              node.widthPercent(FastMath.round(width.value));
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
              node.minWidthPx(FastMath.round(minWidth.value));
              break;
            case PERCENT:
              node.minWidthPercent(FastMath.round(minWidth.value));
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
              node.maxWidthPx(FastMath.round(maxWidth.value));
              break;
            case PERCENT:
              node.maxWidthPercent(FastMath.round(maxWidth.value));
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
              node.heightAuto();
              break;
            case UNDEFINED:
            case POINT:
              node.heightPx(FastMath.round(height.value));
              break;
            case PERCENT:
              node.heightPercent(FastMath.round(height.value));
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
              node.minHeightPx(FastMath.round(minHeight.value));
              break;
            case PERCENT:
              node.minHeightPercent(FastMath.round(minHeight.value));
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
              node.maxHeightPx(FastMath.round(maxHeight.value));
              break;
            case PERCENT:
              node.maxHeightPercent(FastMath.round(maxHeight.value));
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
                node.marginPx(edge, FastMath.round(margin.value));
                break;
              case AUTO:
                node.marginAuto(edge);
                break;
              case PERCENT:
                node.marginPercent(edge, FastMath.round(margin.value));
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
                node.paddingPx(edge, FastMath.round(padding.value));
                break;
              case PERCENT:
                node.paddingPercent(edge, FastMath.round(padding.value));
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
                node.positionPx(edge, FastMath.round(position.value));
                break;
              case PERCENT:
                node.positionPercent(edge, FastMath.round(position.value));
                break;
            }
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("border-" + toCSSString(edge))) {
            final float border = parseFloat(value);
            node.borderWidthPx(edge, FastMath.round(border));
          }
        }
      }
    }

    if (mPropOverrides.containsKey(nodeKey)) {
      final Component component = node.getRootComponent();
      if (component != null) {
        final SimpleArrayMap<String, String> props = mPropOverrides.get(nodeKey);
        for (int i = 0, size = props.size(); i < size; i++) {
          final String key = props.keyAt(i);
          applyReflectiveOverride(component, key, props.get(key));
        }
      }
    }

    if (mStateOverrides.containsKey(nodeKey)) {
      final Component component = node.getRootComponent();
      final ComponentLifecycle.StateContainer stateContainer =
          component == null ? null : component.getStateContainer();
      if (stateContainer != null) {
        final SimpleArrayMap<String, String> state = mStateOverrides.get(nodeKey);
        for (int i = 0, size = state.size(); i < size; i++) {
          final String key = state.keyAt(i);
          applyReflectiveOverride(stateContainer, key, state.get(key));
        }
      }
    }
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
    } catch (Exception ignored) {}
  }

  public void setStyleOverride(DebugComponent stethoNode, String key, String value) {
    SimpleArrayMap<String, String> styles = mStyleOverrides.get(stethoNode.key);
    if (styles == null) {
      styles = new SimpleArrayMap<>();
      mStyleOverrides.put(stethoNode.key, styles);
    }

    styles.put(key, value);
  }

  public void setPropOverride(DebugComponent element, String key, String value) {
    SimpleArrayMap<String, String> props = mPropOverrides.get(element.key);
    if (props == null) {
      props = new SimpleArrayMap<>();
      mPropOverrides.put(element.key, props);
    }

    props.put(key, value);
  }

  public void setStateOverride(DebugComponent element, String key, String value) {
    SimpleArrayMap<String, String> props = mStateOverrides.get(element.key);
    if (props == null) {
      props = new SimpleArrayMap<>();
      mStateOverrides.put(element.key, props);
    }

    props.put(key, value);
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

  private static String getGlobalKey(InternalNode node, int componentIndex) {
    final InternalNode parent = node.getParent();
    final InternalNode nestedTreeHolder = node.getNestedTreeHolder();

    String key;
    if (parent != null) {
      key = getGlobalKey(parent, 0) + "." + parent.getChildIndex(node);
    } else if (nestedTreeHolder != null) {
      key = "nested";
    } else {
      key = "root";
    }

    return key + "(" + componentIndex + ")";
  }

  public DebugComponent getComponentsStethoNode(InternalNode node, int componentIndex) {
    final String globalKey = getGlobalKey(node, componentIndex);
    DebugComponent debugComponent =
        mComponentsStethoNodes.get(globalKey);

    if (debugComponent == null) {
      debugComponent = new DebugComponent();
      mComponentsStethoNodes.put(globalKey, debugComponent);
    }

    debugComponent.key = globalKey;
    debugComponent.node = node;
    debugComponent.componentIndex = componentIndex;

    return debugComponent;
  }
}
