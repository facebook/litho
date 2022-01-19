/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static com.facebook.litho.Component.isLayoutSpec;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isNestedTree;
import static com.facebook.litho.Component.sMeasureFunction;
import static com.facebook.litho.Layout.applyRenderResultToNode;
import static com.facebook.litho.Layout.areTransitionsEnabled;

import androidx.annotation.Nullable;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaWrap;
import java.lang.reflect.Field;
import java.util.List;

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
      LayoutStateContext layoutStateContext,
      ComponentContext c,
      Component component,
      int widthSpec,
      int heightSpec) {

    c = component.updateInternalChildState(layoutStateContext, c, null);
    c.setWidthSpec(widthSpec);
    c.setHeightSpec(heightSpec);

    final InternalNode root = createImmediateLayout(layoutStateContext, c, component);

    if (root == null || layoutStateContext.isLayoutInterrupted()) {
      return root;
    }

    Layout.measure(layoutStateContext, c, root, widthSpec, heightSpec, null, null);

    return root;
  }

  public static @Nullable InternalNode newImmediateLayoutBuilder(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component) {

    // this can be false for a mocked component
    if (component.getMountType().toString() == null) {
      return null;
    }

    if (component.canResolve()) {
      if (component instanceof Wrapper) {
        return createImmediateLayout(layoutStateContext, c, component);
      }
      return create(layoutStateContext, c, component);
    }

    final InternalNode node = createInternalNode(c);
    final ComponentContext scopedContext =
        component.updateInternalChildState(layoutStateContext, c, null);

    node.appendComponent(
        new TestComponent(component),
        scopedContext.getGlobalKey(),
        scopedContext.useStatelessComponent() ? scopedContext.getScopedComponentInfo() : null);

    return node;
  }

  /**
   * Mimicks implementation of Column.resolve or Row.resolve but uses a test InternalNode for
   * shallow child resolution.
   */
  private static InternalNode resolve(
      LayoutStateContext layoutContext, ComponentContext c, Component component) {

    // this can be false for a mocked component
    if (component.getMountType().toString() == null) {
      return null;
    }

    boolean reverse = getInternalState(component, "reverse");
    YogaAlign alignItems = getInternalState(component, "alignItems");
    YogaAlign alignContent = getInternalState(component, "alignContent");
    YogaJustify justifyContent = getInternalState(component, "justifyContent");
    YogaWrap wrap = getInternalState(component, "wrap");
    List<Component> children = getInternalState(component, "children");

    InternalNode node =
        createInternalNode(c)
            .flexDirection(reverse ? YogaFlexDirection.COLUMN_REVERSE : YogaFlexDirection.COLUMN);

    if (alignItems != null) {
      node.alignItems(alignItems);
    }

    if (alignContent != null) {
      node.alignContent(alignContent);
    }

    if (justifyContent != null) {
      node.justifyContent(justifyContent);
    }

    if (wrap != null) {
      node.wrap(wrap);
    }

    if (children != null) {
      for (Component child : children) {
        if (layoutContext.isLayoutReleased()) {
          return null;
        }

        if (layoutContext.isLayoutInterrupted()) {
          node.appendUnresolvedComponent(child);
        } else {
          if (child != null) {
            node.child(TestLayoutState.newImmediateLayoutBuilder(layoutContext, c, child));
          }
        }
      }
    }

    return node;
  }

  private static InternalNode createImmediateLayout(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component) {

    // this can be false for a mocked component
    if (component.getMountType().toString() == null) {
      return null;
    }

    final InternalNode node;
    final InternalNode layoutCreatedInWillRender =
        component.consumeLayoutCreatedInWillRender(layoutStateContext, c);

    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender;
    }

    final TreeProps treeProps = c.getTreeProps();
    c.setTreeProps(component.getTreePropsForChildren(c, treeProps));

    if (component instanceof Wrapper) {
      Component delegate = ((Wrapper) component).delegate;
      if (delegate == null) {
        return null;
      } else {
        return newImmediateLayoutBuilder(layoutStateContext, c, delegate);
      }
    } else if (component.canResolve()) {
      c.setTreeProps(c.getTreePropsCopy());
      if (component instanceof Column || component instanceof Row) {
        node = (InternalNode) resolve(layoutStateContext, c, component);
      } else {
        node = (InternalNode) component.resolve(layoutStateContext, c);
      }
    } else if (isMountSpec(component)) {
      node = createInternalNode(c);
    } else {
      final RenderResult renderResult = component.render(c);
      final Component root = renderResult.component;
      if (root == null || root.getId() <= 0) {
        node = null;
      } else {
        node = resolveImmediateSubTree(layoutStateContext, c, root);
        if (Component.isLayoutSpec(root) && root.canResolve()) {
          node.appendComponent(
              root, root.getKey(), c.useStatelessComponent() ? c.getScopedComponentInfo() : null);
        }
      }
    }

    if (node == null) {
      return null;
    }

    final CommonProps commonProps = component.getCommonProps();
    if (commonProps != null && (!isLayoutSpecWithSizeSpec(component))) {
      commonProps.copyInto(c, node);
    }

    if (node.getTailComponent() == null) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    node.appendComponent(
        component,
        component.getKey(),
        c.useStatelessComponent() ? c.getScopedComponentInfo() : null);
    component.onPrepare(c);

    return node;
  }

  static @Nullable InternalNode resolveImmediateSubTree(
      LayoutStateContext layoutStateContext, final ComponentContext c, Component component) {

    // this can be false for a mocked component
    if (component.getMountType().toString() == null) {
      return null;
    }

    if (component instanceof Wrapper) {
      Component delegate = ((Wrapper) component).delegate;
      if (delegate == null) {
        return null;
      } else {
        return newImmediateLayoutBuilder(layoutStateContext, c, delegate);
      }
    } else if (component.canResolve()) {
      return create(layoutStateContext, c, component);
    }

    InternalNode node = createInternalNode(c);
    node.appendComponent(
        new TestComponent(component),
        component.getKey(),
        c.useStatelessComponent() ? new ScopedComponentInfo(component, c, null) : null);

    return node;
  }

  private static InternalNode createInternalNode(ComponentContext c) {
    return new InputOnlyInternalNode<>(c);
  }

  private static InternalNode createNestedTreeHolder(
      ComponentContext c, @Nullable TreeProps treeProps) {
    return new InputOnlyNestedTreeHolder(c, treeProps);
  }

  // Mimicks implementation of Layout.create but uses a custom InternalNode for shallow child
  // resolution.
  private static @Nullable InternalNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      Component component) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    }

    final InternalNode node;
    final ComponentContext c;
    final String globalKey;
    final @Nullable ScopedComponentInfo scopedComponentInfo;

    try {

      // 1. Consume the layout created in `willrender`.
      final InternalNode cached =
          component.consumeLayoutCreatedInWillRender(layoutStateContext, parent);

      // 2. Return immediately if cached layout is available.
      if (cached != null) {
        return cached;
      }

      // 4. Update the component.
      // 5. Get the scoped context of the updated component.
      c = Layout.update(layoutStateContext, parent, component, false, null);
      globalKey = c.getGlobalKey();

      component = c.getComponentScope();

      scopedComponentInfo = c.useStatelessComponent() ? c.getScopedComponentInfo() : null;
      // 6. Resolve the component into an InternalNode tree.

      final boolean shouldDeferNestedTreeResolution = isNestedTree(layoutStateContext, component);

      // If nested tree resolution is deferred, then create an nested tree holder.
      if (shouldDeferNestedTreeResolution) {
        node = createNestedTreeHolder(c, c.getTreeProps());
      }

      // If the component can resolve itself resolve it.
      else if (component.canResolve()) {

        // Resolve the component into an InternalNode.
        if (component instanceof Column || component instanceof Row) {
          node = resolve(layoutStateContext, c, component);
        } else {
          node = component.resolve(layoutStateContext, c);
        }
      }

      // If the component is a MountSpec.
      else if (isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs and set the default flex direction.
        node = createInternalNode(c).flexDirection(YogaFlexDirection.COLUMN);
      }

      // If the component is a LayoutSpec.
      else if (isLayoutSpec(component)) {

        final RenderResult renderResult = component.render(c);
        final Component root = renderResult.component;

        if (root != null) {
          // TODO: (T57741374) this step is required because of a bug in redex.
          if (root == component) {
            node = root.resolve(layoutStateContext, c);
          } else {
            node = create(layoutStateContext, c, root);
          }
        } else {
          node = null;
        }

        if (renderResult != null && node != null) {
          applyRenderResultToNode(renderResult, node);
        }
      }

      // What even is this?
      else {
        throw new IllegalArgumentException("component:" + component.getSimpleName());
      }

      // 7. If the layout is null then return immediately.
      if (node == null) {
        return null;
      }

    } catch (Exception e) {
      ComponentUtils.handleWithHierarchy(parent, component, e);
      return null;
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("afterCreateLayout:" + component.getSimpleName());
    }

    // 8. Set the measure function
    // Set measure func on the root node of the generated tree so that the mount calls use
    // those (see Controller.mountNodeTree()). Handle the case where the component simply
    // delegates its layout creation to another component, i.e. the root node belongs to
    // another component.
    if (node.getTailComponent() == null) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure || (isNestedTree(layoutStateContext, component))) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    // 9. Copy the common props
    // Skip if resolving a layout with size spec because common props were copied in the previous
    // layout pass.
    final CommonProps commonProps = component.getCommonProps();

    // 10. Add the component to the InternalNode.
    node.appendComponent(component, globalKey, scopedComponentInfo);

    // 11. Create and add transition to this component's InternalNode.
    if (areTransitionsEnabled(c)) {
      if (component.needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(globalKey, component, scopedComponentInfo);
      } else {
        try {
          // Calls onCreateTransition on the Spec.
          final Transition transition = component.createTransition(c);
          if (transition != null) {
            node.addTransition(transition);
          }
        } catch (Exception e) {
          ComponentUtils.handleWithHierarchy(parent, component, e);
        }
      }
    }

    // 12. Add attachable components
    if (component.hasAttachDetachCallback()) {
      // needs ComponentUtils.getGlobalKey?
      node.addAttachable(new LayoutSpecAttachable(globalKey, component, scopedComponentInfo));
    }

    // 13. Call onPrepare for MountSpecs.
    if (isMountSpec(component)) {
      try {
        component.onPrepare(c);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(parent, component, e);
      }
    }

    // 14. Add working ranges to the InternalNode.
    Component.addWorkingRangeToNode(node, c, component);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return node;
  }

  private static <T> T getInternalState(Object object, String fieldName) {
    Field foundField = findFieldInHierarchy(getType(object), fieldName);
    try {
      return (T) foundField.get(object);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(
          "Internal error: Failed to get field in method getInternalState.", e);
    }
  }

  private static Field findFieldInHierarchy(Class<?> startClass, String fieldName) {
    Field foundField = null;
    Class<?> currentClass = startClass;
    while (currentClass != null) {
      final Field[] declaredFields = currentClass.getDeclaredFields();

      for (Field field : declaredFields) {
        if (field.getName().equals(fieldName)) {
          if (foundField != null) {
            throw new IllegalArgumentException(
                "Two or more fields matching " + fieldName + " in " + startClass + ".");
          }

          foundField = field;
        }
      }
      if (foundField != null) {
        break;
      }
      currentClass = currentClass.getSuperclass();
    }
    if (foundField == null) {
      throw new IllegalArgumentException(
          "No fields matching " + fieldName + " in " + startClass + ".");
    }
    foundField.setAccessible(true);
    return foundField;
  }

  private static Class<?> getType(Object object) {
    if (object instanceof Class<?>) {
      return (Class<?>) object;
    } else {
      return object.getClass();
    }
  }
}
