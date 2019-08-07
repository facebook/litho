/*
 * Copyright 2014-present Facebook, Inc.
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnShouldCreateLayoutWithNewSizeSpec;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;

/**
 * {@link ComponentLifecycle} is extended by the {@link Component} class and declare methods used by
 * a {@link Component} instances to calculate their layout bounds and mount elements, among other
 * things. This is the base class from which all new component types inherit.
 */
public abstract class ComponentLifecycle implements EventDispatcher, EventTriggerTarget {
  private static final AtomicInteger sComponentTypeId = new AtomicInteger();
  private static final int DEFAULT_MAX_PREALLOCATION = 3;

  // This name needs to match the generated code in specmodels in
  // com.facebook.litho.specmodels.generator.EventCaseGenerator#INTERNAL_ON_ERROR_HANDLER_NAME.
  // Since we cannot easily share this identifier across modules, we verify the consistency through
  // integration tests.
  static final int ERROR_EVENT_HANDLER_ID = "__internalOnErrorHandler".hashCode();

  public enum MountType {
    NONE,
    DRAWABLE,
    VIEW,
  }

  /**
   * Generated component's state container could implement this interface along with {@link
   * StateContainer} when componentspec specifies state update method with {@link
   * com.facebook.litho.annotations.OnUpdateStateWithTransition} annotation.
   */
  public interface TransitionContainer {

    /** Remove and return all transitions collected from state updates with transitions. */
    List<Transition> consumeTransitions();
  }

  /**
   * A per-Component-class data structure to keep track of some of the last mounted @Prop/@State
   * params a component was rendered with. The exact params that are tracked are just the ones
   * needed to support that Component's use of {@link Diff} params in their lifecycle methods that
   * allow Diff params (e.g. {@link #onCreateTransition}).
   */
  public interface RenderData {}

  private static final YogaBaselineFunction sBaselineFunction =
      new YogaBaselineFunction() {
        @Override
        public float baseline(YogaNode cssNode, float width, float height) {
          final InternalNode node = (InternalNode) cssNode.getData();
          return node.getTailComponent()
              .onMeasureBaseline(node.getContext(), (int) width, (int) height);
        }
      };

  static final YogaMeasureFunction sMeasureFunction =
      new YogaMeasureFunction() {

        private Size acquireSize(int initialValue) {
          return new Size(initialValue, initialValue);
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
          final Component component = node.getTailComponent();
          final int widthSpec;
          final int heightSpec;
          final boolean isTracing = ComponentsSystrace.isTracing();

          widthSpec = SizeSpec.makeSizeSpecFromCssSpec(width, widthMode);
          heightSpec = SizeSpec.makeSizeSpecFromCssSpec(height, heightMode);

          if (isTracing) {
            ComponentsSystrace.beginSectionWithArgs("measure:" + component.getSimpleName())
                .arg("widthSpec", SizeSpec.toString(widthSpec))
                .arg("heightSpec", SizeSpec.toString(heightSpec))
                .arg("componentId", component.getId())
                .flush();
          }

          node.setLastWidthSpec(widthSpec);
          node.setLastHeightSpec(heightSpec);

          int outputWidth = 0;
          int outputHeight = 0;

          if (Component.isNestedTree(component) || node.hasNestedTree()) {

            ComponentContext context = node.getContext();

            // TODO: (T39009736) evaluate why the parent is null sometimes
            if (context.isReconciliationEnabled()) {
              if (node.getParent() != null) {
                context = node.getParent().getContext();
              } else if (context.getLogger() != null) {
                context
                    .getLogger()
                    .emitMessage(
                        ComponentsLogger.LogLevel.ERROR,
                        "component "
                            + component.getSimpleName()
                            + " is a nested tree but does not have a parent component."
                            + "[mGlobalKey:"
                            + component.getGlobalKey()
                            + "]");
              }
            }

            final InternalNode nestedTree =
                LayoutState.resolveNestedTree(context, node, widthSpec, heightSpec);

            outputWidth = nestedTree.getWidth();
            outputHeight = nestedTree.getHeight();
          } else if (diffNode != null
              && diffNode.getLastWidthSpec() == widthSpec
              && diffNode.getLastHeightSpec() == heightSpec
              && !component.shouldAlwaysRemeasure()) {
            outputWidth = (int) diffNode.getLastMeasuredWidth();
            outputHeight = (int) diffNode.getLastMeasuredHeight();
          } else {
            final Size size = acquireSize(Integer.MIN_VALUE /* initialValue */);

            component.onMeasure(component.getScopedContext(), node, widthSpec, heightSpec, size);

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
          }

          node.setLastMeasuredWidth(outputWidth);
          node.setLastMeasuredHeight(outputHeight);

          if (isTracing) {
            ComponentsSystrace.endSection();
          }

          return YogaMeasureOutput.make(outputWidth, outputHeight);
        }
      };

