// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.util.Map;

import android.support.v4.util.SimpleArrayMap;

import com.facebook.stetho.inspector.elements.AttributeAccumulator;
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
import static java.lang.Float.parseFloat;

class ComponentsStethoManager {
  private static final YogaValue YOGA_VALUE_UNDEFINED =
      new YogaValue(YogaConstants.UNDEFINED, YogaUnit.UNDEFINED);
  private static final YogaValue YOGA_VALUE_AUTO =
      new YogaValue(YogaConstants.UNDEFINED, YogaUnit.AUTO);
  private static final YogaValue YOGA_VALUE_ZERO = new YogaValue(0, YogaUnit.POINT);
  private final static YogaEdge[] edges = YogaEdge.values();
  private final SimpleArrayMap<String, Map<String, String>> mOverrides =
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
      for (Map.Entry<String, String> entry : mOverrides.get(nodeKey).entrySet()) {
        attributes.store(entry.getKey(), entry.getValue());
      }
    }

    ComponentsPools.release(defaults);
  }

  void applyOverrides(InternalNode node) {
    final String nodeKey = getGlobalKey(node);
    if (!mOverrides.containsKey(nodeKey)) {
      return;
    }

    final Map<String, String> styles = mOverrides.get(nodeKey);
    for (Map.Entry<String, String> entry : mOverrides.get(nodeKey).entrySet()) {
      final String key = entry.getKey();
      final String value = entry.getValue();

      if (key.equals("direction")) {
        node.layoutDirection(YogaDirection.valueOf(toEnumString(value)));
      }

      if (key.equals("flex-direction")) {
        node.direction(YogaFlexDirection.valueOf(toEnumString(value)));
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

      if (key.equals("flex-basis")) {
        final YogaValue flexBasis = yogaValueFromString(value);
        switch (flexBasis.unit) {
          case UNDEFINED:
          case POINT:
            node.flexBasisPx(FastMath.round(flexBasis.value));
            break;
          case AUTO:
            // TODO
            break;
          case PERCENT:
            // TODO
            break;
        }
      }

      if (key.equals("width")) {
        final YogaValue width = yogaValueFromString(value);
        switch (width.unit) {
          case UNDEFINED:
          case POINT:
            node.widthPx(FastMath.round(width.value));
            break;
          case AUTO:
            // TODO
            break;
          case PERCENT:
            // TODO
            break;
        }
      }

      if (key.equals("min-width")) {
        final YogaValue minWidth = yogaValueFromString(value);
        switch (minWidth.unit) {
          case UNDEFINED:
          case POINT:
            node.minWidthPx(FastMath.round(minWidth.value));
            break;
          case PERCENT:
            // TODO
            break;
        }
      }

      if (key.equals("max-width")) {
        final YogaValue maxWidth = yogaValueFromString(value);
        switch (maxWidth.unit) {
          case UNDEFINED:
          case POINT:
            node.maxWidthPx(FastMath.round(maxWidth.value));
            break;
          case PERCENT:
            // TODO
            break;
        }
      }

      if (key.equals("height")) {
        final YogaValue height = yogaValueFromString(value);
        switch (height.unit) {
          case UNDEFINED:
          case POINT:
            node.heightPx(FastMath.round(height.value));
            break;
          case AUTO:
            // TODO
            break;
          case PERCENT:
            // TODO
            break;
        }
      }

      if (key.equals("min-height")) {
        final YogaValue minHeight = yogaValueFromString(value);
        switch (minHeight.unit) {
          case UNDEFINED:
          case POINT:
            node.minHeightPx(FastMath.round(minHeight.value));
            break;
          case PERCENT:
            // TODO
            break;
        }
      }

      if (key.equals("max-height")) {
        final YogaValue maxHeight = yogaValueFromString(value);
        switch (maxHeight.unit) {
          case UNDEFINED:
          case POINT:
            node.maxHeightPx(FastMath.round(maxHeight.value));
            break;
          case PERCENT:
            // TODO
            break;
        }
      }

      for (YogaEdge edge : edges) {
        if (key.equals("margin-" + toCSSString(edge))) {
          final YogaValue margin = yogaValueFromString(value);
          switch (margin.unit) {
            case UNDEFINED:
            case POINT:
              node.marginPx(edge, FastMath.round(margin.value));
              break;
            case AUTO:
              // TODO
              break;
            case PERCENT:
              // TODO
              break;
          }
        }
      }

      for (YogaEdge edge : edges) {
        if (key.equals("padding-" + toCSSString(edge))) {
          final YogaValue padding = yogaValueFromString(value);
          switch (padding.unit) {
            case UNDEFINED:
            case POINT:
              node.paddingPx(edge, FastMath.round(padding.value));
              break;
            case PERCENT:
              // TODO
              break;
          }
        }
      }

      for (YogaEdge edge : edges) {
        if (key.equals( "position-" + toCSSString(edge))) {
          final YogaValue position = yogaValueFromString(value);
          switch (position.unit) {
            case UNDEFINED:
            case POINT:
              node.positionPx(edge, FastMath.round(position.value));
              break;
            case PERCENT:
              // TODO
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
    mOverrides.put(getGlobalKey(element), overrides);
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

  private String getGlobalKey(InternalNode node) {
    if (node.getComponent() != null) {
      return node.getComponent().getGlobalKey();
    }

    // This is a container. Try to construct a at least semi stable id by concatenating
    // the depth of this node with the sibling position.
    int parentCount = 0;
    InternalNode parent = node.getParent();
    while (parent != null) {
      parent = parent.getParent();
      parentCount++;
    }

    final int childIndex = node.getParent() == null ? 0 : node.getParent().getChildIndex(node);
    return parentCount + "." + childIndex;
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
