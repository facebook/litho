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

public final class ComponentStethoNodeDescriptor
    extends AbstractChainedDescriptor<ComponentStethoNode>
    implements HighlightableDescriptor<ComponentStethoNode> {

  @Override
  protected String onGetNodeName(ComponentStethoNode element) {
    if (element.node.getComponents().isEmpty()) {
      switch (element.node.mYogaNode.getFlexDirection()) {
        case COLUMN: return Column.class.getName();
        case COLUMN_REVERSE: return ColumnReverse.class.getName();
        case ROW: return Row.class.getName();
        case ROW_REVERSE: return RowReverse.class.getName();
      }
    }

    return element.node
        .getComponents()
        .get(element.componentIndex)
        .getLifecycle()
        .getClass()
        .getName();
  }

  @Override
  protected void onGetChildren(ComponentStethoNode element, Accumulator<Object> children) {
    final ComponentsStethoManagerImpl stethoManager = getStethoManager(element);
    if (stethoManager == null) {
      return;
    }

    if (element.componentIndex > 0) {
      final int wrappedComponentIndex = element.componentIndex - 1;
      children.store(stethoManager.getComponentsStethoNode(element.node, wrappedComponentIndex));
      return;
    }

    for (int i = 0, count = element.node.getChildCount(); i < count; i++) {
      final InternalNode childNode = element.node.getChildAt(i);
      final int outerWrapperComponentIndex = Math.max(0, childNode.getComponents().size() - 1);
      children.store(stethoManager.getComponentsStethoNode(childNode, outerWrapperComponentIndex));
    }

    if (element.node.hasNestedTree()) {
      final InternalNode nestedTree = element.node.getNestedTree();
      for (int i = 0, count = nestedTree.getChildCount(); i < count; i++) {
        final InternalNode childNode = nestedTree.getChildAt(i);
        children.store(stethoManager.getComponentsStethoNode(childNode, Math.max(0, childNode.getComponents().size() - 1)));
      }
    }

    final ComponentContext context = element.node.getContext();
    final ComponentTree tree = context == null ? null : context.getComponentTree();
    final LithoView view = tree == null ? null : tree.getComponentView();
    final MountState mountState = view == null ? null : view.getMountState();

    if (mountState != null) {
      for (int i = 0, count = mountState.getItemCount(); i < count; i++) {
        final MountItem mountItem = mountState.getItemAt(i);
        final Component component = mountItem == null ? null : mountItem.getComponent();

        if (component != null &&
            component == element.node.getRootComponent() &&
            Component.isMountSpec(component)) {
          children.store(mountItem.getContent());
        }
      }
    }
  }

  @Override
  protected void onGetAttributes(ComponentStethoNode element, AttributeAccumulator attributes) {
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
  public View getViewAndBoundsForHighlighting(ComponentStethoNode element, Rect bounds) {
    final int x = getXFromRoot(element.node);
    final int y = getYFromRoot(element.node);
    bounds.set(x, y, x + element.node.getWidth(), y + element.node.getHeight());

    if (element.node.getContext() != null &&
        element.node.getContext().getComponentTree() != null) {
      return element.node.getContext().getComponentTree().getComponentView();
    } else {
      return null;
    }
  }

  @Override
  public Object getElementToHighlightAtPosition(
      ComponentStethoNode element,
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
      ComponentStethoNode element,
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
    logInspected(element, element.node.getContext());
  }

  @Override
  protected void onGetStyles(
      ComponentStethoNode element,
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
      ComponentStethoNode element,
      String ruleName,
      String name,
      String value) {
    final ComponentContext context = element.node.getContext();
    final ComponentTree componentTree = context.getComponentTree();
    final LithoView view = componentTree == null ? null : componentTree.getComponentView();
    final ComponentsStethoManagerImpl stethoManager = getStethoManager(element);

    if (view == null || stethoManager == null) {
      return;
    }

    if ("layout".equals(ruleName)) {
      stethoManager.setStyleOverride(element, name, value);
      view.forceRelayout();
      logStyleUpdate(element, context);
    } else if ("props".equals(ruleName)) {
      stethoManager.setPropOverride(element, name, value);
      view.forceRelayout();
      logStyleUpdate(element, context);
    } else if ("state".equals(ruleName)) {
      stethoManager.setStateOverride(element, name, value);
      view.forceRelayout();
      logStyleUpdate(element, context);
    }
  }

  private void logStyleUpdate(ComponentStethoNode element, ComponentContext context) {
    final ComponentsLogger logger = context.getLogger();
    if (logger != null) {
      logger.eventStart(ComponentsLogger.EVENT_STETHO_UPDATE_COMPONENT, element);
      logger.eventEnd(
          ComponentsLogger.EVENT_STETHO_UPDATE_COMPONENT,
          element,
          ComponentsLogger.ACTION_SUCCESS);
    }
  }

  private void logInspected(ComponentStethoNode element, ComponentContext context) {
    final ComponentsLogger logger = context.getLogger();
    if (logger != null) {
      logger.eventStart(ComponentsLogger.EVENT_STETHO_INSPECT_COMPONENT, element);
      logger.eventEnd(
          ComponentsLogger.EVENT_STETHO_INSPECT_COMPONENT,
          element,
          ComponentsLogger.ACTION_SUCCESS);
    }
  }

  private ComponentsStethoManagerImpl getStethoManager(ComponentStethoNode element) {
    final ComponentContext context = element.node.getContext();
    final ComponentTree componentTree = context == null ? null : context.getComponentTree();
    return componentTree == null
        ? null :
        (ComponentsStethoManagerImpl) componentTree.getStethoManager();
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
}
