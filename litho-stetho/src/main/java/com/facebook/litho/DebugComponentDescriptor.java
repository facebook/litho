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

import android.graphics.Rect;
import android.view.View;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
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
    final ComponentsStethoManagerImpl stethoManager = getStethoManager(element);
    if (stethoManager == null) {
      return;
    }

    for (DebugComponent child : element.getChildComponents(stethoManager)) {
      children.store(child);
    }

    for (View child : element.getMountedViews()) {
      children.store(child);
    }
  }

  @Override
  protected void onGetAttributes(DebugComponent element, AttributeAccumulator attributes) {
    final String testKey = element.node.getTestKey();
    if (testKey != null) {
      attributes.store("testKey", testKey);
    }

    if (!element.node.getComponents().isEmpty()) {
      final Component component = element.node.getComponents().get(element.componentIndex);
      if (component != null) {
        final String key = component.getKey();
        if (key != null) {
          attributes.store("key", key);
        }
      }
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
    final HitTestAccumulator hitTestAccumulator = new HitTestAccumulator(bounds, x, y);
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

    if (!element.node.getComponents().isEmpty()) {
      // We currently have no way of overriding props / state of non-root components
      accumulator.store("state", element.componentIndex == 0);
      accumulator.store("props", element.componentIndex == 0);

      // Only the root component has actual layout info
      if (element.componentIndex == 0) {
        accumulator.store("layout", true);
      }
    } else {
      accumulator.store("layout", true);
    }

    // This method is called once a node is inspected and not during tree creation like many of the
    // other lifecycle methods.
    logInspected(element.node.getContext());
  }

  @Override
  protected void onGetStyles(
      DebugComponent element,
      String ruleName,
      StyleAccumulator accumulator) {

    final ComponentsStethoManagerImpl stethoManager = getStethoManager(element);
    if ("layout".equals(ruleName) && stethoManager != null) {
      stethoManager.getStyles(element, accumulator);
    }

    if (element.node.getComponents().isEmpty()) {
      return;
    }

    final Component component = element.node.getComponents().get(element.componentIndex);
    if (component == null) {
      return;
    }

    final ComponentLifecycle.StateContainer stateContainer = component.getStateContainer();

    if ("props".equals(ruleName)) {
      for (Field field : component.getClass().getDeclaredFields()) {
        try {
          field.setAccessible(true);
          if (isPrimitiveField(field) && field.getAnnotation(Prop.class) != null) {
            final Object value = field.get(component);
            if (value != stateContainer && !(value instanceof ComponentLifecycle)) {
              accumulator.store(
                  field.getName(),
                  value == null ? "null" : value.toString(),
                  false);
            }
          }
        } catch (IllegalAccessException ignored) {}
      }
    } else if ("state".equals(ruleName) && stateContainer != null) {
      for (Field field : stateContainer.getClass().getDeclaredFields()) {
        try {
          field.setAccessible(true);
          if (isPrimitiveField(field) && field.getAnnotation(State.class) != null) {
            final Object value = field.get(stateContainer);
            if (!(value instanceof ComponentLifecycle)) {
              accumulator.store(
                  field.getName(),
                  value == null ? "null" : value.toString(),
                  false);
            }
          }
        } catch (IllegalAccessException ignored) {}
      }
    }
  }

  private static boolean isPrimitiveField(Field field) {
    return field.getType().isPrimitive() ||
        CharSequence.class.isAssignableFrom(field.getType());
  }

  protected void onSetStyle(
      DebugComponent element,
      String ruleName,
      String name,
      String value) {
    final ComponentContext context = element.node.getContext();
    final ComponentTree componentTree = context.getComponentTree();
    final LithoView view = componentTree == null ? null : componentTree.getLithoView();
    final ComponentsStethoManagerImpl stethoManager = getStethoManager(element);

    if (view == null || stethoManager == null) {
      return;
    }

    if ("layout".equals(ruleName)) {
      stethoManager.setStyleOverride(element, name, value);
      view.forceRelayout();
      logStyleUpdate(context);
    } else if ("props".equals(ruleName)) {
      stethoManager.setPropOverride(element, name, value);
      view.forceRelayout();
      logStyleUpdate(context);
    } else if ("state".equals(ruleName)) {
      stethoManager.setStateOverride(element, name, value);
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

  private ComponentsStethoManagerImpl getStethoManager(DebugComponent element) {
    final ComponentContext context = element.node.getContext();
    final ComponentTree componentTree = context == null ? null : context.getComponentTree();
    return componentTree == null
        ? null :
        (ComponentsStethoManagerImpl) componentTree.getStethoManager();
  }
}
