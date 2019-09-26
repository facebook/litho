/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaDirection;

/**
 * This should be only used with the deprecated DiffNode based testing infrastructure. This class
 * hosts a test implementation of create and resolve layout. The implementation only resolves the
 * immediate subcomponents of the component. The implementation details of this class should be kept
 * in sync with {@link LayoutState} if the existing tests written with the deprecated testing
 * infrastructure are relevant.
 *
 * @deprecated Only to be used with the deprecated {@link ComponentTestHelper}
 */
@Deprecated
public class TestLayoutState {

  public static InternalNode createAndMeasureTreeForComponent(
      ComponentContext c, Component component, int widthSpec, int heightSpec) {

    component.updateInternalChildState(c);
    c = component.getScopedContext();
    c.setWidthSpec(widthSpec);
    c.setHeightSpec(heightSpec);

    final InternalNode root = createImmediateLayout(c, component);

    if (root == NULL_LAYOUT || c.wasLayoutInterrupted()) {
      return root;
    }

    if (root.getStyleDirection() == com.facebook.yoga.YogaDirection.INHERIT
        && LayoutState.isLayoutDirectionRTL(c.getAndroidContext())) {
      root.layoutDirection(YogaDirection.RTL);
    }

    LayoutState.measureTree(root, widthSpec, heightSpec, null);

    return root;
  }

  static InternalNode createImmediateLayout(final ComponentContext c, final Component component) {

    final InternalNode node;
    final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();

    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender;
    }

    final TreeProps treeProps = c.getTreeProps();
    c.setTreeProps(component.getTreePropsForChildren(c, treeProps));

    if (component.canResolve()) {
      c.setTreeProps(c.getTreePropsCopy());
      node = (InternalNode) component.resolve(c);
    } else if (ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled
        && isMountSpec(component)) {
      node = c.newLayoutBuilder(0, 0);
    } else {
      final Component root = component.createComponentLayout(c);
      if (root == null || root.getId() <= 0) {
        node = null;
      } else {
        node = resolveImmediateSubTree(c, root);
        if (ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled
            && Component.isLayoutSpec(root)
            && root.canResolve()) {
          node.appendComponent(root);
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
        node.setMeasureFunction(ComponentLifecycle.sMeasureFunction);
      }
    }

    node.appendComponent(component);
    component.onPrepare(c);

    return node;
  }

  static InternalNode resolveImmediateSubTree(final ComponentContext c, Component component) {
    if (component.canResolve()) {
      return LayoutState.resolve(c, component);
    }

    InternalNode node = InternalNodeUtils.create(c);
    node.appendComponent(new TestComponent(component));

    return node;
  }
}
