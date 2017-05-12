/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.Map;

import android.graphics.Rect;
import android.view.View;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleRuleNameAccumulator;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;

import static com.facebook.litho.FrameworkLogEvents.EVENT_STETHO_INSPECT_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_STETHO_UPDATE_COMPONENT;

public final class DebugComponentDescriptor
    extends AbstractChainedDescriptor<DebugComponent>
    implements HighlightableDescriptor<DebugComponent> {

  @Override
  protected String onGetNodeName(DebugComponent element) {
    return element.getName();
  }

  @Override
  protected void onGetChildren(DebugComponent element, Accumulator<Object> children) {
    for (DebugComponent child : element.getChildComponents()) {
      children.store(child);
    }

    for (View child : element.getMountedViews()) {
      children.store(child);
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
      final Map<String, String> styles = element.getStyles();
      for (String key : styles.keySet()) {
        accumulator.store(key, styles.get(key), false);
      }
    } else if ("props".equals(ruleName)) {
      final Map<String, String> props = element.getProps();
      for (String key : props.keySet()) {
        accumulator.store(key, props.get(key), false);
      }
    } else if ("state".equals(ruleName)) {
      final Map<String, String> state = element.getState();
      for (String key : state.keySet()) {
        accumulator.store(key, state.get(key), false);
      }
    }
  }

  protected void onSetStyle(
      DebugComponent element,
      String ruleName,
      String name,
      String value) {
    final ComponentContext context = element.getContext();
    final ComponentTree componentTree = context.getComponentTree();
    final LithoView view = componentTree == null ? null : componentTree.getLithoView();

    if (view == null) {
      return;
    }

    if ("layout".equals(ruleName)) {
      element.setStyleOverride(name, value);
      view.forceRelayout();
      logStyleUpdate(context);
    } else if ("props".equals(ruleName)) {
      element.setPropOverride(name, value);
      view.forceRelayout();
      logStyleUpdate(context);
    } else if ("state".equals(ruleName)) {
      element.setStateOverride(name, value);
      view.forceRelayout();
      logStyleUpdate(context);
    }
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
}
