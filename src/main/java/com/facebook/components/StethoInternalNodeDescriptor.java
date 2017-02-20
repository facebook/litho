// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.util.Map;

import android.graphics.Rect;
import android.view.View;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;

public final class StethoInternalNodeDescriptor
    extends AbstractChainedDescriptor<StethoInternalNode>
    implements HighlightableDescriptor<StethoInternalNode> {

  @Override
  protected String onGetNodeName(StethoInternalNode element) {
    final Component component = element.node.getComponent();
    return component == null
        ? Container.class.getName()
        : component.getLifecycle().getClass().getName();
  }

  @Override
  protected void onGetChildren(StethoInternalNode element, Accumulator<Object> children) {
    final ComponentsStethoManager stethoManager = element.node.getContext().getStethoManager();
    for (int i = 0, count = element.node.getChildCount(); i < count; i++) {
      children.store(stethoManager.getStethoInternalNode(element.node.getChildAt(i)));
    }

    if (element.node.hasNestedTree()) {
      final InternalNode nestedTree = element.node.getNestedTree();
      for (int i = 0, count = nestedTree.getChildCount(); i < count; i++) {
        children.store(stethoManager.getStethoInternalNode(nestedTree.getChildAt(i)));
      }
    }
  }

  @Override
  public View getViewAndBoundsForHighlighting(StethoInternalNode element, Rect bounds) {
    final int x = getXFromRoot(element.node);
    final int y = getYFromRoot(element.node);
    bounds.set(x, y, x + element.node.getWidth(), y + element.node.getHeight());
    return element.node.getContext().getComponentTree().getComponentView();
  }

  @Override
  protected void onGetAttributes(StethoInternalNode element, AttributeAccumulator attributes) {
    element.node.getContext().getStethoManager().getAttributes(element.node, attributes);
  }

  @Override
  protected void onSetAttributesAsText(StethoInternalNode element, String text) {
    final ComponentContext context = element.node.getContext();

    context.getStethoManager().setStyleOverrides(element.node, parseSetAttributesAsTextArg(text));

    final ComponentTree componentTree = context.getComponentTree();
    final ComponentView view = componentTree == null ? null : componentTree.getComponentView();

    if (view != null) {
      view.forceRelayout();
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
}
