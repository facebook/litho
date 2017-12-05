/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.view.View;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;

/**
 * {@link ComponentLifecycle} is extended by the {@link Component} class and declare
 * methods used by a {@link Component} instances to calculate their layout bounds
 * and mount elements, among other things. This is the base class from which all new component
 * types inherit.
 */
public abstract class ComponentLifecycle implements EventDispatcher, EventTriggerTarget {
  private static final AtomicInteger sComponentTypeId = new AtomicInteger();
  private static final int DEFAULT_MAX_PREALLOCATION = 3;

  public enum MountType {
    NONE,
    DRAWABLE,
    VIEW,
  }

  public interface StateContainer {}

  /**
   * A per-Component-class data structure to keep track of some of the last mounted @Prop/@State
   * params a component was rendered with. The exact params that are tracked are just the ones
   * needed to support that Component's use of {@link Diff} params in their lifecycle methods that
   * allow Diff params (e.g. {@link #onCreateTransition}).
   */
  public interface RenderData {}

  private static final YogaBaselineFunction sBaselineFunction = new YogaBaselineFunction() {
    public float baseline(YogaNode cssNode, float width, float height) {
      final InternalNode node = (InternalNode) cssNode.getData();
      return node.getRootComponent()
          
          .onMeasureBaseline(node.getContext(), (int) width, (int) height);
    }
  };

  private static final YogaMeasureFunction sMeasureFunction =
      new YogaMeasureFunction() {

        private final Pools.SynchronizedPool<Size> mSizePool = new Pools.SynchronizedPool<>(2);

        private Size acquireSize(int initialValue) {
          Size size = mSizePool.acquire();
          if (size == null) {
            size = new Size();
          }

          size.width = initialValue;
          size.height = initialValue;
          return size;
        }

        private void releaseSize(Size size) {
          mSizePool.release(size);
        }

        @Override
        @SuppressLint("WrongCall")
        @SuppressWarnings("unchecked")
        public long measure(
            YogaNode cssNode,
            float width,
            YogaMeasureMode widthMode,
            float height,
            YogaMeasureMode heightMode) {
          final InternalNode node = (InternalNode) cssNode.getData();
          final DiffNode diffNode = node.areCachedMeasuresValid() ? node.getDiffNode() : null;
          final Component component = node.getRootComponent();
          final int widthSpec;
          final int heightSpec;
          final boolean isTracing = ComponentsSystrace.isTracing();

          if (isTracing) {
            ComponentsSystrace.beginSection("measure:" + component.getSimpleName());
          }

          widthSpec = SizeSpec.makeSizeSpecFromCssSpec(width, widthMode);
          heightSpec = SizeSpec.makeSizeSpecFromCssSpec(height, heightMode);

          node.setLastWidthSpec(widthSpec);
          node.setLastHeightSpec(heightSpec);

          int outputWidth = 0;
          int outputHeight = 0;

          if (Component.isNestedTree(component) || node.hasNestedTree()) {
            final InternalNode nestedTree =
                LayoutState.resolveNestedTree(node, widthSpec, heightSpec);

            outputWidth = nestedTree.getWidth();
            outputHeight = nestedTree.getHeight();
          } else if (diffNode != null
              && diffNode.getLastWidthSpec() == widthSpec
              && diffNode.getLastHeightSpec() == heightSpec) {
            outputWidth = (int) diffNode.getLastMeasuredWidth();
            outputHeight = (int) diffNode.getLastMeasuredHeight();
          } else {
            final Size size = acquireSize(Integer.MIN_VALUE /* initialValue */);

            try {
              component.onMeasure(node.getContext(), node, widthSpec, heightSpec, size, component);

              if (size.width < 0 || size.height < 0) {
                throw new IllegalStateException(
                    "MeasureOutput not set, ComponentLifecycle is: " + component);
              }

              outputWidth = size.width;
              outputHeight = size.height;

              if (node.getDiffNode() != null) {
                node.getDiffNode().setLastWidthSpec(widthSpec);
                node.getDiffNode().setLastHeightSpec(heightSpec);
                node.getDiffNode().setLastMeasuredWidth(outputWidth);
                node.getDiffNode().setLastMeasuredHeight(outputHeight);
              }
            } finally {
              releaseSize(size);
            }
          }

          node.setLastMeasuredWidth(outputWidth);
          node.setLastMeasuredHeight(outputHeight);

          if (isTracing) {
            ComponentsSystrace.endSection();
          }

          return YogaMeasureOutput.make(outputWidth, outputHeight);
        }
      };

