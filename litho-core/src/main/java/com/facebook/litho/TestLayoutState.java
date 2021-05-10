/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.litho.ComponentLifecycle.sMeasureFunction;

import com.facebook.infer.annotation.Nullsafe;

/**
 * This should be only used with the deprecated DiffNode based testing infrastructure. This class
 * hosts a test implementation of create and resolve layout. The implementation only resolves the
 * immediate subcomponents of the component. The implementation details of this class should be kept
 * in sync with {@link LayoutState} if the existing tests written with the deprecated testing
 * infrastructure are relevant.
 *
 * @deprecated Only to be used with the deprecated {@link ComponentTestHelper}
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@Deprecated
public class TestLayoutState {

  public static InternalNode createAndMeasureTreeForComponent(
      ComponentContext c, Component component, int widthSpec, int heightSpec) {

    c = component.updateInternalChildState(c, null);
    c.setWidthSpec(widthSpec);
    c.setHeightSpec(heightSpec);

    final InternalNode root = createImmediateLayout(c, component);

    if (root == NULL_LAYOUT || c.wasLayoutInterrupted()) {
      return root;
    }

    Layout.measure(c, root, widthSpec, heightSpec, null, null);

    return root;
  }

  public static InternalNode newImmediateLayoutBuilder(
      final ComponentContext c, Component component) {
    if (component.canResolve()) {
      if (component instanceof Wrapper) {
        return createImmediateLayout(c, component);
      }
      return Layout.create(c, component);
    }

    final InternalNode node = InternalNodeUtils.create(c);
    final ComponentContext scopedContext = component.updateInternalChildState(c, null);

    node.appendComponent(new TestComponent(component), scopedContext.getGlobalKey());

    return node;
  }

  private static InternalNode createImmediateLayout(
      final ComponentContext c, final Component component) {

    final InternalNode node;
    final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender(c);

    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender;
    }

    final TreeProps treeProps = c.getTreeProps();
    c.setTreeProps(component.getTreePropsForChildren(c, treeProps));

    if (component instanceof Wrapper) {
      Component delegate = ((Wrapper) component).delegate;
      if (delegate == null) {
        return NULL_LAYOUT;
      } else {
        return newImmediateLayoutBuilder(c, delegate);
      }
    } else if (component.canResolve()) {
      c.setTreeProps(c.getTreePropsCopy());
      node = (InternalNode) component.resolve(c);
    } else if (isMountSpec(component)) {
      node = InternalNodeUtils.create(c);
    } else {
      final RenderResult renderResult = component.render(c);
      final Component root = renderResult.component;
      if (root == null || root.getId() <= 0) {
        node = null;
      } else {
        node = resolveImmediateSubTree(c, root);
        if (Component.isLayoutSpec(root) && root.canResolve()) {
          node.appendComponent(root, root.getKey());
        }
      }
    }

    if (node == null || node == NULL_LAYOUT) {
      return NULL_LAYOUT;
    }

    final CommonPropsCopyable commonProps = component.getCommonPropsCopyable();
    if (commonProps != null && (!isLayoutSpecWithSizeSpec(component))) {
      commonProps.copyInto(c, node);
    }

    if (node.getTailComponent() == null) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    node.appendComponent(component, component.getKey());
    component.onPrepare(c);

    return node;
  }

  static InternalNode resolveImmediateSubTree(final ComponentContext c, Component component) {
    if (component instanceof Wrapper) {
      Component delegate = ((Wrapper) component).delegate;
      if (delegate == null) {
        return NULL_LAYOUT;
      } else {
        return newImmediateLayoutBuilder(c, delegate);
      }
    } else if (component.canResolve()) {
      return Layout.create(c, component);
    }

    InternalNode node = InternalNodeUtils.create(c);
    node.appendComponent(new TestComponent(component), component.getKey());

    return node;
  }
}
