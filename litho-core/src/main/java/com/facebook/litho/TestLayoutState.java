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
import static com.facebook.litho.Resolver.applyTransitionsAndUseEffectEntriesToNode;

import androidx.annotation.Nullable;
import com.facebook.rendercore.LayoutCache;
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

  public static @Nullable LithoNode createAndMeasureTreeForComponent(
      ResolveContext resolveContext,
      ComponentContext context,
      Component component,
      int widthSpec,
      int heightSpec) {

    final ComponentContext c =
        Resolver.createScopedContext(
            resolveContext,
            context,
            component,
            ComponentKeyUtils.generateGlobalKey(context, context.getComponentScope(), component),
            null);

    final LithoNode root =
        createImmediateLayout(resolveContext, c, widthSpec, heightSpec, component);

    c.clearCalculationStateContext();

    if (root == null || resolveContext.isLayoutInterrupted()) {
      return root;
    }

    final LithoLayoutContext lsc =
        new LithoLayoutContext(
            resolveContext.getTreeId(),
            resolveContext.getCache(),
            c,
            resolveContext.getTreeState(),
            resolveContext.getLayoutVersion(),
            resolveContext.getRootComponentId(),
            resolveContext.isAccessibilityEnabled(),
            new LayoutCache(),
            null,
            null);

    c.setLithoLayoutContext(lsc);

    Layout.measureTree(lsc, c.getAndroidContext(), root, widthSpec, heightSpec, null);

    c.clearCalculationStateContext();

    return root;
  }

  public static @Nullable LithoNode newImmediateLayoutBuilder(
      final ResolveContext resolveContext,
      final ComponentContext c,
      final int widthSpec,
      final int heightSpec,
      final Component component) {

    // this can be false for a mocked component
    if (component.getMountType().toString() == null) {
      return null;
    }

    if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).canResolve()) {
      if (component instanceof Wrapper) {
        return createImmediateLayout(resolveContext, c, widthSpec, heightSpec, component);
      }
      return create(resolveContext, c, widthSpec, heightSpec, component);
    }

    final LithoNode node = createInternalNode();
    final ComponentContext scopedContext =
        ComponentContext.withComponentScope(
            c, component, ComponentKeyUtils.generateGlobalKey(c, c.getComponentScope(), component));

    TestComponent testComponent = new TestComponent(component);
    ScopedComponentInfo scopedComponentInfo =
        new ScopedComponentInfo(testComponent, scopedContext, null);
    scopedComponentInfo.setCommonProps(testComponent.getCommonProps());
    node.appendComponent(scopedComponentInfo);

    return node;
  }

  /**
   * Mimicks implementation of Column.resolve or Row.resolve but uses a test InternalNode for
   * shallow child resolution.
   */
  private static @Nullable LithoNode resolve(
      ResolveContext resolveContext,
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      Component component) {

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

    LithoNode node = createInternalNode();
    node.flexDirection(reverse ? YogaFlexDirection.COLUMN_REVERSE : YogaFlexDirection.COLUMN);

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
        if (resolveContext.isFutureReleased()) {
          return null;
        }

        if (resolveContext.isLayoutInterrupted()) {
          node.appendUnresolvedComponent(child);
        } else {
          if (child != null) {
            node.child(
                TestLayoutState.newImmediateLayoutBuilder(
                    resolveContext, c, widthSpec, heightSpec, child));
          }
        }
      }
    }

    return node;
  }

  private static @Nullable LithoNode createImmediateLayout(
      final ResolveContext resolveContext,
      final ComponentContext c,
      final int widthSpec,
      final int heightSpec,
      final Component component) {

    // this can be false for a mocked component
    if (component.getMountType().toString() == null) {
      return null;
    }

    final LithoNode node;
    final LithoNode layoutCreatedInWillRender =
        component.consumeLayoutCreatedInWillRender(resolveContext, c);

    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender;
    }

    if (component instanceof SpecGeneratedComponent) {
      final TreeProps treeProps = c.getTreeProps();
      c.setTreeProps(((SpecGeneratedComponent) component).getTreePropsForChildren(c, treeProps));
    }

    if (component instanceof Wrapper) {
      Component delegate = ((Wrapper) component).delegate;
      if (delegate == null) {
        return null;
      } else {
        return newImmediateLayoutBuilder(resolveContext, c, widthSpec, heightSpec, delegate);
      }
    } else if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).canResolve()) {
      c.setTreeProps(c.getTreePropsCopy());
      if (component instanceof Column || component instanceof Row) {
        node = resolve(resolveContext, c, widthSpec, heightSpec, component);
      } else {
        node = component.resolve(resolveContext, c);
      }
    } else if (isMountSpec(component)) {
      node = createInternalNode();
    } else {
      final RenderResult renderResult = component.render(resolveContext, c, widthSpec, heightSpec);
      final Component root = renderResult.component;
      if (root == null || root.getId() <= 0) {
        node = null;
      } else {
        node = resolveImmediateSubTree(resolveContext, c, widthSpec, heightSpec, root);
      }
    }

    if (node == null) {
      return null;
    }

    final CommonProps commonProps =
        (component instanceof SpecGeneratedComponent)
            ? ((SpecGeneratedComponent) component).getCommonProps()
            : null;
    if (commonProps != null && (!isLayoutSpecWithSizeSpec(component))) {
      commonProps.copyInto(c, node);
    }

    if (node.getChildCount() == 0) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    ScopedComponentInfo scopedComponentInfo = c.getScopedComponentInfo();
    scopedComponentInfo.setCommonProps(commonProps);
    node.appendComponent(scopedComponentInfo);
    component.prepare(resolveContext, c);

    return node;
  }

  static @Nullable LithoNode resolveImmediateSubTree(
      ResolveContext resolveContext,
      final ComponentContext c,
      final int widthSpec,
      final int heightSpec,
      Component component) {

    final @Nullable LithoNode node;

    // this can be false for a mocked component
    if (component.getMountType().toString() == null) {
      node = null;
    } else if (component instanceof Wrapper) {
      Component delegate = ((Wrapper) component).delegate;
      if (delegate == null) {
        node = null;
      } else {
        node = newImmediateLayoutBuilder(resolveContext, c, widthSpec, heightSpec, delegate);
      }
    } else if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).canResolve()) {
      node = create(resolveContext, c, widthSpec, heightSpec, component);
    } else {

      node = createInternalNode();
    }

    if (node != null) {
      SpecGeneratedComponent testComponent = new TestComponent(component);
      ScopedComponentInfo scopedComponentInfo = new ScopedComponentInfo(testComponent, c, null);
      final CommonProps commonProps = testComponent.getCommonProps();
      scopedComponentInfo.setCommonProps(commonProps);
      node.appendComponent(scopedComponentInfo);
    }

    return node;
  }

  private static LithoNode createInternalNode() {
    return new LithoNode();
  }

  private static LithoNode createNestedTreeHolder(@Nullable TreeProps treeProps) {
    return new NestedTreeHolder(treeProps);
  }

  // Mimicks implementation of Layout.create but uses a custom InternalNode for shallow child
  // resolution.
  private static @Nullable LithoNode create(
      final ResolveContext resolveContext,
      final ComponentContext parent,
      final int widthSpec,
      final int heightSpec,
      Component component) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    }

    final LithoNode node;
    final ComponentContext c;
    final String globalKey;
    final ScopedComponentInfo scopedComponentInfo;

    try {

      // 1. Consume the layout created in `willrender`.
      final LithoNode cached = component.consumeLayoutCreatedInWillRender(resolveContext, parent);

      // 2. Return immediately if cached layout is available.
      if (cached != null) {
        return cached;
      }

      // 4. Update the component.
      // 5. Get the scoped context of the updated component.
      c = Resolver.createScopedContext(resolveContext, parent, component, null, null);
      globalKey = c.getGlobalKey();

      scopedComponentInfo = c.getScopedComponentInfo();
      // 6. Resolve the component into an InternalNode tree.

      final boolean shouldDeferNestedTreeResolution = isNestedTree(component);

      // If nested tree resolution is deferred, then create an nested tree holder.
      if (shouldDeferNestedTreeResolution) {
        node = createNestedTreeHolder(c.getTreeProps());
      }

      // If the component can resolve itself resolve it.
      else if (component instanceof SpecGeneratedComponent
          && ((SpecGeneratedComponent) component).canResolve()) {

        // Resolve the component into an InternalNode.
        if (component instanceof Column || component instanceof Row) {
          node = resolve(resolveContext, c, widthSpec, heightSpec, component);
        } else {
          node = component.resolve(resolveContext, c);
        }
      }

      // If the component is a MountSpec.
      else if (isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs and set the default flex direction.
        node = createInternalNode();
        node.flexDirection(YogaFlexDirection.COLUMN);
      }

      // If the component is a LayoutSpec.
      else if (isLayoutSpec(component)) {

        final RenderResult renderResult =
            component.render(resolveContext, c, widthSpec, heightSpec);
        final Component root = renderResult.component;

        if (root != null) {
          // TODO: (T57741374) this step is required because of a bug in redex.
          if (root == component) {
            node = root.resolve(resolveContext, c);
          } else {
            node = create(resolveContext, c, widthSpec, heightSpec, root);
          }
        } else {
          node = null;
        }

        if (renderResult != null && node != null) {
          applyTransitionsAndUseEffectEntriesToNode(
              renderResult.transitions, renderResult.useEffectEntries, node);
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
    if (node.getChildCount() == 0) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure || (isNestedTree(component))) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    // 10. Add the component to the InternalNode.
    node.appendComponent(scopedComponentInfo);

    // 11. Create and add transition to this component's InternalNode.
    if (c.areTransitionsEnabled()) {
      if (component instanceof SpecGeneratedComponent
          && ((SpecGeneratedComponent) component).needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(globalKey, scopedComponentInfo);
      } else {
        try {
          // Calls onCreateTransition on the Spec.
          final Transition transition =
              (component instanceof SpecGeneratedComponent)
                  ? ((SpecGeneratedComponent) component).createTransition(c)
                  : null;
          if (transition != null) {
            node.addTransition(transition);
          }
        } catch (Exception e) {
          ComponentUtils.handleWithHierarchy(parent, component, e);
        }
      }
    }

    // 12. Add attachable components
    if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component)
            .hasAttachDetachCallback()) { // needs ComponentUtils.getGlobalKey?
      node.addAttachable(
          new LayoutSpecAttachable(
              globalKey, (SpecGeneratedComponent) component, scopedComponentInfo));
    }

    // 13. Call onPrepare for MountSpecs.
    if (isMountSpec(component)) {
      try {
        component.prepare(resolveContext, c);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(parent, component, e);
      }
    }

    // 14. Add working ranges to the InternalNode.
    scopedComponentInfo.addWorkingRangeToNode(node);

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
