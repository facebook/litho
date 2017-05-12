/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Rect;
import android.view.View;

/**
 * A DebugComponent represents a node in Litho's component hierarchy. DebugComponent removes the
 * need to worry about implementation details of whether a node is represented by a
 * {@link Component} or a {@link ComponentLayout}. The purpose of this class is for tools such as
 * Stetho's UI inspector to be able to easily visualize a component hierarchy without worrying about
 * implementation details of Litho.
 */
class DebugComponent {
  String key;
  InternalNode node;
  int componentIndex;

  /**
   * @return A conanical name for this component. Suitable to present to the user.
   */
  public String getName() {
    if (node.getComponents().isEmpty()) {
      switch (node.mYogaNode.getFlexDirection()) {
        case COLUMN: return Column.class.getName();
        case COLUMN_REVERSE: return ColumnReverse.class.getName();
        case ROW: return Row.class.getName();
        case ROW_REVERSE: return RowReverse.class.getName();
      }
    }

    return node
        .getComponents()
        .get(componentIndex)
        .getLifecycle()
        .getClass()
        .getName();
  }

  /**
   * Get the list of components composed by this component. This will not include any {@link View}s
   * that are mounted by this component as those are not components.
   * Use {@link this#getMountedViews} for that.
   *
   * @return A list of child components.
   */
  public List<DebugComponent> getChildComponents(ComponentsStethoManagerImpl stethoManager) {
    if (componentIndex > 0) {
      final int wrappedComponentIndex = componentIndex - 1;
      return Arrays.asList(stethoManager.getComponentsStethoNode(node, wrappedComponentIndex));
    }

    final ArrayList<DebugComponent> children = new ArrayList<>();

    for (int i = 0, count = node.getChildCount(); i < count; i++) {
      final InternalNode childNode = node.getChildAt(i);
      final int outerWrapperComponentIndex = Math.max(0, childNode.getComponents().size() - 1);
      children.add(stethoManager.getComponentsStethoNode(childNode, outerWrapperComponentIndex));
    }

    if (node.hasNestedTree()) {
      final InternalNode nestedTree = node.getNestedTree();
      for (int i = 0, count = nestedTree.getChildCount(); i < count; i++) {
        final InternalNode childNode = nestedTree.getChildAt(i);
        children.add(
            stethoManager.getComponentsStethoNode(
                childNode,
                Math.max(0, childNode.getComponents().size() - 1)));
      }
    }

    return children;
  }

  /**
   * @return A list of mounted views.
   */
  public List<View> getMountedViews() {
    final ComponentContext context = node.getContext();
    final ComponentTree tree = context == null ? null : context.getComponentTree();
    final LithoView view = tree == null ? null : tree.getLithoView();
    final MountState mountState = view == null ? null : view.getMountState();
    final ArrayList<View> children = new ArrayList<>();

    if (mountState != null) {
      for (int i = 0, count = mountState.getItemCount(); i < count; i++) {
        final MountItem mountItem = mountState.getItemAt(i);
        final Component component = mountItem == null ? null : mountItem.getComponent();

        if (component != null &&
            component == node.getRootComponent() &&
            Component.isMountViewSpec(component)) {
          children.add((View) mountItem.getContent());
        }
      }
    }

    return children;
  }

  /**
   * @return The litho view hosting this component.
   */
  public View getLithoView() {
    final ComponentContext c = node.getContext();
    final ComponentTree tree = c == null ? null : c.getComponentTree();
    return tree == null ? null : tree.getLithoView();
  }

  /**
   * @return The bounds of this component relative to its hosting {@link LithoView}.
   */
  public Rect getBoundsInLithoView() {
    final int x = getXFromRoot(node);
    final int y = getYFromRoot(node);
    return new Rect(x, y, x + node.getWidth(), y + node.getHeight());
  }

  /**
   * @return The bounds of this component relative to its parent.
   */
  public Rect getBounds() {
    final int x = node.getX();
    final int y = node.getY();
    return new Rect(x, y, x + node.getWidth(), y + node.getHeight());
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