  @GuardedBy("sTypeIdByComponentClass")
  private static final Map<Class, Integer> sTypeIdByComponentClass = new HashMap<>();
  private final int mTypeId;

  ComponentLifecycle() {
    this(null);
  }

  /**
   * This constructor should be called only if working with a manually crafted special Component.
   * This should NOT be used in general use cases.
   */
  protected ComponentLifecycle(Class classType) {
    if (classType == null) {
      classType = getClass();
    }

    synchronized (sTypeIdByComponentClass) {
      if (!sTypeIdByComponentClass.containsKey(classType)) {
        sTypeIdByComponentClass.put(classType, sComponentTypeId.incrementAndGet());
      }

      mTypeId = sTypeIdByComponentClass.get(classType);
    }
  }

  int getTypeId() {
    return mTypeId;
  }

  @ThreadSafe(enableChecks = false)
  public Object createMountContent(ComponentContext c) {
    return onCreateMountContent(c);
  }

  void mount(ComponentContext c, Object convertContent, Component component) {
    c.enterNoStateUpdatesMethod("mount");
    onMount(c, convertContent, component);
    c.exitNoStateUpdatesMethod();
  }

  void bind(ComponentContext c, Object mountedContent, Component component) {
    c.enterNoStateUpdatesMethod("bind");
    onBind(c, mountedContent, component);
    c.exitNoStateUpdatesMethod();
  }

  void unbind(ComponentContext c, Object mountedContent, Component component) {
    onUnbind(c, mountedContent, component);
  }

  void unmount(ComponentContext c, Object mountedContent, Component component) {
    onUnmount(c, mountedContent, component);
  }

  /**
   * Create a layout from the given component.
   *
   * @param context ComponentContext associated with the current ComponentTree.
   * @param component Component to process the layout for.
   * @param resolveNestedTree if the component's layout tree should be resolved as part of this
   *                          call.
   * @return New InternalNode associated with the given component.
   */
  ComponentLayout createLayout(
      ComponentContext context,
      Component component,
      boolean resolveNestedTree) {
    final boolean deferNestedTreeResolution =
        Component.isNestedTree(component) && !resolveNestedTree;

    final TreeProps parentTreeProps = context.getTreeProps();
    context.setTreeProps(getTreePropsForChildren(context, component, parentTreeProps));

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    }

