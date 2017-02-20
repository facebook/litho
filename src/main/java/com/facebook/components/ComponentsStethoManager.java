// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNodeAPI;
import com.facebook.yoga.YogaUnit;
import com.facebook.yoga.YogaValue;

class ComponentsStethoManager {
  private static final YogaValue YOGA_VALUE_ZERO = new YogaValue(0, YogaUnit.POINT);
  private final static YogaEdge[] edges = YogaEdge.values();

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

    ComponentsPools.release(defaults);
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
}
