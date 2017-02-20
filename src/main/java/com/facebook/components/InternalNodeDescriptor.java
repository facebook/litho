// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.graphics.Rect;
import android.view.View;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;

public final class InternalNodeDescriptor
    extends AbstractChainedDescriptor<InternalNode>
    implements HighlightableDescriptor<InternalNode> {

  @Override
  protected String onGetNodeName(InternalNode element) {
    return element.getComponent() == null
        ? Container.class.getName()
        : element.getComponent().getLifecycle().getClass().getName();
  }

  @Override
  protected void onGetChildren(InternalNode element, Accumulator<Object> children) {
    for (int i = 0, count = element.getChildCount(); i < count; i++) {
      children.store(element.getChildAt(i));
    }

    if (element.hasNestedTree()) {
      final InternalNode nestedTree = element.getNestedTree();
      for (int i = 0, count = nestedTree.getChildCount(); i < count; i++) {
        children.store(nestedTree.getChildAt(i));
      }
    }
  }

  @Override
  public View getViewAndBoundsForHighlighting(InternalNode element, Rect bounds) {
    final int x = getXFromRoot(element);
    final int y = getYFromRoot(element);
    bounds.set(x, y, x + element.getWidth(), y + element.getHeight());
    return element.getContext().getComponentTree().getComponentView();
  }

  private InternalNode parent(InternalNode node) {
    return node.getParent() != null ? node.getParent() : node.getNestedTreeHolder();
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