    final InternalNode node;
    if (deferNestedTreeResolution) {
      node = ComponentsPools.acquireInternalNode(context);
      node.markIsNestedTreeHolder(context.getTreeProps());
    } else if (Component.isLayoutSpecWithSizeSpec(component)) {
      node = (InternalNode) onCreateLayoutWithSizeSpec(
          context,
          context.getWidthSpec(),
          context.getHeightSpec(),
          component);
    } else {
      node = (InternalNode) onCreateLayout(context, component);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (node == null) {
      return ComponentContext.NULL_LAYOUT;
    }

    // If this is a layout spec with size spec, and we're not deferring the nested tree resolution,
    // then we already added the props earlier on (when we did defer resolution), and
    // therefore we shouldn't add them again here.
    final CommonProps commonProps = component.getCommonProps();
    if (commonProps != null
        && (deferNestedTreeResolution || !Component.isLayoutSpecWithSizeSpec(component))) {
      commonProps.copyInto(context, node);
    }

    // Set component on the root node of the generated tree so that the mount calls use
    // those (see Controller.mountNodeTree()). Handle the case where the component simply
    // delegates its layout creation to another component i.e. the root node belongs to
    // another component.
    if (node.getRootComponent() == null) {
      node.setBaselineFunction(sBaselineFunction);

      final boolean isMountSpecWithMeasure = canMeasure() && Component.isMountSpec(component);

      if (isMountSpecWithMeasure || deferNestedTreeResolution) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    node.appendComponent(component);
    if (ComponentsConfiguration.ARE_TRANSITIONS_SUPPORTED) {
      if (needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(component);
      } else {
        final Transition transition = onCreateTransition(context, component);
        if (transition != null) {
          node.addTransition(transition);
        }
      }
    }

    if (!deferNestedTreeResolution) {
      onPrepare(context, component);
    }

    if (context.getTreeProps() != parentTreeProps) {
      ComponentsPools.release(context.getTreeProps());
      context.setTreeProps(parentTreeProps);
    }

    return node;
  }

  void loadStyle(
      ComponentContext c,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      Component component) {
    c.setDefStyle(defStyleAttr, defStyleRes);
    onLoadStyle(c, component);
    c.setDefStyle(0, 0);
  }

  void loadStyle(ComponentContext c, Component component) {
    onLoadStyle(c, component);
  }

  protected Output acquireOutput() {
    return ComponentsPools.acquireOutput();
  }

  protected void releaseOutput(Output output) {
    ComponentsPools.release(output);
  }

  protected final <T> Diff<T> acquireDiff(T previousValue, T nextValue) {
    Diff<T> diff =  ComponentsPools.acquireDiff(previousValue, nextValue);

    return diff;
  }

  protected void releaseDiff(Diff diff) {
    ComponentsPools.release(diff);
  }

  /**
   * Retrieves all of the tree props used by this Component from the TreeProps map
   * and sets the tree props as fields on the ComponentImpl.
   */
  protected void populateTreeProps(Component component, TreeProps parentTreeProps) {
  }

  /**
   * Updates the TreeProps map with outputs from all {@link OnCreateTreeProp} methods.
   */
  protected TreeProps getTreePropsForChildren(
      ComponentContext c,
      Component component,
      TreeProps previousTreeProps) {
    return previousTreeProps;
  }

  /**
   * Generate a tree of {@link ComponentLayout} representing the layout structure of
   * the {@link Component} and its sub-components. You should use
   * {@link ComponentContext#newLayoutBuilder} to build the layout tree.
   *
   * @param c The {@link ComponentContext} to build a {@link ComponentLayout} tree.
   * @param component The component to create the {@link ComponentLayout} tree from.
   */
  protected ComponentLayout onCreateLayout(ComponentContext c, Component component) {
    return Column.create(c).build();
  }

  protected ComponentLayout onCreateLayoutWithSizeSpec(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      Component component) {
    return Column.create(c).build();
  }

  protected void onPrepare(ComponentContext c, Component component) {
    // do nothing, by default
  }

  protected void onLoadStyle(ComponentContext c, Component component) {
  }

  /**
   * Called after the layout calculation is finished and the given {@link ComponentLayout}
   * has its bounds defined. You can use {@link ComponentLayout#getX()},
   * {@link ComponentLayout#getY()}, {@link ComponentLayout#getWidth()}, and
   * {@link ComponentLayout#getHeight()} to get the size and position of the component
   * in the layout tree.
   *
   * @param c The {@link Context} used by this component.
   * @param layout The {@link ComponentLayout} with defined position and size.
   * @param component The {@link Component} for this component.
   */
  protected void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      Component component) {
  }

  /**
   * Called during layout calculation to determine the baseline of a component.
   *
   * @param c The {@link Context} used by this component.
   * @param width The width of this component.
   * @param height The height of this component.
   */
  protected int onMeasureBaseline(ComponentContext c, int width, int height) {
    return height;
  }

  /**
   * Whether this {@link ComponentLifecycle} is able to measure itself according
   * to specific size constraints.
   */
  protected boolean canMeasure() {
    return false;
  }

