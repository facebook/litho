// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;

import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaNodeAPI;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaUnit;
import com.facebook.yoga.YogaValue;

import static com.facebook.yoga.YogaUnit.PERCENT;
import static com.facebook.yoga.YogaUnit.POINT;

class ComponentsStethoManagerImpl implements ComponentsStethoManager {
  private static final YogaValue YOGA_VALUE_UNDEFINED =
      new YogaValue(YogaConstants.UNDEFINED, YogaUnit.UNDEFINED);
  private static final YogaValue YOGA_VALUE_AUTO =
      new YogaValue(YogaConstants.UNDEFINED, YogaUnit.AUTO);
  private final static YogaEdge[] edges = YogaEdge.values();
  private final SimpleArrayMap<String, SimpleArrayMap<String, String>> mOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, StethoInternalNode> mStethoInternalNodes =
      new SimpleArrayMap<>();

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

  void getStyles(InternalNode node, StyleAccumulator accumulator) {
    final YogaNodeAPI yogaNode = node.mYogaNode;
    final YogaNodeAPI defaults = ComponentsPools.acquireYogaNode();

    accumulator.store("direction", toCSSString(yogaNode.getStyleDirection()), false);
    accumulator.store("flex-direction", toCSSString(yogaNode.getFlexDirection()), false);
    accumulator.store("justify-content", toCSSString(yogaNode.getJustifyContent()), false);
    accumulator.store("align-items", toCSSString(yogaNode.getAlignItems()), false);
    accumulator.store("align-self", toCSSString(yogaNode.getAlignSelf()), false);
    accumulator.store("align-content", toCSSString(yogaNode.getAlignContent()), false);
    accumulator.store("position", toCSSString(yogaNode.getPositionType()), false);
    accumulator.store("flex-grow", Float.toString(yogaNode.getFlexGrow()), false);
    accumulator.store("flex-shrink", Float.toString(yogaNode.getFlexShrink()), false);
    accumulator.store("flex-basis", yogaValueToString(yogaNode.getFlexBasis()), false);
    accumulator.store("width", yogaValueToString(yogaNode.getWidth()), false);
    accumulator.store("min-width", yogaValueToString(yogaNode.getMinWidth()), false);
    accumulator.store("max-width", yogaValueToString(yogaNode.getMaxWidth()), false);
    accumulator.store("height", yogaValueToString(yogaNode.getHeight()), false);
    accumulator.store("min-height", yogaValueToString(yogaNode.getMinHeight()), false);
    accumulator.store("max-height", yogaValueToString(yogaNode.getMaxHeight()), false);

    for (YogaEdge edge : edges) {
      accumulator.store(
          "margin-" + toCSSString(edge),
          yogaValueToString(yogaNode.getMargin(edge)), false);
    }

    for (YogaEdge edge : edges) {
      accumulator.store(
          "padding-" + toCSSString(edge),
          yogaValueToString(yogaNode.getPadding(edge)), false);
    }

    for (YogaEdge edge : edges) {
      accumulator.store(
          "position-" + toCSSString(edge),
          yogaValueToString(yogaNode.getPosition(edge)), false);
    }

    for (YogaEdge edge : edges) {
      accumulator.store("border-" + toCSSString(edge), Float.toString(yogaNode.getBorder(edge)), false);
    }

    final String nodeKey = getGlobalKey(node);
    if (mOverrides.containsKey(nodeKey)) {
      SimpleArrayMap<String, String> styles =  mOverrides.get(nodeKey);
      for (int i = 0, size = styles.size(); i < size; i++) {
        final String key = styles.keyAt(i);
        accumulator.store(key, styles.get(key), false);
      }
    }

    ComponentsPools.release(defaults);
  }

  public void applyOverrides(InternalNode node) {
    final String nodeKey = getGlobalKey(node);
    if (!mOverrides.containsKey(nodeKey)) {
      return;
    }

    final SimpleArrayMap<String, String> styles = mOverrides.get(nodeKey);
    for (int i = 0, size = styles.size(); i < size; i++) {
      final String key = styles.keyAt(i);
      final String value = styles.get(key);

      try {
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
        if (key.equals( "position-" + toCSSString(edge))) {
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

  public void setStyleOverride(InternalNode element, String key, String value) {
    final String globalKey = getGlobalKey(element);

    SimpleArrayMap<String, String> styles = mOverrides.get(globalKey);
    if (styles == null) {
      styles = new SimpleArrayMap<>();
      mOverrides.put(globalKey, styles);
    }

    styles.put(key, value);
  }

  private static String yogaValueToString(YogaValue v) {
    switch (v.unit) {
      case UNDEFINED: return "undefined";
      case POINT: return Float.toString(v.value);
      case PERCENT: return v.value + "%";
      case AUTO: return "auto";
      default: throw new IllegalStateException();
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

  private static String getGlobalKey(InternalNode node) {
    final InternalNode parent = node.getParent();
    final InternalNode nestedTreeHolder = node.getNestedTreeHolder();

    if (parent != null) {
      return getGlobalKey(parent) + "." + parent.getChildIndex(node);
    } else if (nestedTreeHolder != null) {
      return "nested";
    } else {
      return "root";
    }
  }

  public StethoInternalNode getStethoInternalNode(InternalNode node) {
    final String globalKey = getGlobalKey(node);
    StethoInternalNode stethoInternalNode =
        mStethoInternalNodes.get(globalKey);

    if (stethoInternalNode == null) {
      stethoInternalNode = new StethoInternalNode();
      stethoInternalNode.node = node;
      mStethoInternalNodes.put(globalKey, stethoInternalNode);
    } else {
      stethoInternalNode.node = node;
    }

    return stethoInternalNode;
  }
}
