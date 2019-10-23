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

import static com.facebook.litho.ComponentContext.NO_SCOPE_EVENT_HANDLER;

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;

/**
 * {@link ComponentLifecycle} is extended by the {@link Component} class and declare methods used by
 * a {@link Component} instances to calculate their layout bounds and mount elements, among other
 * things. This is the base class from which all new component types inherit.
 */
public abstract class ComponentLifecycle implements EventDispatcher, EventTriggerTarget {
  // This name needs to match the generated code in specmodels in
  // com.facebook.litho.specmodels.generator.EventCaseGenerator#INTERNAL_ON_ERROR_HANDLER_NAME.
  // Since we cannot easily share this identifier across modules, we verify the consistency through
  // integration tests.
  static final int ERROR_EVENT_HANDLER_ID = "__internalOnErrorHandler".hashCode();
  static final YogaMeasureFunction sMeasureFunction = new LithoYogaMeasureFunction();
  private static final AtomicInteger sComponentTypeId = new AtomicInteger();
  private static final int DEFAULT_MAX_PREALLOCATION = 3;
  private static final YogaBaselineFunction sBaselineFunction = new LithoYogaBaselineFunction();

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
  ComponentLifecycle(Object type) {
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

  @Override
  @Nullable
  public Object acceptTriggerEvent(EventTrigger eventTrigger, Object eventState, Object[] params) {
    // Do nothing by default
    return null;
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

  @Override
  public @Nullable Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    if (ComponentsConfiguration.enableOnErrorHandling
        && eventHandler.id == ERROR_EVENT_HANDLER_ID) {
      ((Component) this).getErrorHandler().dispatchEvent(((ErrorEvent) eventState));
    }

    // Don't do anything by default, unless we're handling an error.
    return null;
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

  boolean canUsePreviousLayout(ComponentContext context) {
    return ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec
        && !onShouldCreateLayoutWithNewSizeSpec(
            context, context.getWidthSpec(), context.getHeightSpec());
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

  final @Nullable Transition createTransition(ComponentContext c) {
    final Transition transition = onCreateTransition(c);
    if (transition != null) {
      TransitionUtils.setOwnerKey(transition, ((Component) this).getGlobalKey());
    }
    return transition;
  }

  int getTypeId() {
    return mTypeId;
  }

  void loadStyle(ComponentContext c, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    c.setDefStyle(defStyleAttr, defStyleRes);
    onLoadStyle(c);
    c.setDefStyle(0, 0);
  }

  void loadStyle(ComponentContext c) {
    onLoadStyle(c);
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

  final boolean shouldComponentUpdate(Component previous, Component next) {
    if (isPureRender()) {
      return shouldUpdate(previous, next);
    }

    return true;
  }

  void unbind(ComponentContext c, Object mountedContent) {
    onUnbind(c, mountedContent);
  }

  void unmount(ComponentContext c, Object mountedContent) {
    onUnmount(c, mountedContent);
  }

  protected void applyPreviousRenderData(RenderData previousRenderData) {}

  protected boolean callsShouldUpdateOnMount() {
    return false;
  }

  /**
   * Whether this {@link ComponentLifecycle} is able to measure itself according to specific size
   * constraints.
   */
  protected boolean canMeasure() {
    return false;
  }

  /** @return true if this component can be preallocated. */
  protected boolean canPreallocate() {
    return false;
  }

  protected void createInitialState(ComponentContext c) {}

  protected void dispatchOnEnteredRange(String name) {
    // Do nothing by default
  }

  protected void dispatchOnExitedRange(String name) {
    // Do nothing by default
  }

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

  /** Updates the TreeProps map with outputs from all {@link OnCreateTreeProp} methods. */
  protected @Nullable TreeProps getTreePropsForChildren(
      ComponentContext c, @Nullable TreeProps treeProps) {
    return treeProps;
  }

  /**
   * @return true if the component implements {@link OnAttached} or {@link OnDetached} delegate
   *     methods.
   */
  protected boolean hasAttachDetachCallback() {
    return false;
  }

  /**
   * Whether this {@link ComponentLifecycle} mounts views that contain component-based content that
   * can be incrementally mounted e.g. if the mounted view has a LithoView with incremental mount
   * enabled.
   */
  protected boolean hasChildLithoViews() {
    return false;
  }

  /** @return true if the Component is using state, false otherwise. */
  protected boolean hasState() {
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
   * Whether this component will expose any virtual views to the accessibility framework
   *
   * @return true if the component exposes extra accessibility nodes
   */
  protected boolean implementsExtraAccessibilityNodes() {
    return false;
  }

  /**
   * @return {@code true} iff the {@link LayoutSpec} implements {@link
   *     OnShouldCreateLayoutWithNewSizeSpec} to {@code true}.
   */
  protected boolean isLayoutSpecWithSizeSpecCheck() {
    return false;
  }

  /** @return true if Mount uses @FromMeasure or @FromOnBoundsDefined parameters. */
  protected boolean isMountSizeDependent() {
    return false;
  }

  protected boolean isPureRender() {
    return false;
  }

  protected boolean needsPreviousRenderData() {
    return false;
  }

  /**
   * Called when the component is attached to the {@link ComponentTree}.
   *
   * @param c The {@link ComponentContext} the Component was constructed with.
   */
  protected void onAttached(ComponentContext c) {}

  protected void onBind(ComponentContext c, Object mountedContent) {
    // Do nothing by default.
  }

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
   * Generate a tree of {@link ComponentLayout} representing the layout structure of the {@link
   * Component} and its sub-components.
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
   * @return a {@link TransitionSet} specifying how to animate this component to its new layout and
   *     props.
   */
  protected @Nullable Transition onCreateTransition(ComponentContext c) {
    return null;
  }

  /**
   * Called when the component is detached from the {@link ComponentTree}.
   *
   * @param c The {@link ComponentContext} the Component was constructed with.
   */
  protected void onDetached(ComponentContext c) {}

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

  protected void onLoadStyle(ComponentContext c) {}

  protected void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    throw new IllegalStateException(
        "You must override onMeasure() if you return true in canMeasure(), "
            + "ComponentLifecycle is: "
            + this);
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

  protected void onPrepare(ComponentContext c) {
    // do nothing, by default
  }

  protected boolean onShouldCreateLayoutWithNewSizeSpec(
      ComponentContext context, int newWidthSpec, int newHeightSpec) {
    return true;
  }

  protected void onUnbind(ComponentContext c, Object mountedContent) {
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

  @ThreadSafe
  protected int poolSize() {
    return DEFAULT_MAX_PREALLOCATION;
  }

  /**
   * Retrieves all of the tree props used by this Component from the TreeProps map and sets the tree
   * props as fields on the ComponentImpl.
   */
  protected void populateTreeProps(@Nullable TreeProps parentTreeProps) {}

  protected @Nullable RenderData recordRenderData(RenderData toRecycle) {
    return null;
  }

  /** Resolves the {@link ComponentLayout} for the given {@link Component}. */
  protected ComponentLayout resolve(ComponentContext c) {
    return LayoutState.createLayout(c, (Component) this, false);
  }

  /**
   * @return true if the Component should always be measured when receiving a remeasure event, false
   *     otherwise.
   */
  protected boolean shouldAlwaysRemeasure() {
    return false;
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

  /**
   * Call this to transfer the {@link com.facebook.litho.annotations.State} annotated values between
   * two {@link Component} with the same global scope.
   */
  protected void transferState(
      StateContainer previousStateContainer, StateContainer nextStateContainer) {}

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
    final Component scope = c.getComponentScope();
    if (scope == null) {
      throw new RuntimeException(
          "No component scope found for handler to throw error", e.exception);
    }
    final EventHandler<ErrorEvent> errorHandler = scope.getErrorHandler();

    // TODO(T26533980): This check is only necessary as long as we have the configuration flag as
    //                  the enabled state could theoretically change at runtime.
    if (errorHandler != null) {
      errorHandler.dispatchEvent(e);
    }
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

  protected static <E> EventHandler<E> newEventHandler(
      ComponentContext c, int id, Object[] params) {
    final EventHandler<E> eventHandler = c.newEventHandler(id, params);
    if (c.getComponentTree() != null) {
      c.getComponentTree().recordEventHandler(c.getComponentScope(), eventHandler);
    }

    return eventHandler;
  }

  protected static <E> EventHandler<E> newEventHandler(Component c, int id, Object[] params) {
    if (c == null) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          NO_SCOPE_EVENT_HANDLER,
          "Creating event handler without scope.");
    }
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

    /** Remove and return transition provided from OnUpdateStateWithTransition. */
    Transition consumeTransition();
  }

  /**
   * A per-Component-class data structure to keep track of some of the last mounted @Prop/@State
   * params a component was rendered with. The exact params that are tracked are just the ones
   * needed to support that Component's use of {@link Diff} params in their lifecycle methods that
   * allow Diff params (e.g. {@link #onCreateTransition}).
   */
  public interface RenderData {}

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