  protected void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      Component component) {
    throw new IllegalStateException(
        "You must override onMeasure() if you return true in canMeasure(), "
            + "ComponentLifecycle is: "
            + component);
  }

  /**
   * Whether this {@link ComponentLifecycle} mounts views that contain component-based
   * content that can be incrementally mounted e.g. if the mounted view has a
   * LithoView with incremental mount enabled.
   */
  protected boolean canMountIncrementally() {
    return false;
  }

  /**
   * Whether this drawable mount spec should cache its drawing in a display list.
   */
  protected boolean shouldUseDisplayList() {
    return false;
  }

  /**
   * Create the object that will be mounted in the {@link LithoView}.
   *
   * @param context The {@link ComponentContext} to be used to create the content.
   * @return an Object that can be mounted for this component.
   */
  protected Object onCreateMountContent(ComponentContext context) {
    throw new RuntimeException(
        "Trying to mount a MountSpec that doesn't implement @OnCreateMountContent");
  }

  /**
   * @return the MountContentPool that should be used to recycle mount content for this mount spec.
   */
  protected MountContentPool onCreateMountContentPool() {
    return new DefaultMountContentPool(getClass().getSimpleName(), poolSize(), true);
  }

  /**
   * Deploy all UI elements representing the final bounds defined in the given {@link
   * ComponentLayout}. Return either a {@link Drawable} or a {@link View} or {@code null} to be
   * mounted.
   *
   * @param c The {@link ComponentContext} to mount the component into.
   * @param component The {@link Component} for this component.
   */
  protected void onMount(ComponentContext c, Object convertContent, Component component) {
    // Do nothing by default.
  }

  /**
   * Unload UI elements associated with this component.
   *
   * @param c The {@link Context} for this mount operation.
   * @param mountedContent The {@link Drawable} or {@link View} mounted by this component.
   * @param component The {@link Component} for this component.
   */
  protected void onUnmount(ComponentContext c, Object mountedContent, Component component) {
    // Do nothing by default.
  }

  protected void onBind(ComponentContext c, Object mountedContent, Component component) {
    // Do nothing by default.
  }

  protected void onUnbind(ComponentContext c, Object mountedContent, Component component) {
    // Do nothing by default.
  }

  /**
   * This indicates the type of the {@link java.lang.Object} that will be returned by
   * {@link ComponentLifecycle#mount}.
   *
   * @return one of {@link ComponentLifecycle.MountType}
   */
  public MountType getMountType() {
    return MountType.NONE;
  }

  /**
   * Populate an accessibility node with information about the component.
   * @param accessibilityNode node to populate
   * @param component The {@link Component} for this component.
   */
  protected void onPopulateAccessibilityNode(
      AccessibilityNodeInfoCompat accessibilityNode,
      Component component) {
  }

  /**
   * Populate an extra accessibility node.
   * @param accessibilityNode node to populate
   * @param extraNodeIndex index of extra node
   * @param componentBoundsX left bound of the mounted component
   * @param componentBoundsY top bound of the mounted component
   * @param component The {@link Component} for this component.
   */
  protected void onPopulateExtraAccessibilityNode(
      AccessibilityNodeInfoCompat accessibilityNode,
      int extraNodeIndex,
      int componentBoundsX,
      int componentBoundsY,
      Component component) {
  }

  /**
   * Get extra accessibility node id at a given point within the component.
   * @param x x co-ordinate within the mounted component
   * @param y y co-ordinate within the mounted component
   * @param component the {@link Component} for this component
   * @return the extra virtual view id if one is found, otherwise
   *         {@code ExploreByTouchHelper#INVALID_ID}
   */
  protected int getExtraAccessibilityNodeAt(int x, int y, Component component) {
    return ExploreByTouchHelper.INVALID_ID;
  }

  /**
   * The number of extra accessibility nodes that this component wishes to provides to the
   * accessibility system.
   * @param component the {@link Component} for this component
   * @return the number of extra nodes
   */
  protected int getExtraAccessibilityNodesCount(Component component) {
    return 0;
  }

  /**
   * Whether this component will expose any virtual views to the accessibility framework
   * @return true if the component exposes extra accessibility nodes
   */
  protected boolean implementsExtraAccessibilityNodes() {
    return false;
  }

  /**
   * Whether this component will populate any accessibility nodes or events that are passed to it.
   * @return true if the component implements accessibility info
   */
  protected boolean implementsAccessibility() {
    return false;
  }

  /**
   * Call this to transfer the {@link com.facebook.litho.annotations.State} annotated values
   * between two {@link Component} with the same global scope.
   */
  protected void transferState(
      ComponentContext c,
      StateContainer previousStateContainer,
      Component component) {
  }

  protected void createInitialState(ComponentContext c, Component component) {

  }

  @Override
  public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    // Do nothing by default.
    return null;
  }

  @Override
  @Nullable
  public Object acceptTriggerEvent(EventTrigger eventTrigger, Object eventState, Object[] params) {
    // Do nothing by default
    return null;
  }

  protected boolean isPureRender() {
    return false;
  }

  protected boolean callsShouldUpdateOnMount() {
    return false;
  }

  /**
   * @return true if Mount uses @FromMeasure or @FromOnBoundsDefined parameters.
   */
  protected boolean isMountSizeDependent() {
    return false;
  }

  @ThreadSafe
  protected int poolSize() {
    return DEFAULT_MAX_PREALLOCATION;
  }

  /** @return true if this component can be preallocated. */
  protected boolean canPreallocate() {
    return false;
  }

  final boolean shouldComponentUpdate(Component previous, Component next) {
    if (isPureRender()) {
      return shouldUpdate(previous, next);
    }

    return true;
  }

  /**
   * Whether the component needs updating.
   * <p>
   * For layout components, the framework will verify that none of the children of the component
   * need updating, and that both components have the same number of children. Therefore this
   * method just needs to determine any changes to the top-level component that would cause it to
   * need to be updated (for example, a click handler was added).
   * <p>
   * For mount specs, the framework does nothing extra and this method alone determines whether the
   * component is updated or not.
   * @param previous the previous component to compare against.
   * @param next the component that is now in use.
   * @return true if the component needs an update, false otherwise.
   */
  protected boolean shouldUpdate(Component previous, Component next) {
    return !previous.isEquivalentTo(next);
  }

  /**
   * @return a {@link TransitionSet} specifying how to animate this component to its new layout and
   *     props.
   */
  protected Transition onCreateTransition(ComponentContext c, Component component) {
    return null;
  }

  protected static <E> EventHandler<E> newEventHandler(
      ComponentContext c,
      String name,
      int id,
      Object[] params) {
    final EventHandler<E> eventHandler = c.newEventHandler(name, id, params);
    if (c.getComponentTree() != null) {
      c.getComponentTree().recordEventHandler(c.getComponentScope(), eventHandler);
    }

    return eventHandler;
  }

  protected static <E> EventHandler<E> newEventHandler(
      Component c,
      String name,
      int id,
      Object[] params) {
    final EventHandler<E> eventHandler = new EventHandler<>(c, name, id, params);
    if (c.getScopedContext() != null && c.getScopedContext().getComponentTree() != null) {
      c.getScopedContext().getComponentTree().recordEventHandler(c, eventHandler);
    }

    return eventHandler;
  }

  protected static <E> EventTrigger<E> newEventTrigger(
      ComponentContext c, String childKey, int id) {
    return c.newEventTrigger(childKey, id);
  }

  @Nullable
  protected static EventTrigger getEventTrigger(ComponentContext c, int id, String key) {
    if (c.getComponentScope() == null) {
      return null;
    }

    EventTrigger trigger =
        c.getComponentTree().getEventTrigger(c.getComponentScope().getGlobalKey() + id + key);

    if (trigger == null) {
      return null;
    }

    return trigger;
  }

  protected boolean needsPreviousRenderData() {
    return false;
  }

  protected RenderData recordRenderData(Component component, RenderData toRecycle) {
    return null;
  }

  protected void applyPreviousRenderData(Component component, RenderData previousRenderData) {}

  public interface StateUpdate {
    void updateState(StateContainer stateContainer, Component newComponent);
  }

  /**
   * @return true if the Component is using state, false otherwise.
   */
  protected boolean hasState() {
    return false;
  }
}
