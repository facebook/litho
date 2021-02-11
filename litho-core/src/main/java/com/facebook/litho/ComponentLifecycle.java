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
import com.facebook.rendercore.transitions.TransitionUtils;
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
  static final String WRONG_CONTEXT_FOR_EVENT_HANDLER =
      "ComponentLifecycle:WrongContextForEventHandler";
  private static final @Nullable YogaMeasureFunction sMeasureFunction =
      ComponentsConfiguration.useStatelessComponent ? null : new LithoYogaMeasureFunction(null);

  private static final int DEFAULT_MAX_PREALLOCATION = 3;
  private static final YogaBaselineFunction sBaselineFunction = new LithoYogaBaselineFunction();

  @GuardedBy("sTypeIdByComponentType")
  private static final Map<Object, Integer> sTypeIdByComponentType = new HashMap<>();

  private static final AtomicInteger sComponentTypeId = new AtomicInteger();
  private final int mTypeId;

  /**
   * @return the globally unique ID associated with {@param type}, creating one if necessary.
   *     Allocated IDs map 1-to-1 with objects passed to this method.
   */
  private static int getOrCreateId(Object type) {
    synchronized (sTypeIdByComponentType) {
      if (!sTypeIdByComponentType.containsKey(type)) {
        sTypeIdByComponentType.put(type, sComponentTypeId.incrementAndGet());
      }

      //noinspection ConstantConditions
      return sTypeIdByComponentType.get(type);
    }
  }

  static YogaMeasureFunction getYogaMeasureFunction(
      @Nullable LayoutStateContext layoutStateContext) {
    if (ComponentsConfiguration.useStatelessComponent) {
      return layoutStateContext.getLithoYogaMeasureFunction();
    }

    return sMeasureFunction;
  }

  ComponentLifecycle() {
    mTypeId = getOrCreateId(getClass());
  }

  /**
   * This constructor should be called only if working with a manually crafted special Component.
   * This should NOT be used in general use cases.
   */
  ComponentLifecycle(int identityHashCode) {
    mTypeId = getOrCreateId(identityHashCode);
  }

  @Override
  @Nullable
  public final Object acceptTriggerEvent(
      EventTrigger eventTrigger, Object eventState, Object[] params) {
    try {
      return acceptTriggerEventImpl(eventTrigger, eventState, params);
    } catch (Exception e) {
      if (eventTrigger.mComponentContext != null) {
        throw ComponentUtils.wrapWithMetadata(eventTrigger.mComponentContext, e);
      } else {
        throw e;
      }
    }
  }

  protected @Nullable Object acceptTriggerEventImpl(
      EventTrigger eventTrigger, Object eventState, Object[] params) {
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
  public final @Nullable Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    // We don't want to wrap and throw error events
    if (eventHandler.id == ERROR_EVENT_HANDLER_ID) {
      return dispatchOnEventImpl(eventHandler, eventState);
    }

    try {
      return dispatchOnEventImpl(eventHandler, eventState);
    } catch (Exception e) {
      if (eventHandler.params != null && eventHandler.params[0] instanceof ComponentContext) {
        throw ComponentUtils.wrapWithMetadata((ComponentContext) eventHandler.params[0], e);
      } else {
        throw e;
      }
    }
  }

  protected @Nullable Object dispatchOnEventImpl(EventHandler eventHandler, Object eventState) {
    if (eventHandler.id == ERROR_EVENT_HANDLER_ID) {
      getErrorHandler().dispatchEvent((ErrorEvent) eventState);
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
    } catch (Exception e) {
      ComponentUtils.handle(c, e);
    } finally {
      c.exitNoStateUpdatesMethod();
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  boolean canUsePreviousLayout(ComponentContext context) {
    return ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec
        && !onShouldCreateLayoutWithNewSizeSpec(
            context, context.getWidthSpec(), context.getHeightSpec());
  }

  @Nullable
  @ThreadSafe(enableChecks = false)
  Component createComponentLayout(ComponentContext c) {
    Component layoutComponent = null;

    if (Component.isLayoutSpecWithSizeSpec(((Component) this))) {
      layoutComponent = onCreateLayoutWithSizeSpec(c, c.getWidthSpec(), c.getHeightSpec());
    } else {
      layoutComponent = onCreateLayout(c);
    }

    return layoutComponent;
  }

  final @Nullable Transition createTransition(ComponentContext c) {
    final Transition transition = onCreateTransition(c);
    if (transition != null) {
      TransitionUtils.setOwnerKey(transition, Component.getGlobalKey(c, ((Component) this)));
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
      ComponentUtils.handle(c, e);
    } finally {
      c.exitNoStateUpdatesMethod();
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  final boolean shouldComponentUpdate(
      ComponentContext previousScopedContext,
      Component previous,
      ComponentContext nextScopedContext,
      Component next) {
    if (isPureRender()) {
      return shouldUpdate(previousScopedContext, previous, nextScopedContext, next);
    }

    return true;
  }

  void unbind(ComponentContext c, Object mountedContent) {
    try {
      onUnbind(c, mountedContent);
    } catch (Exception e) {
      ComponentUtils.handle(c, e);
    }
  }

  void unmount(ComponentContext c, Object mountedContent) {
    try {
      onUnmount(c, mountedContent);
    } catch (Exception e) {
      ComponentUtils.handle(c, e);
    }
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

  protected void dispatchOnEnteredRange(ComponentContext c, String name) {
    // Do nothing by default
  }

  protected void dispatchOnExitedRange(ComponentContext c, String name) {
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
    EventHandler<ErrorEvent> eventHandler = c.getErrorEventHandler();
    if (eventHandler == null) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
    ErrorEvent errorEvent = new ErrorEvent();
    errorEvent.exception = e;
    eventHandler.dispatchEvent(errorEvent);
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

  protected @Nullable RenderData recordRenderData(ComponentContext c, RenderData toRecycle) {
    return null;
  }

  /** Resolves the {@link ComponentLayout} for the given {@link Component}. */
  protected InternalNode resolve(ComponentContext c) {
    return Layout.create(c, (Component) this, false);
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
  protected boolean shouldUpdate(
      ComponentContext previousScopedContext,
      Component previous,
      ComponentContext nextScopedContext,
      Component next) {
    final StateContainer prevStateContainer =
        previous == null ? null : previous.getStateContainer(previousScopedContext);
    final StateContainer nextStateContainer =
        next == null ? null : next.getStateContainer(nextScopedContext);
    return !previous.isEquivalentTo(next)
        || !ComponentUtils.hasEquivalentState(prevStateContainer, nextStateContainer);
  }

  /**
   * Call this to transfer the {@link com.facebook.litho.annotations.State} annotated values between
   * two {@link Component} with the same global scope.
   */
  protected void transferState(
      StateContainer previousStateContainer, StateContainer nextStateContainer) {}

  /**
   * For internal use, only. In order to reraise an error event up the hierarchy use {@link
   * ComponentUtils#raise(ComponentContext, Exception)} instead.
   */
  protected static void dispatchErrorEvent(ComponentContext c, Exception e) {
    ComponentUtils.dispatchErrorEvent(c, e);
  }

  /** For internal use, only. */
  public static void dispatchErrorEvent(ComponentContext c, ErrorEvent e) {
    ComponentUtils.dispatchErrorEvent(c, e);
  }

  protected abstract @Nullable EventHandler<ErrorEvent> getErrorHandler();

  @Nullable
  protected static EventTrigger getEventTrigger(ComponentContext c, int id, String key) {
    if (c.getComponentScope() == null) {
      return null;
    }

    return c.getComponentTree().getEventTrigger(c.getGlobalKey() + id + key);
  }

  @Nullable
  protected static EventTrigger getEventTrigger(ComponentContext c, int id, Handle handle) {
    if (c.getComponentTree() == null) {
      return null;
    }

    EventTrigger trigger = c.getComponentTree().getEventTrigger(handle, id);

    if (trigger == null) {
      return null;
    }

    return trigger;
  }

  /**
   * This method is overridden in the generated component to return true if and only if the
   * Component Spec has an OnError lifecycle callback.
   */
  protected boolean hasOwnErrorHandler() {
    return false;
  }

  protected static <E> EventHandler<E> newEventHandler(
      final Class<? extends Component> reference,
      final String className,
      final ComponentContext c,
      final int id,
      final Object[] params) {
    if (c == null || c.getComponentScope() == null) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.FATAL,
          NO_SCOPE_EVENT_HANDLER,
          "Creating event handler without scope.");
      return NoOpEventHandler.getNoOpEventHandler();
    } else if (reference != c.getComponentScope().getClass()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          WRONG_CONTEXT_FOR_EVENT_HANDLER + ":" + c.getComponentScope().getSimpleName(),
          String.format(
              "A Event handler from %s was created using a context from %s. "
                  + "Event Handlers must be created using a ComponentContext from its Component.",
              className, c.getComponentScope().getSimpleName()));
    }
    final EventHandler<E> eventHandler = c.newEventHandler(id, params);
    if (c.getComponentTree() != null) {
      c.getComponentTree().recordEventHandler(c, eventHandler);
    }

    return eventHandler;
  }

  /* TODO: (T81557408) Fix @Nullable issue. */
  protected static <E> EventTrigger<E> newEventTrigger(
      ComponentContext c, String childKey, int id, @Nullable Handle handle) {
    return c.newEventTrigger(childKey, id, handle);
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
}