  @GuardedBy("sTypeIdByComponentType")
  private static final Map<Object, Integer> sTypeIdByComponentType = new HashMap<>();

  private final int mTypeId;

  ComponentLifecycle() {
    this(null);
  }

  /**
   * This constructor should be called only if working with a manually crafted special Component.
   * This should NOT be used in general use cases.
   */
  protected ComponentLifecycle(Object type) {
    if (type == null) {
      type = getClass();
    }

    synchronized (sTypeIdByComponentType) {
      if (!sTypeIdByComponentType.containsKey(type)) {
        sTypeIdByComponentType.put(type, sComponentTypeId.incrementAndGet());
      }

      mTypeId = sTypeIdByComponentType.get(type);
    }
  }

  int getTypeId() {
    return mTypeId;
  }

  @ThreadSafe(enableChecks = false)
  public Object createMountContent(Context c) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createMountContent:" + ((Component) this).getSimpleName());
    }
    try {
      return onCreateMountContent(c);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  void mount(ComponentContext c, Object convertContent) {
    c.enterNoStateUpdatesMethod("mount");

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onMount:" + ((Component) this).getSimpleName());
    }
    try {
      onMount(c, convertContent);
    } catch (Exception e) {
      c.exitNoStateUpdatesMethod();
      dispatchErrorEvent(c, e);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    c.exitNoStateUpdatesMethod();
  }

  void bind(ComponentContext c, Object mountedContent) {
    c.enterNoStateUpdatesMethod("bind");

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onBind:" + ((Component) this).getSimpleName());
    }
    try {
      onBind(c, mountedContent);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    c.exitNoStateUpdatesMethod();
  }

  void unbind(ComponentContext c, Object mountedContent) {
    onUnbind(c, mountedContent);
  }

  void unmount(ComponentContext c, Object mountedContent) {
    onUnmount(c, mountedContent);
  }

  /**
   * Create a layout from the given component.
   *
   * @param context ComponentContext associated with the current ComponentTree.
   * @param resolveNestedTree if the component's layout tree should be resolved as part of this
   *     call.
   * @return New InternalNode associated with the given component.
   */
  InternalNode createLayout(ComponentContext context, boolean resolveNestedTree) {
    final Component component = (Component) this;
    final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();

    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender;
    }

    final boolean deferNestedTreeResolution =
        Component.isNestedTree((Component) this) && !resolveNestedTree;

    final TreeProps parentTreeProps = context.getTreeProps();
    context.setTreeProps(getTreePropsForChildren(context, parentTreeProps));

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + ((Component) this).getSimpleName());
    }

    InternalNode node;
    try {
      if (deferNestedTreeResolution) {
        node = InternalNodeUtils.create(context);
        node.markIsNestedTreeHolder(context.getTreeProps());
      } else if (component.canResolve()) {
        context.setTreeProps(component.getScopedContext().getTreePropsCopy());
        node = (InternalNode) component.resolve(context);
      } else {
        if (ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled
            && Component.isMountSpec(component)) {
          // create a blank InternalNode for MountSpecs
          node = context.newLayoutBuilder(0, 0);
        } else {

          // create the component's layout
          final Component root = createComponentLayout(context);

          // resolve the layout into an InternalNode
          if (root == null || root.getId() <= 0) {
            node = null;
          } else {
            node = context.resolveLayout(root);

            // if the root is a layout spec which can resolve itself add it to the InternalNode
            if (ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled
                && Component.isLayoutSpec(root)
                && root.canResolve()) {
              node.appendComponent(root);
            }
          }
        }
      }
    } catch (Throwable t) {
      throw new ComponentsChainException((Component) this, t);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (node == null || node == ComponentContext.NULL_LAYOUT) {
      return ComponentContext.NULL_LAYOUT;
    }

    // If this is a layout spec with size spec, and we're not deferring the nested tree resolution,
    // then we already added the props earlier on (when we did defer resolution), and
    // therefore we shouldn't add them again here.
    final CommonPropsCopyable commonProps = ((Component) this).getCommonPropsCopyable();
    if (commonProps != null
        && (deferNestedTreeResolution || !Component.isLayoutSpecWithSizeSpec((Component) this))) {
      commonProps.copyInto(context, node);
    }

    // Set component on the root node of the generated tree so that the mount calls use
    // those (see Controller.mountNodeTree()). Handle the case where the component simply
    // delegates its layout creation to another component i.e. the root node belongs to
    // another component.
    if (node.getTailComponent() == null) {
      final boolean isMountSpecWithMeasure =
          canMeasure() && Component.isMountSpec((Component) this);

      if (isMountSpecWithMeasure || deferNestedTreeResolution) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    node.appendComponent((Component) this);
    if (TransitionUtils.areTransitionsEnabled(context.getAndroidContext())) {
      if (needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData((Component) this);
      } else {
        final Transition transition = createTransition(context);
        if (transition != null) {
          node.addTransition(transition);
        }
      }
    }

    if (!deferNestedTreeResolution) {
      onPrepare(context);
    }

    if (component.mWorkingRangeRegistrations != null
        && !component.mWorkingRangeRegistrations.isEmpty()) {
      node.addWorkingRanges(component.mWorkingRangeRegistrations);
    }

    return node;
  }

  final @Nullable Transition createTransition(ComponentContext c) {
    final Transition transition = onCreateTransition(c);
    if (transition != null) {
      TransitionUtils.setOwnerKey(transition, ((Component) this).getGlobalKey());
    }
    return transition;
  }

  @ThreadSafe(enableChecks = false)
  Component createComponentLayout(ComponentContext c) {
    Component layoutComponent = null;

    try {
      if (Component.isLayoutSpecWithSizeSpec(((Component) this))) {
        layoutComponent = onCreateLayoutWithSizeSpec(c, c.getWidthSpec(), c.getHeightSpec());
      } else {
        layoutComponent = onCreateLayout(c);
      }
    } catch (Exception e) {
      dispatchErrorEvent(c, e);
    }

    return layoutComponent;
  }

  /**
   * Reraise an error event up the hierarchy so it can be caught by another component, or reach the
   * root and cause the application to crash.
   *
   * @param c The component context the error event was caught in.
   * @param e The original exception.
   */
  public static void dispatchErrorEvent(ComponentContext c, Exception e) {
    if (ComponentsConfiguration.enableOnErrorHandling) {
      final ErrorEvent errorEvent = new ErrorEvent();
      errorEvent.exception = e;

      dispatchErrorEvent(c, errorEvent);
    } else {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * For internal use, only. Use {@link #dispatchErrorEvent(ComponentContext, Exception)} instead.
   */
  public static void dispatchErrorEvent(ComponentContext c, ErrorEvent e) {
    final EventHandler<ErrorEvent> errorHandler = c.getComponentScope().getErrorHandler();

    // TODO(T26533980): This check is only necessary as long as we have the configuration flag as
    //                  the enabled state could theoretically change at runtime.
    if (errorHandler != null) {
      errorHandler.dispatchEvent(e);
    }
  }

  void loadStyle(ComponentContext c, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    c.setDefStyle(defStyleAttr, defStyleRes);
    onLoadStyle(c);
    c.setDefStyle(0, 0);
  }

  void loadStyle(ComponentContext c) {
    onLoadStyle(c);
  }

  /**
   * Retrieves all of the tree props used by this Component from the TreeProps map and sets the tree
   * props as fields on the ComponentImpl.
   */
  protected void populateTreeProps(TreeProps parentTreeProps) {}

  /** Updates the TreeProps map with outputs from all {@link OnCreateTreeProp} methods. */
  protected TreeProps getTreePropsForChildren(ComponentContext c, TreeProps previousTreeProps) {
    return previousTreeProps;
  }

  /**
   * Generate a tree of {@link ComponentLayout} representing the layout structure of the {@link
   * Component} and its sub-components. You should use {@link ComponentContext#newLayoutBuilder} to
   * build the layout tree.
   *
   * @param c The {@link ComponentContext} to build a {@link ComponentLayout} tree.
   */
  protected Component onCreateLayout(ComponentContext c) {
    return Column.create(c).build();
  }

  protected Component onCreateLayoutWithSizeSpec(
      ComponentContext c, int widthSpec, int heightSpec) {
    return Column.create(c).build();
  }

  /** Resolves the {@link ComponentLayout} for the given {@link Component}. */
  protected ComponentLayout resolve(ComponentContext c) {
    if (ComponentsConfiguration.isRefactoredLayoutCreationEnabled) {
      return LayoutState.createLayout(c, (Component) this, false);
    } else {
      return createLayout(c, false);
    }
  }

  protected void onPrepare(ComponentContext c) {
    // do nothing, by default
  }

  protected void onLoadStyle(ComponentContext c) {}

  /**
   * Called after the layout calculation is finished and the given {@link ComponentLayout} has its
   * bounds defined. You can use {@link ComponentLayout#getX()}, {@link ComponentLayout#getY()},
   * {@link ComponentLayout#getWidth()}, and {@link ComponentLayout#getHeight()} to get the size and
   * position of the component in the layout tree.
   *
   * @param c The {@link Context} used by this component.
   * @param layout The {@link ComponentLayout} with defined position and size.
   */
  protected void onBoundsDefined(ComponentContext c, ComponentLayout layout) {}

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
   * Whether this {@link ComponentLifecycle} is able to measure itself according to specific size
   * constraints.
   */
  protected boolean canMeasure() {
    return false;
  }

  /**
   * @return {@code true} iff the {@link LayoutSpec} implements {@link
   *     OnShouldCreateLayoutWithNewSizeSpec} to {@code true}.
   */
  protected boolean isLayoutSpecWithSizeSpecCheck() {
    return false;
  }

  protected void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    throw new IllegalStateException(
        "You must override onMeasure() if you return true in canMeasure(), "
            + "ComponentLifecycle is: "
            + this);
  }

  /**
   * Whether this {@link ComponentLifecycle} mounts views that contain component-based content that
   * can be incrementally mounted e.g. if the mounted view has a LithoView with incremental mount
   * enabled.
   */
  protected boolean hasChildLithoViews() {
    return false;
  }

  /** Whether this drawable mount spec should cache its drawing in a display list. */
  protected boolean shouldUseDisplayList() {
    return false;
  }

  /**
   * Create the object that will be mounted in the {@link LithoView}.
   *
   * @param context The {@link Context} to be used to create the content.
   * @return an Object that can be mounted for this component.
   */
  protected Object onCreateMountContent(Context context) {
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
   */
  protected void onMount(ComponentContext c, Object convertContent) {
    // Do nothing by default.
  }

  /**
   * Unload UI elements associated with this component.
   *
   * @param c The {@link Context} for this mount operation.
   * @param mountedContent The {@link Drawable} or {@link View} mounted by this component.
   */
  protected void onUnmount(ComponentContext c, Object mountedContent) {
    // Do nothing by default.
  }

  protected void onBind(ComponentContext c, Object mountedContent) {
    // Do nothing by default.
  }

  protected void onUnbind(ComponentContext c, Object mountedContent) {
    // Do nothing by default.
  }

  /**
   * This indicates the type of the {@link Object} that will be returned by {@link
   * ComponentLifecycle#mount}.
   *
   * @return one of {@link ComponentLifecycle.MountType}
   */
  public MountType getMountType() {
    return MountType.NONE;
  }

  /**
   * Populate an accessibility node with information about the component.
   *
   * @param accessibilityNode node to populate
   */
  protected void onPopulateAccessibilityNode(
      View host, AccessibilityNodeInfoCompat accessibilityNode) {}

  /**
   * Populate an extra accessibility node.
   *
   * @param accessibilityNode node to populate
   * @param extraNodeIndex index of extra node
   * @param componentBoundsX left bound of the mounted component
   * @param componentBoundsY top bound of the mounted component
   */
  protected void onPopulateExtraAccessibilityNode(
      AccessibilityNodeInfoCompat accessibilityNode,
      int extraNodeIndex,
      int componentBoundsX,
      int componentBoundsY) {}

  /**
   * Get extra accessibility node id at a given point within the component.
   *
   * @param x x co-ordinate within the mounted component
   * @param y y co-ordinate within the mounted component
   * @return the extra virtual view id if one is found, otherwise {@code
   *     ExploreByTouchHelper#INVALID_ID}
   */
  protected int getExtraAccessibilityNodeAt(int x, int y) {
    return ExploreByTouchHelper.INVALID_ID;
  }

  /**
   * The number of extra accessibility nodes that this component wishes to provides to the
   * accessibility system.
   *
   * @return the number of extra nodes
   */
  protected int getExtraAccessibilityNodesCount() {
    return 0;
  }

  /**
   * Whether this component will expose any virtual views to the accessibility framework
   *
   * @return true if the component exposes extra accessibility nodes
   */
  protected boolean implementsExtraAccessibilityNodes() {
    return false;
  }

  /**
   * Whether this component will populate any accessibility nodes or events that are passed to it.
   *
   * @return true if the component implements accessibility info
   */
  protected boolean implementsAccessibility() {
    return false;
  }

  /**
   * Call this to transfer the {@link com.facebook.litho.annotations.State} annotated values between
   * two {@link Component} with the same global scope.
   */
  protected void transferState(
      StateContainer previousStateContainer, StateContainer nextStateContainer) {}

  protected void createInitialState(ComponentContext c) {}

  /**
   * Called to provide a fallback if a supported lifecycle method throws an exception. It is
   * possible to either recover from the error here or reraise the exception to catch it at a higher
   * level or crash the application.
   *
   * @see com.facebook.litho.annotations.OnError
   * @param c The {@link ComponentContext} the Component was constructed with.
   * @param e The exception caught.
   */
  protected void onError(ComponentContext c, Exception e) {
    throw new RuntimeException(e);
  }

  @Override
  public @Nullable Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    if (ComponentsConfiguration.enableOnErrorHandling
        && eventHandler.id == ERROR_EVENT_HANDLER_ID) {
      ((Component) this).getErrorHandler().dispatchEvent(((ErrorEvent) eventState));
    }

    // Don't do anything by default, unless we're handling an error.
    return null;
  }

  @Override
  @Nullable
  public Object acceptTriggerEvent(EventTrigger eventTrigger, Object eventState, Object[] params) {
    // Do nothing by default
    return null;
  }

  protected void dispatchOnEnteredRange(String name) {
    // Do nothing by default
  }

  protected void dispatchOnExitedRange(String name) {
    // Do nothing by default
  }

  protected boolean isPureRender() {
    return false;
  }

  protected boolean callsShouldUpdateOnMount() {
    return false;
  }

  /** @return true if Mount uses @FromMeasure or @FromOnBoundsDefined parameters. */
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
   *
   * <p>For layout components, the framework will verify that none of the children of the component
   * need updating, and that both components have the same number of children. Therefore this method
   * just needs to determine any changes to the top-level component that would cause it to need to
   * be updated (for example, a click handler was added).
   *
   * <p>For mount specs, the framework does nothing extra and this method alone determines whether
   * the component is updated or not.
   *
   * @param previous the previous component to compare against.
   * @param next the component that is now in use.
   * @return true if the component needs an update, false otherwise.
   */
  protected boolean shouldUpdate(Component previous, Component next) {
    return !previous.isEquivalentTo(next);
  }

  boolean canUsePreviousLayout(ComponentContext context) {
    return ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec
        && !onShouldCreateLayoutWithNewSizeSpec(
            context, context.getWidthSpec(), context.getHeightSpec());
  }

  protected boolean onShouldCreateLayoutWithNewSizeSpec(
      ComponentContext context, int newWidthSpec, int newHeightSpec) {
    return true;
  }

  /**
   * @return a {@link TransitionSet} specifying how to animate this component to its new layout and
   *     props.
   */
  protected @Nullable Transition onCreateTransition(ComponentContext c) {
    return null;
  }

  protected static <E> EventHandler<E> newEventHandler(
      ComponentContext c, int id, Object[] params) {
    final EventHandler<E> eventHandler = c.newEventHandler(id, params);
    if (c.getComponentTree() != null) {
      c.getComponentTree().recordEventHandler(c.getComponentScope(), eventHandler);
    }

    return eventHandler;
  }

  protected static <E> EventHandler<E> newEventHandler(Component c, int id, Object[] params) {
    final EventHandler<E> eventHandler = new EventHandler<>(c, id, params);
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

  protected @Nullable RenderData recordRenderData(RenderData toRecycle) {
    return null;
  }

  protected void applyPreviousRenderData(RenderData previousRenderData) {}

  /** @return true if the Component is using state, false otherwise. */
  protected boolean hasState() {
    return false;
  }

  /**
   * @return true if the Component should always be measured when receiving a remeasure event, false
   *     otherwise.
   */
  protected boolean shouldAlwaysRemeasure() {
    return false;
  }

  /**
   * Called when the component is attached to the {@link ComponentTree}.
   *
   * @param c The {@link ComponentContext} the Component was constructed with.
   */
  protected void onAttached(ComponentContext c) {}

  /**
   * Called when the component is detached from the {@link ComponentTree}.
   *
   * @param c The {@link ComponentContext} the Component was constructed with.
   */
  protected void onDetached(ComponentContext c) {}

  /**
   * @return true if the component implements {@link OnAttached} or {@link OnDetached} delegate
   *     methods.
   */
  protected boolean hasAttachDetachCallback() {
    return false;
  }

  /**
   * Exception class used to print the Components' hierarchy involved in a layout creation crash.
   */
  private static class CreateLayoutException extends RuntimeException {
    CreateLayoutException(Component c, Throwable cause) {
      super(c.getSimpleName());
      initCause(cause);
      setStackTrace(new StackTraceElement[0]);
    }
  }
}
