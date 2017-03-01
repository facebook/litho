// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.util.Map;

import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;

import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaDisplay;
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
  private static final YogaValue YOGA_VALUE_ZERO = new YogaValue(0, YogaUnit.POINT);
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

  void getAttributes(InternalNode node, AttributeAccumulator attributes) {
    final YogaNodeAPI yogaNode = node.mYogaNode;
    final YogaNodeAPI defaults = ComponentsPools.acquireYogaNode();

    if (yogaNode.getStyleDirection() != defaults.getStyleDirection()) {
      attributes.store("direction", toCSSString(yogaNode.getStyleDirection()));
    }

    if (yogaNode.getFlexDirection() != defaults.getFlexDirection()) {
      attributes.store("flex-direction", toCSSString(yogaNode.getFlexDirection()));
    }

    if (yogaNode.getJustifyContent() != defaults.getJustifyContent()) {
      attributes.store("justify-content", toCSSString(yogaNode.getJustifyContent()));
    }

    if (yogaNode.getAlignItems() != defaults.getAlignItems()) {
      attributes.store("align-items", toCSSString(yogaNode.getAlignItems()));
    }

    if (yogaNode.getAlignSelf() != defaults.getAlignSelf()) {
      attributes.store("align-self", toCSSString(yogaNode.getAlignSelf()));
    }

    if (yogaNode.getAlignContent() != defaults.getAlignContent()) {
      attributes.store("align-content", toCSSString(yogaNode.getAlignContent()));
    }

    if (yogaNode.getPositionType() != defaults.getPositionType()) {
      attributes.store("position", toCSSString(yogaNode.getPositionType()));
    }

    if (yogaNode.getDisplay() != defaults.getDisplay()) {
      attributes.store("display", toCSSString(yogaNode.getDisplay()));
    }

    if (yogaNode.getFlexGrow() != defaults.getFlexGrow()) {
      attributes.store("flex-grow", Float.toString(yogaNode.getFlexGrow()));
    }

    if (yogaNode.getFlexShrink() != defaults.getFlexShrink()) {
      attributes.store("flex-shrink", Float.toString(yogaNode.getFlexShrink()));
    }

    if (!yogaNode.getFlexBasis().equals(defaults.getFlexBasis())) {
      attributes.store("flex-basis", yogaValueToString(yogaNode.getFlexBasis()));
    }

    if (!yogaNode.getWidth().equals(defaults.getWidth())) {
      attributes.store("width", yogaValueToString(yogaNode.getWidth()));
    }

    if (!yogaNode.getMinWidth().equals(defaults.getMinWidth())) {
      attributes.store("min-width", yogaValueToString(yogaNode.getMinWidth()));
    }

    if (!yogaNode.getMaxWidth().equals(defaults.getMaxWidth())) {
      attributes.store("max-width", yogaValueToString(yogaNode.getMaxWidth()));
    }

    if (!yogaNode.getHeight().equals(defaults.getHeight())) {
      attributes.store("height", yogaValueToString(yogaNode.getHeight()));
    }

    if (!yogaNode.getMinHeight().equals(defaults.getMinHeight())) {
      attributes.store("min-height", yogaValueToString(yogaNode.getMinHeight()));
    }

    if (!yogaNode.getMaxHeight().equals(defaults.getMaxHeight())) {
      attributes.store("max-height", yogaValueToString(yogaNode.getMaxHeight()));
    }

    for (YogaEdge edge : edges) {
      if (!yogaNode.getMargin(edge).equals(defaults.getMargin(edge)) &&
          !yogaNode.getMargin(edge).equals(YOGA_VALUE_ZERO)) {
        attributes.store(
            "margin-" + toCSSString(edge),
            yogaValueToString(yogaNode.getMargin(edge)));
      }
    }

    for (YogaEdge edge : edges) {
      if (!yogaNode.getPadding(edge).equals(defaults.getPadding(edge)) &&
          !yogaNode.getPadding(edge).equals(YOGA_VALUE_ZERO)) {
        attributes.store(
            "padding-" + toCSSString(edge),
            yogaValueToString(yogaNode.getPadding(edge)));
      }
    }

    for (YogaEdge edge : edges) {
      if (!yogaNode.getPosition(edge).equals(defaults.getPosition(edge)) &&
          !yogaNode.getPosition(edge).equals(YOGA_VALUE_ZERO)) {
        attributes.store(
            "position-" + toCSSString(edge),
            yogaValueToString(yogaNode.getPosition(edge)));
      }
    }

    for (YogaEdge edge : edges) {
      if (Float.compare(yogaNode.getBorder(edge), defaults.getBorder(edge)) != 0 &&
          Float.compare(yogaNode.getBorder(edge), 0) != 0) {
        attributes.store("border-" + toCSSString(edge), Float.toString(yogaNode.getBorder(edge)));
      }
    }

    final String nodeKey = getGlobalKey(node);
    if (mOverrides.containsKey(nodeKey)) {
      SimpleArrayMap<String, String> styles =  mOverrides.get(nodeKey);
      for (int i = 0, size = styles.size(); i < size; i++) {
        final String key = styles.keyAt(i);
        attributes.store(key, styles.get(key));
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

        if (key.equals("display")) {
          node.display(YogaDisplay.valueOf(toEnumString(value)));
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

  public void setStyleOverrides(InternalNode element, Map<String, String> overrides) {
    final String key = getGlobalKey(element);

    SimpleArrayMap<String, String> styles = mOverrides.get(key);
    if (styles == null) {
      styles = new SimpleArrayMap<>();
      mOverrides.put(key, styles);
    }

    for (Map.Entry<String, String> entry : overrides.entrySet()) {
      styles.put(entry.getKey(), entry.getValue());
    }
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
    StethoInternalNode stethoInternalNode =
        mStethoInternalNodes.get(getGlobalKey(node));
    if (stethoInternalNode == null) {
      stethoInternalNode = new StethoInternalNode();
      stethoInternalNode.node = node;
      mStethoInternalNodes.put(getGlobalKey(node), stethoInternalNode);
    } else {
      stethoInternalNode.node = node;
    }
    return stethoInternalNode;
  }
}
