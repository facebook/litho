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

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static androidx.annotation.Dimension.DP;
import static com.facebook.litho.ComponentContext.NO_SCOPE_EVENT_HANDLER;
import static com.facebook.litho.DynamicPropsManager.KEY_ALPHA;
import static com.facebook.litho.DynamicPropsManager.KEY_BACKGROUND_COLOR;
import static com.facebook.litho.DynamicPropsManager.KEY_BACKGROUND_DRAWABLE;
import static com.facebook.litho.DynamicPropsManager.KEY_ELEVATION;
import static com.facebook.litho.DynamicPropsManager.KEY_FOREGROUND_COLOR;
import static com.facebook.litho.DynamicPropsManager.KEY_ROTATION;
import static com.facebook.litho.DynamicPropsManager.KEY_SCALE_X;
import static com.facebook.litho.DynamicPropsManager.KEY_SCALE_Y;
import static com.facebook.litho.DynamicPropsManager.KEY_TRANSLATION_X;
import static com.facebook.litho.DynamicPropsManager.KEY_TRANSLATION_Y;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.EventHandlerRebindMode;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.LithoDebugConfigurations;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.rendercore.Equivalence;
import com.facebook.rendercore.LayoutCache;
import com.facebook.rendercore.ResourceResolver;
import com.facebook.rendercore.utils.CommonUtils;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaGutter;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.jvm.functions.Function1;

/**
 * Represents a unique instance of a component. To create new {@link Component} instances, use the
 * {@code create()} method in the generated subclass which returns a builder that allows you to set
 * values for individual props. {@link Component} instances are immutable after creation.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class Component implements Cloneable, Equivalence<Component>, AttributesAcceptor {

  // This name needs to match the generated code in specmodels in
  // com.facebook.litho.specmodels.generator.EventCaseGenerator#INTERNAL_ON_ERROR_HANDLER_NAME.
  // Since we cannot easily share this identifier across modules, we verify the consistency through
  // integration tests.
  static final int ERROR_EVENT_HANDLER_ID = "__internalOnErrorHandler".hashCode();
  static final String WRONG_CONTEXT_FOR_EVENT_HANDLER = "Component:WrongContextForEventHandler";
  static final YogaMeasureFunction sMeasureFunction = new LithoYogaMeasureFunction();

  @GuardedBy("sTypeIdByComponentType")
  private static final Map<Object, Integer> sTypeIdByComponentType = new HashMap<>();

  private static final AtomicInteger sComponentTypeId = new AtomicInteger();
  private static final String MISMATCHING_BASE_CONTEXT = "Component:MismatchingBaseContext";
  private static final String NULL_KEY_SET = "Component:NullKeySet";
  private static final AtomicInteger sIdGenerator = new AtomicInteger(1);

  /**
   * @return the globally unique ID associated with {@param type}, creating one if necessary.
   *     Allocated IDs map 1-to-1 with objects passed to this method.
   */
  static int getOrCreateId(Object type) {
    synchronized (sTypeIdByComponentType) {
      final Integer typeId = sTypeIdByComponentType.get(type);
      if (typeId != null) {
        return typeId;
      }
      final int nextTypeId = sComponentTypeId.incrementAndGet();
      sTypeIdByComponentType.put(type, nextTypeId);
      return nextTypeId;
    }
  }

  private final int mTypeId;

  private int mId = sIdGenerator.getAndIncrement();
  private @Nullable String mKey;
  private boolean mHasManualKey;
  private @Nullable Handle mHandle;

  /**
   * Holds an event handler with its dispatcher set to the parent component, or - in case that this
   * is a root component - a default handler that reraises the exception.
   */
  private @Nullable EventHandler<ErrorEvent> mErrorEventHandler;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable Context mBuilderContext;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable String mBuilderContextName;

  protected Component() {
    mTypeId = getOrCreateId(getClass());
  }

  /**
   * This constructor should be called only if working with a manually crafted "special" Component.
   * This should NOT be used in general use cases. Use the standard {@link #Component()} instead.
   */
  protected Component(int identityHashCode) {
    mTypeId = getOrCreateId(identityHashCode);
  }

  @ThreadSafe(enableChecks = false)
  public final Object createMountContent(Context c) {
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

  /**
   * This indicates the type of the {@link Object} that will be returned by {@link
   * com.facebook.litho.Component#mount}.
   *
   * @return one of {@link com.facebook.litho.Component.MountType}
   */
  public com.facebook.litho.Component.MountType getMountType() {
    return com.facebook.litho.Component.MountType.NONE;
  }

  public final int getTypeId() {
    return mTypeId;
  }

  /**
   * Whether this {@link com.facebook.litho.Component} is able to measure itself according to
   * specific size constraints.
   */
  protected boolean canMeasure() {
    return false;
  }

  protected boolean usesLocalStateContainer() {
    return false;
  }

  protected boolean implementsShouldUpdate() {
    return false;
  }

  protected boolean isPureRender() {
    return false;
  }

  /**
   * Invokes the Component-specific render implementation, returning a RenderResult. The
   * RenderResult will have the Component this Component rendered to (which will then need to be
   * render()'ed or {@link #resolve(LithoLayoutContext, ComponentContext)}'ed), as well as other
   * metadata from that render call such as transitions that should be applied.
   */
  protected RenderResult render(
      ResolveContext resolveContext, ComponentContext c, int widthSpec, int heightSpec) {
    throw new RuntimeException(
        "Render should not be called on a component which hasn't implemented render! "
            + getSimpleName());
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
   * Invokes the Component-specific resolve implementation, returning a ComponentResolveResult. The
   * ComponentResolveResult will have the {@link LithoNode} and {@link CommonProps} for the resolved
   * component.
   */
  protected ComponentResolveResult resolve(
      final ResolveContext resolveContext,
      final ScopedComponentInfo scopedComponentInfo,
      final int parentWidthSpec,
      final int parentHeightSpec,
      final @Nullable ComponentsLogger componentsLogger) {
    throw new RuntimeException(
        "resolve should not be called on a component which hasn't implemented it! "
            + getSimpleName());
  }

  protected boolean isEqualivalentTreePropContainer(
      ComponentContext current, ComponentContext next) {
    return true;
  }

  final boolean shouldComponentUpdate(
      final @Nullable ComponentContext previousScopedContext,
      Component currentComponent,
      final @Nullable ComponentContext nextScopedContext,
      Component nextComponent) {
    final boolean shouldUpdate =
        shouldUpdate(
            currentComponent,
            previousScopedContext == null
                ? null
                : previousScopedContext.getScopedComponentInfo().getStateContainer(),
            nextComponent,
            nextScopedContext == null
                ? null
                : nextScopedContext.getScopedComponentInfo().getStateContainer());

    if (!implementsShouldUpdate()) {
      return shouldUpdate
          || (previousScopedContext != null
              && nextScopedContext != null
              && currentComponent != null
              && !currentComponent.isEqualivalentTreePropContainer(
                  previousScopedContext, nextScopedContext));
    }

    return shouldUpdate;
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
      final Component previous,
      final @Nullable StateContainer prevStateContainer,
      final Component next,
      final @Nullable StateContainer nextStateContainer) {
    if (!isPureRender()) {
      return true;
    }

    return !previous.isEquivalentProps(next, false)
        || !ComponentUtils.hasEquivalentState(prevStateContainer, nextStateContainer);
  }

  /** For internal use, only. */
  public static void dispatchErrorEvent(ComponentContext c, ErrorEvent e) {
    ComponentUtils.dispatchErrorEvent(c, e);
  }

  @Nullable
  protected static EventTrigger getEventTrigger(ComponentContext c, int id, String key) {
    if (c.getComponentScope() == null || c.getStateUpdater() == null) {
      return null;
    }

    return c.getStateUpdater().getEventTrigger(c.getGlobalKey() + id + key);
  }

  @Nullable
  protected static EventTrigger getEventTrigger(ComponentContext c, int id, Handle handle) {
    if (handle.getStateUpdater() == null) {
      return null;
    }

    return handle.getStateUpdater().getEventTrigger(handle, id);
  }

  protected static <E> EventHandler<E> newEventHandler(
      final Class<? extends Component> reference,
      final String className,
      final ComponentContext c,
      final int id,
      final Object[] params,
      final EventHandlerRebindMode mode) {
    if (c == null
        || c.getComponentScope() == null
        || !(c.getComponentScope() instanceof HasEventDispatcher)) {
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
    final EventHandler eventHandler =
        new EventHandler<>(
            id, mode, new EventDispatchInfo((HasEventDispatcher) c.getComponentScope(), c), params);
    final CalculationContext calculationContext = c.getCalculationStateContext();
    if (calculationContext != null) {
      if (c.shouldUseNonRebindingEventHandlers()) {
        if (mode == EventHandlerRebindMode.REBIND) {
          calculationContext.recordEventHandler(c.getGlobalKey(), eventHandler);
        }
      } else {
        calculationContext.recordEventHandler(c.getGlobalKey(), eventHandler);
      }
    }
    return eventHandler;
  }

  /**
   * This variant is used to create an EventTrigger used to register this component as a target in
   * {@link EventTriggersContainer}
   */
  protected static <E> EventTrigger<E> newEventTrigger(
      ComponentContext c, Component component, int methodId) {
    return c.newEventTrigger(methodId, component.getKey(), component.getHandle());
  }

  /**
   * This is used to create a Trigger to be invoked later, e.g. in the context of the deprecated
   * trigger API TextInput.requestFocusTrigger(c, "my_key").
   */
  @Deprecated
  protected static <E> EventTrigger<E> newEventTrigger(
      ComponentContext c, String childKey, int methodId) {
    return c.newEventTrigger(methodId, childKey, null);
  }

  public enum MountType {
    NONE,
    DRAWABLE,
    VIEW,
    PRIMITIVE /* For internal use only. Used only by Kotlin PrimitiveComponent */
  }

  /**
   * A per-Component-class data structure to keep track of some of the last mounted @Prop/@State
   * params a component was rendered with. The exact params that are tracked are just the ones
   * needed to support that Component's use of {@link Diff} params in their lifecycle methods that
   * allow Diff params (e.g. {@link #onCreateTransition}).
   */
  public interface RenderData {}

  public String getSimpleName() {
    return CommonUtils.getSectionNameForTracing(getClass());
  }

  /**
   * Determine if this component has equivalent props to a given component. This method does not
   * compare common props.
   *
   * @param other the component to compare to
   * @return true if the components have equivalent props
   */
  public boolean isEquivalentProps(@Nullable Component other, boolean shouldCompareCommonProps) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    if (getId() == other.getId()) {
      return true;
    }
    return ComponentUtils.hasEquivalentFields(this, other);
  }

  /**
   * Compares this component to a different one to check if they are the same
   *
   * <p>This is used to be able to skip rendering a component again. We avoid using the {@link
   * Object#equals(Object)} so we can optimize the code better over time since we don't have to
   * adhere to the contract required for a equals method.
   *
   * @param other the component to compare to
   * @return true if the components are of the same type and have the same props
   */
  @Override
  public final boolean isEquivalentTo(@Nullable Component other) {
    return isEquivalentTo(other, ComponentsConfiguration.shouldCompareCommonPropsInIsEquivalentTo);
  }

  public boolean isEquivalentTo(@Nullable Component other, boolean shouldCompareCommonProps) {
    return isEquivalentProps(other, shouldCompareCommonProps);
  }

  public Component makeShallowCopy() {
    try {
      return (Component) super.clone();
    } catch (CloneNotSupportedException e) {
      // This class implements Cloneable, so this is impossible
      throw new RuntimeException(e);
    }
  }

  /**
   * Measure a component with the given {@link SizeSpec} constrain.
   *
   * @param c {@link ComponentContext}.
   * @param widthSpec Width {@link SizeSpec} constrain.
   * @param heightSpec Height {@link SizeSpec} constrain.
   * @param outputSize Size object that will be set with the measured dimensions.
   */
  public final void measure(ComponentContext c, int widthSpec, int heightSpec, Size outputSize) {
    measure(c, widthSpec, heightSpec, outputSize, true);
  }

  public final void measure(
      final ComponentContext c,
      final int widthSpec,
      final int heightSpec,
      final Size outputSize,
      final boolean shouldCacheResult) {

    final CalculationContext calculationContext = c.getCalculationStateContext();

    if (calculationContext == null) {
      if (shouldCacheResult) {
        throw new IllegalStateException(
            getSimpleName()
                + ": Trying to measure outside of layout calculation. "
                + "See Component#measureMightNotCacheInternalNode instead.");
      } else {
        // Not caching, so redirect to measureMightNotCacheInternalNode that will setup a temp
        // state-context and call this method again.
        measureMightNotCacheInternalNode(c, widthSpec, heightSpec, outputSize);
        return;
      }
    }

    final int layoutVersion = calculationContext.getLayoutVersion();
    final int rootComponentId = calculationContext.getRootComponentId();
    final MeasuredResultCache resultCache =
        shouldCacheResult ? calculationContext.getCache() : new MeasuredResultCache();
    final TreeState treeState = calculationContext.getTreeState();
    final ResolveContext mainRsc =
        calculationContext instanceof ResolveContext ? (ResolveContext) calculationContext : null;

    LithoLayoutResult lastMeasuredLayout = resultCache.getCachedResult(this);

    if (lastMeasuredLayout == null
        || !MeasureComparisonUtils.isMeasureSpecCompatible(
            lastMeasuredLayout.getWidthSpec(), widthSpec, lastMeasuredLayout.getWidth())
        || !MeasureComparisonUtils.isMeasureSpecCompatible(
            lastMeasuredLayout.getHeightSpec(), heightSpec, lastMeasuredLayout.getHeight())) {
      resultCache.clearCache(this);

      final CalculationContext prevContext = calculationContext;

      try {
        final LithoNode node;

        if (lastMeasuredLayout != null && lastMeasuredLayout.getNode() != null) {
          node = lastMeasuredLayout.getNode();
        } else {
          final ResolveContext nestedRsc =
              new ResolveContext(
                  calculationContext.getTreeId(),
                  resultCache,
                  treeState,
                  layoutVersion,
                  rootComponentId,
                  calculationContext.isAccessibilityEnabled(),
                  null,
                  null,
                  null,
                  null);
          c.setRenderStateContext(nestedRsc);

          node = Resolver.resolveTree(nestedRsc, c, this);
        }

        if (mainRsc != null && mainRsc.isLayoutInterrupted() && node != null) {
          outputSize.width = 0;
          outputSize.height = 0;
          return;
        }

        final LithoLayoutContext nestedLsc =
            new LithoLayoutContext(
                calculationContext.getTreeId(),
                resultCache,
                c,
                treeState,
                layoutVersion,
                rootComponentId,
                calculationContext.isAccessibilityEnabled(),
                new LayoutCache(),
                null,
                null);

        lastMeasuredLayout =
            Layout.measureTree(nestedLsc, c.getAndroidContext(), node, widthSpec, heightSpec, null);

        if (lastMeasuredLayout == null) {
          outputSize.width = 0;
          outputSize.height = 0;
          return;
        }
      } finally {
        c.setCalculationStateContext(prevContext);
      }

      // Add the cached result.
      resultCache.addCachedResult(this, lastMeasuredLayout.getNode(), lastMeasuredLayout);
    }
    outputSize.width = lastMeasuredLayout.getWidth();
    outputSize.height = lastMeasuredLayout.getHeight();

    if (!shouldCacheResult) {
      resultCache.clearCache(this);
    }
  }

  /**
   * Should not be used! Components should be manually measured only as part of a LayoutState
   * calculation. This will measure a component and set the size in the outputSize object but the
   * measurement result will not be cached and reused for future measurements of this component.
   *
   * <p>This is very inefficient because it throws away the InternalNode from measuring here and
   * will have to remeasure when the component needs to be measured as part of a LayoutState. This
   * will lead to suboptimal performance.
   *
   * <p>You probably don't need to use this. If you really need to measure your Component outside of
   * a LayoutState calculation reach out to the Litho team to discuss an alternative solution.
   *
   * <p>If this is called during a LayoutState calculation, it will delegate to {@link
   * SpecGeneratedComponent#onMeasure(ComponentContext, ComponentLayout, int, int, Size,
   * InterStagePropsContainer)}, which does cache the measurement result for the duration of this
   * LayoutState.
   */
  @Deprecated
  public final void measureMightNotCacheInternalNode(
      ComponentContext c, int widthSpec, int heightSpec, Size outputSize) {
    final CalculationContext prevContext = c.getCalculationStateContext();

    if (prevContext != null && !prevContext.isFutureReleased()) {
      measure(c, widthSpec, heightSpec, outputSize);
      return;
    }

    try {
      LithoTree lithoTree = c.getLithoTree();
      int componentTreeId;

      if (lithoTree == null) {
        // This is a temporary tree that will be only used as a way of measuring a component.
        // we could be using a treeless context here as well. Might be worth revisiting later.
        ComponentTree ct = ComponentTree.create(c).build();
        componentTreeId = ct.mId;

        c =
            new ComponentContext(
                c.getAndroidContext(),
                c.getTreePropContainer(),
                ct.getLithoConfiguration(),
                LithoTree.Companion.create(ct),
                c.mGlobalKey,
                c.getLifecycleProvider(),
                null,
                c.getParentTreePropContainer());
      } else {
        componentTreeId = lithoTree.getId();
      }

      final ResolveContext tempRsc =
          new ResolveContext(
              componentTreeId,
              new MeasuredResultCache(),
              new TreeState(),
              0,
              -1,
              prevContext != null
                  ? prevContext.isAccessibilityEnabled()
                  : AccessibilityUtils.isAccessibilityEnabled(
                      (AccessibilityManager)
                          c.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE)),
              null,
              null,
              null,
              null);

      c.setRenderStateContext(tempRsc);

      // At this point we're trying to measure the Component outside of a layout calculation.
      // The state values are irrelevant in this scenario - outside of a layout calculation they
      // should bethe default/initial values.
      measure(c, widthSpec, heightSpec, outputSize, false);
    } finally {
      c.setCalculationStateContext(prevContext);
    }
  }

  @Nullable
  final LithoNode consumeLayoutCreatedInWillRender(
      final @Nullable ResolveContext resolveContext, @Nullable ComponentContext context) {
    LithoNode layout;

    if (context == null || resolveContext == null) {
      return null;
    }

    return resolveContext.consumeLayoutCreatedInWillRender(mId);
  }

  @VisibleForTesting
  @Nullable
  final LithoNode getLayoutCreatedInWillRender(final ResolveContext resolveContext) {
    return resolveContext.getLayoutCreatedInWillRender(mId);
  }

  private void setLayoutCreatedInWillRender(
      final ResolveContext resolveContext, final @Nullable LithoNode newValue) {
    resolveContext.setLayoutCreatedInWillRender(mId, newValue);
  }

  /**
   * @return The error handler dispatching to either the parent component if available, or reraising
   *     the exception. Null if the component isn't initialized.
   */
  @Nullable
  final EventHandler<ErrorEvent> getErrorHandler(ComponentContext scopedContext) {
    return scopedContext.getScopedComponentInfo().getErrorEventHandler();
  }

  protected final @Nullable EventHandler<ErrorEvent> getErrorHandler() {
    return mErrorEventHandler;
  }

  /** This setter should only be called during the render phase of the component, never after. */
  final void setErrorEventHandlerDuringRender(EventHandler<ErrorEvent> errorHandler) {
    mErrorEventHandler = errorHandler;
  }

  /**
   * @return a handle that is unique to this component.
   */
  @Nullable
  public final Handle getHandle() {
    return mHandle;
  }

  /**
   * Set a handle that is unique to this component.
   *
   * @param handle handle
   */
  final void setHandle(@Nullable Handle handle) {
    mHandle = handle;
  }

  /**
   * @return a key that is local to the component's parent.
   */
  final String getKey() {
    if (mKey == null) {
      if (mHasManualKey) {
        throw new IllegalStateException(
            "Should not have null manual key! (" + getSimpleName() + ")");
      }
      mKey = Integer.toString(getTypeId());
    }
    return mKey;
  }

  /**
   * Set a key that is local to the parent of this component.
   *
   * @param key key
   */
  final void setKey(String key) {
    mHasManualKey = true;
    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    }
    mKey = key;
  }

  /**
   * @return if has a handle set
   */
  final boolean hasHandle() {
    return mHandle != null;
  }

  /**
   * @return if has a manually set key
   */
  final boolean hasManualKey() {
    return mHasManualKey;
  }

  final Component makeShallowCopyWithNewId() {
    final Component component = makeShallowCopy();
    component.mId = sIdGenerator.getAndIncrement();
    return component;
  }

  // Get an id that is identical across cloned instances, but otherwise unique
  final int getId() {
    return mId;
  }

  @Override
  public final String toString() {
    return getSimpleName();
  }

  private boolean hasCachedNode(final ResolveContext resolveContext) {
    final MeasuredResultCache resultCache = resolveContext.getCache();
    return resultCache.hasCachedNode(this);
  }

  /**
   * @return whether the given component will render because it returns non-null from its resolved
   *     onCreateLayout, based on its current props and state. Returns true if the resolved layout
   *     is non-null, otherwise false.
   * @deprecated Using willRender is regarded as an anti-pattern, since it will load all classes
   *     into memory in order to potentially decide not to use any of them.
   */
  @Deprecated
  public static boolean willRender(ComponentContext c, Component component) {
    if (component == null) {
      return false;
    }

    final ResolveContext resolveContext = Preconditions.checkNotNull(c.getRenderStateContext());

    final LithoNode componentLayoutCreatedInWillRender =
        component.getLayoutCreatedInWillRender(resolveContext);
    if (componentLayoutCreatedInWillRender != null) {
      return willRender(resolveContext, c, component, componentLayoutCreatedInWillRender);
    }

    final LithoNode newLayoutCreatedInWillRender = Resolver.resolve(resolveContext, c, component);
    boolean willRender = willRender(resolveContext, c, component, newLayoutCreatedInWillRender);

    // will render will return false for a null node but the
    // node still needs to be cached for reconciliation.
    if (willRender || newLayoutCreatedInWillRender instanceof NullNode) {
      component.setLayoutCreatedInWillRender(resolveContext, newLayoutCreatedInWillRender);
    }
    return willRender;
  }

  static boolean isHostSpec(@Nullable Component component) {
    return (component instanceof HostComponent);
  }

  static boolean isLayoutSpec(@Nullable Component component) {
    return (component != null && component.getMountType() == MountType.NONE);
  }

  public static boolean isLayoutSpecWithSizeSpec(@Nullable Component component) {
    return component != null
        && component.getMountType() == MountType.NONE
        && component.canMeasure();
  }

  static boolean isMountSpec(@Nullable Component component) {
    return (component != null && component.getMountType() != MountType.NONE);
  }

  static boolean isPrimitive(@Nullable Component component) {
    return (component != null && component.getMountType() == MountType.PRIMITIVE);
  }

  static boolean isNestedTree(@Nullable Component component) {
    return isLayoutSpecWithSizeSpec(component);
  }

  static boolean hasCachedNode(final ResolveContext context, final Component component) {
    return component.hasCachedNode(context);
  }

  /**
   * @return whether the given component is a pure render component.
   */
  @VisibleForTesting
  public static boolean isPureRender(@Nullable Component component) {
    return component != null && component.isPureRender();
  }

  /** Store a working range information into a list for later use by {@link LayoutState}. */
  protected static void registerWorkingRange(
      ComponentContext scopedContext,
      String name,
      WorkingRange workingRange,
      Component component,
      String globalKey) {
    scopedContext
        .getScopedComponentInfo()
        .registerWorkingRange(name, workingRange, component, globalKey);
  }

  protected static @Nullable <T> T retrieveValue(@Nullable DynamicValue<T> dynamicValue) {
    return dynamicValue != null ? dynamicValue.get() : null;
  }

  private static boolean willRender(
      final ResolveContext resolveContext,
      ComponentContext context,
      Component component,
      @Nullable LithoNode node) {
    if (node == null || node instanceof NullNode) {
      return false;
    }

    if (node instanceof NestedTreeHolder) {
      // Components using @OnCreateLayoutWithSizeSpec are lazily resolved after the rest of the tree
      // has been measured (so that we have the proper measurements to pass in). This means we can't
      // eagerly check the result of OnCreateLayoutWithSizeSpec.
      component.consumeLayoutCreatedInWillRender(
          resolveContext, context); // Clear the layout created in will render
      throw new IllegalArgumentException(
          "Cannot check willRender on a component that uses @OnCreateLayoutWithSizeSpec! "
              + "Try wrapping this component in one that uses @OnCreateLayout if possible.");
    }

    return true;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final boolean equals(@Nullable Object obj) {
    return super.equals(obj);
  }

  @Override
  protected final Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  final void setBuilderContext(@Nullable Context context) {
    mBuilderContext = context;
  }

  public final @Nullable String getBuilderContextName() {
    return mBuilderContextName;
  }

  public final void setBuilderContextName(@Nullable String name) {
    mBuilderContextName = name;
  }

  public static String getBuilderContextName(@Nullable Context context) {
    if (context == null) {
      return "null";
    } else {
      return "<cls>" + context.getClass().getName() + "</cls>@" + context.hashCode();
    }
  }

  /**
   * @param <T> the type of this builder. Required to ensure methods defined here in the abstract
   *     class correctly return the type of the concrete subclass.
   */
  public abstract static class Builder<T extends Builder<T>> {

    protected final ResourceResolver mResourceResolver;
    private final ComponentContext mContext;
    private SpecGeneratedComponent mComponent;

    protected Builder(
        ComponentContext c,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        Component component) {
      Preconditions.checkNotNull(c);

      if (!(component instanceof SpecGeneratedComponent)) {
        throw new RuntimeException(
            "Component.Builder only accepts SpecGeneratedComponent and "
                + component.getClass()
                + " was provided.");
      }

      mResourceResolver = c.getResourceResolver();
      mComponent = (SpecGeneratedComponent) component;
      mContext = c;

      final Component owner = getOwner();
      if (owner != null) {
        mComponent.setOwnerGlobalKey(mContext.getGlobalKey());
      }

      if (defStyleAttr != 0 || defStyleRes != 0) {
        mComponent.getOrCreateCommonProps().setStyle(defStyleAttr, defStyleRes);
        try {
          mComponent.loadStyle(c, defStyleAttr, defStyleRes);
        } catch (Exception e) {
          ComponentUtils.handleWithHierarchy(c, component, e);
        }
      }
      final Context context = c.getAndroidContext();
      mComponent.setBuilderContextName(getBuilderContextName(context));
    }

    @ReturnsOwnership
    public abstract Component build();

    public abstract T getThis();

    protected abstract void setComponent(Component component);

    /**
     * Ports {@link androidx.core.view.ViewCompat#setAccessibilityHeading} into components world.
     * However, since the aforementioned ViewCompat's method is available only on API 19 and above,
     * calling this method on lower APIs will have no effect. On the legit versions, on the other
     * hand, calling this method will lead to the component being treated as a heading. The
     * AccessibilityHeading property allows accessibility services to help users navigate directly
     * from one heading to the next. See {@link
     * androidx.core.view.accessibility.AccessibilityNodeInfoCompat#setHeading} for more
     * information.
     *
     * <p>Default: false
     */
    public T accessibilityHeading(boolean isHeading) {
      mComponent.getOrCreateCommonProps().accessibilityHeading(isHeading);
      return getThis();
    }

    public T accessibilityRole(@Nullable @AccessibilityRole.AccessibilityRoleType String role) {
      mComponent.getOrCreateCommonProps().accessibilityRole(role);
      return getThis();
    }

    public T accessibilityRoleDescription(@Nullable CharSequence roleDescription) {
      mComponent.getOrCreateCommonProps().accessibilityRoleDescription(roleDescription);
      return getThis();
    }

    public T accessibilityRoleDescription(@StringRes int stringId) {
      return accessibilityRoleDescription(mContext.getResources().getString(stringId));
    }

    public T accessibilityRoleDescription(@StringRes int stringId, Object... formatArgs) {
      return accessibilityRoleDescription(mContext.getResources().getString(stringId, formatArgs));
    }

    /**
     * Controls how a child aligns in the cross direction, overriding the alignItems of the parent.
     * See <a
     * href="https://yogalayout.dev/docs/align-items">https://yogalayout.dev/docs/align-items</a>
     * for more information.
     *
     * <p>Default: {@link YogaAlign#AUTO}
     */
    public T alignSelf(YogaAlign alignSelf) {
      mComponent.getOrCreateCommonProps().alignSelf(alignSelf);
      return getThis();
    }

    /** Sets the alpha (opacity) of this component. */
    public T alpha(float alpha) {
      mComponent.getOrCreateCommonProps().alpha(alpha);
      return getThis();
    }

    /**
     * Links a {@link DynamicValue} object ot the alpha value for this Component
     *
     * @param value controller for the alpha value
     */
    public T alpha(DynamicValue<Float> value) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_ALPHA, value);
      return getThis();
    }

    /**
     * Defined as the ratio between the width and the height of a node. See <a
     * href="https://yogalayout.dev/docs/aspect-ratio">https://yogalayout.dev/docs/aspect-ratio</a>
     * for more information
     */
    public T aspectRatio(float aspectRatio) {
      mComponent.getOrCreateCommonProps().aspectRatio(aspectRatio);
      return getThis();
    }

    /**
     * Set the background of this component. The background drawable can implement {@link
     * ComparableDrawable} for more efficient diffing while when drawables are remounted or updated.
     *
     * @see ComparableDrawable
     */
    public T background(@Nullable Drawable background) {
      mComponent.getOrCreateCommonProps().background(background);
      return getThis();
    }

    public T backgroundAttr(@AttrRes int resId, @DrawableRes int defaultResId) {
      return backgroundRes(mResourceResolver.resolveResIdAttr(resId, defaultResId));
    }

    public T backgroundAttr(@AttrRes int resId) {
      return backgroundAttr(resId, 0);
    }

    public T backgroundColor(@ColorInt int backgroundColor) {
      return background(ComparableColorDrawable.create(backgroundColor));
    }

    /**
     * Links a {@link DynamicValue} object to the background color value for this Component
     *
     * @param value controller for the background color value
     */
    public T backgroundColor(@Nullable DynamicValue<Integer> value) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_BACKGROUND_COLOR, value);
      return getThis();
    }

    /**
     * Links a {@link DynamicValue} object to the background drawable for this Component
     *
     * @param value controller for the background drawable
     */
    public T backgroundDynamicDrawable(DynamicValue<? extends Drawable> value) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_BACKGROUND_DRAWABLE, value);
      return getThis();
    }

    public T backgroundRes(@DrawableRes int resId) {
      if (resId == 0) {
        return background(null);
      }

      return background(ContextCompat.getDrawable(mContext.getAndroidContext(), resId));
    }

    public T border(@Nullable Border border) {
      mComponent.getOrCreateCommonProps().border(border);
      return getThis();
    }

    public T clickHandler(@Nullable EventHandler<ClickEvent> clickHandler) {
      mComponent.getOrCreateCommonProps().clickHandler(clickHandler);
      return getThis();
    }

    public T clickable(boolean isClickable) {
      mComponent.getOrCreateCommonProps().clickable(isClickable);
      return getThis();
    }

    /**
     * Ports {@link android.view.ViewGroup#setClipChildren(boolean)} into components world. However,
     * there is no guarantee that child of this component would be translated into direct view child
     * in the resulting view hierarchy.
     *
     * @param clipChildren true to clip children to their bounds. False allows each child to draw
     *     outside of its own bounds within the parent, it doesn't allow children to draw outside of
     *     the parent itself.
     */
    public T clipChildren(boolean clipChildren) {
      mComponent.getOrCreateCommonProps().clipChildren(clipChildren);
      return getThis();
    }

    public T clipToOutline(boolean clipToOutline) {
      mComponent.getOrCreateCommonProps().clipToOutline(clipToOutline);
      return getThis();
    }

    public T contentDescription(@Nullable CharSequence contentDescription) {
      mComponent.getOrCreateCommonProps().contentDescription(contentDescription);
      return getThis();
    }

    public T contentDescription(@StringRes int stringId) {
      return contentDescription(mContext.getAndroidContext().getResources().getString(stringId));
    }

    public T contentDescription(@StringRes int stringId, Object... formatArgs) {
      return contentDescription(
          mContext.getAndroidContext().getResources().getString(stringId, formatArgs));
    }

    public T dispatchPopulateAccessibilityEventHandler(
        @Nullable
            EventHandler<DispatchPopulateAccessibilityEventEvent>
                dispatchPopulateAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .dispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
      return getThis();
    }

    /**
     * If true, component duplicates its drawable state (focused, pressed, etc.) from the direct
     * parent.
     *
     * <p>In the following example, when {@code Row} gets pressed state, its child {@code
     * OtherStatefulDrawable} will get that pressed state within itself too:
     *
     * <pre>{@code
     * Row.create(c)
     *     .drawable(stateListDrawable)
     *     .clickable(true)
     *     .child(
     *         OtherStatefulDrawable.create(c)
     *             .duplicateParentState(true))
     * }</pre>
     *
     * @see android.view.View#setDuplicateParentStateEnabled(boolean)
     */
    public T duplicateParentState(boolean duplicateParentState) {
      mComponent.getOrCreateCommonProps().duplicateParentState(duplicateParentState);
      return getThis();
    }

    /**
     * If true, component applies all of its children's drawable states (focused, pressed, etc.) to
     * itself.
     *
     * <p>In the following example, when {@code OtherStatefulDrawable} gets pressed state, its
     * parent {@code Row} will also get that pressed state within itself:
     *
     * <pre>{@code
     * Row.create(c)
     *     .drawable(stateListDrawable)
     *     .duplicateChildrenStates(true)
     *     .child(
     *         OtherStatefulDrawable.create(c)
     *             .clickable(true))
     * }</pre>
     *
     * @see android.view.ViewGroup#setAddStatesFromChildren
     */
    public T duplicateChildrenStates(boolean duplicateChildrenStates) {
      mComponent.getOrCreateCommonProps().duplicateChildrenStates(duplicateChildrenStates);
      return getThis();
    }

    public T enabled(boolean isEnabled) {
      mComponent.getOrCreateCommonProps().enabled(isEnabled);
      return getThis();
    }

    /**
     * Sets flexGrow, flexShrink, and flexBasis at the same time.
     *
     * <p>When flex is a positive number, it makes the component flexible and it will be sized
     * proportional to its flex value. So a component with flex set to 2 will take twice the space
     * as a component with flex set to 1.
     *
     * <p>When flex is 0, the component is sized according to width and height and it is inflexible.
     *
     * <p>When flex is -1, the component is normally sized according width and height. However, if
     * there's not enough space, the component will shrink to its minWidth and minHeight.
     *
     * <p>See <a href="https://yogalayout.dev/docs/flex">https://yogalayout.dev/docs/flex</a> for
     * more information.
     *
     * <p>Default: 0
     */
    public T flex(float flex) {
      mComponent.getOrCreateCommonProps().flex(flex);
      return getThis();
    }

    /**
     * @see #flexBasisPx
     */
    public T flexBasisAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return flexBasisPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #flexBasisPx
     */
    public T flexBasisAttr(@AttrRes int resId) {
      return flexBasisAttr(resId, 0);
    }

    /**
     * @see #flexBasisPx
     */
    public T flexBasisDip(@Dimension(unit = DP) float flexBasis) {
      return flexBasisPx(mResourceResolver.dipsToPixels(flexBasis));
    }

    /**
     * @param percent a value between 0 and 100.
     * @see #flexBasisPx
     */
    public T flexBasisPercent(float percent) {
      mComponent.getOrCreateCommonProps().flexBasisPercent(percent);
      return getThis();
    }

    /**
     * The FlexBasis property is an axis-independent way of providing the default size of an item on
     * the main axis. Setting the FlexBasis of a child is similar to setting the Width of that child
     * if its parent is a container with FlexDirection = row or setting the Height of a child if its
     * parent is a container with FlexDirection = column. The FlexBasis of an item is the default
     * size of that item, the size of the item before any FlexGrow and FlexShrink calculations are
     * performed. See <a
     * href="https://yogalayout.dev/docs/flex">https://yogalayout.dev/docs/flex</a> for more
     * information.
     *
     * <p>Default: 0
     */
    public T flexBasisPx(@Px int flexBasis) {
      mComponent.getOrCreateCommonProps().flexBasisPx(flexBasis);
      return getThis();
    }

    /**
     * @see #flexBasisPx
     */
    public T flexBasisRes(@DimenRes int resId) {
      return flexBasisPx(mResourceResolver.resolveDimenSizeRes(resId));
    }

    /**
     * If the sum of childrens' main axis dimensions is less than the minimum size, how much should
     * this component grow? This value represents the "flex grow factor" and determines how much
     * this component should grow along the main axis in relation to any other flexible children.
     * See <a href="https://yogalayout.dev/docs/flex">https://yogalayout.dev/docs/flex</a> for more
     * information.
     *
     * <p>Default: 0
     */
    public T flexGrow(float flexGrow) {
      mComponent.getOrCreateCommonProps().flexGrow(flexGrow);
      return getThis();
    }

    /**
     * The FlexShrink property describes how to shrink children along the main axis in the case that
     * the total size of the children overflow the size of the container on the main axis. See <a
     * href="https://yogalayout.dev/docs/flex">https://yogalayout.dev/docs/flex</a> for more
     * information.
     *
     * <p>Default: 1
     */
    public T flexShrink(float flexShrink) {
      mComponent.getOrCreateCommonProps().flexShrink(flexShrink);
      return getThis();
    }

    public T focusChangeHandler(@Nullable EventHandler<FocusChangedEvent> focusChangeHandler) {
      mComponent.getOrCreateCommonProps().focusChangeHandler(focusChangeHandler);
      return getThis();
    }

    public T focusable(boolean isFocusable) {
      mComponent.getOrCreateCommonProps().focusable(isFocusable);
      return getThis();
    }

    public T focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
      mComponent.getOrCreateCommonProps().focusedHandler(focusedHandler);
      return getThis();
    }

    /**
     * Set the foreground of this component. The foreground drawable must extend {@link
     * ComparableDrawable} for more efficient diffing while when drawables are remounted or updated.
     * If the drawable does not extend {@link ComparableDrawable} then create a new class which
     * extends {@link ComparableDrawable} and implement the {@link
     * ComparableDrawable#isEquivalentTo(ComparableDrawable)}.
     *
     * @see ComparableDrawable
     */
    public T foreground(@Nullable Drawable foreground) {
      mComponent.getOrCreateCommonProps().foreground(foreground);
      return getThis();
    }

    public T foregroundAttr(@AttrRes int resId, @DrawableRes int defaultResId) {
      return foregroundRes(mResourceResolver.resolveResIdAttr(resId, defaultResId));
    }

    public T foregroundAttr(@AttrRes int resId) {
      return foregroundAttr(resId, 0);
    }

    public T foregroundColor(@ColorInt int foregroundColor) {
      return foreground(ComparableColorDrawable.create(foregroundColor));
    }

    public T foregroundColor(DynamicValue<Integer> value) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_FOREGROUND_COLOR, value);
      return getThis();
    }

    public T foregroundRes(@DrawableRes int resId) {
      if (resId == 0) {
        return foreground(null);
      }

      return foreground(ContextCompat.getDrawable(mContext.getAndroidContext(), resId));
    }

    public T fullImpressionHandler(
        @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
      mComponent.getOrCreateCommonProps().fullImpressionHandler(fullImpressionHandler);
      return getThis();
    }

    /**
     * @return the {@link ComponentContext} for this {@link Builder}, useful for Kotlin DSL. Will be
     *     null if the Builder was already used to {@link #build()} a component.
     */
    @Nullable
    public ComponentContext getContext() {
      return mContext;
    }

    public T handle(@Nullable Handle handle) {
      mComponent.setHandle(handle);
      return getThis();
    }

    @Deprecated
    public boolean hasBackgroundSet() {
      return mComponent.getCommonProps() != null
          && mComponent.getCommonProps().getBackground() != null;
    }

    public boolean hasClickHandlerSet() {
      return mComponent.hasClickHandlerSet();
    }

    /**
     * @see #heightPx
     */
    public T heightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return heightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #heightPx
     */
    public T heightAttr(@AttrRes int resId) {
      return heightAttr(resId, 0);
    }

    /**
     * @see #heightPx
     */
    public T heightDip(@Dimension(unit = DP) float height) {
      return heightPx(mResourceResolver.dipsToPixels(height));
    }

    /**
     * Sets the height of the Component to be a percentage of its parent's height. Note that if the
     * parent has unspecified height (e.g. it is a RecyclerView), then setting this will have no
     * effect.
     *
     * @param percent a value between 0 and 100.
     * @see #heightPx
     */
    public T heightPercent(float percent) {
      mComponent.getOrCreateCommonProps().heightPercent(percent);
      return getThis();
    }

    /**
     * Specifies the height of the element's content area. See <a
     * href="https://yogalayout.dev/docs/width-height">https://yogalayout.dev/docs/width-height</a>
     * for more information
     */
    public T heightPx(@Px int height) {
      mComponent.getOrCreateCommonProps().heightPx(height);
      return getThis();
    }

    /**
     * @see #heightPx
     */
    public T heightRes(@DimenRes int resId) {
      return heightPx(mResourceResolver.resolveDimenSizeRes(resId));
    }

    public T importantForAccessibility(int importantForAccessibility) {
      mComponent.getOrCreateCommonProps().importantForAccessibility(importantForAccessibility);
      return getThis();
    }

    public T interceptTouchHandler(
        @Nullable EventHandler<InterceptTouchEvent> interceptTouchHandler) {
      mComponent.getOrCreateCommonProps().interceptTouchHandler(interceptTouchHandler);
      return getThis();
    }

    public T invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
      mComponent.getOrCreateCommonProps().invisibleHandler(invisibleHandler);
      return getThis();
    }

    public T isReferenceBaseline(boolean isReferenceBaseline) {
      mComponent.getOrCreateCommonProps().isReferenceBaseline(isReferenceBaseline);
      return getThis();
    }

    /** Set a key on the component that is local to its parent. */
    public T key(@Nullable String key) {
      if (key == null) {
        final String componentName =
            mContext.getComponentScope() != null
                ? mContext.getComponentScope().getSimpleName()
                : "unknown component";
        final String message =
            "Setting a null key from "
                + componentName
                + " which is usually a mistake! If it is not, explicitly set the String 'null'";
        ComponentsReporter.emitMessage(ComponentsReporter.LogLevel.ERROR, NULL_KEY_SET, message);
        key = "null";
      }
      mComponent.setKey(key);
      return getThis();
    }

    /**
     * The RTL/LTR direction of components and text. Determines whether {@link YogaEdge#START} and
     * {@link YogaEdge#END} will resolve to the left or right side, among other things. INHERIT
     * indicates this setting will be inherited from this component's parent.
     *
     * <p>Default: {@link YogaDirection#INHERIT}
     */
    public T layoutDirection(YogaDirection direction) {
      YogaLayoutProps.setLayoutDirection(mComponent.getOrCreateCommonProps(), direction);
      return getThis();
    }

    public T longClickHandler(@Nullable EventHandler<LongClickEvent> longClickHandler) {
      mComponent.getOrCreateCommonProps().longClickHandler(longClickHandler);
      return getThis();
    }

    /**
     * @see #marginPx
     */
    public T marginAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      return marginPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #marginPx
     */
    public T marginAttr(YogaEdge edge, @AttrRes int resId) {
      return marginAttr(edge, resId, 0);
    }

    /**
     * @see #marginPx
     */
    public T marginAuto(YogaEdge edge) {
      mComponent.getOrCreateCommonProps().marginAuto(edge);
      return getThis();
    }

    /**
     * @see #marginPx
     */
    public T marginDip(YogaEdge edge, @Dimension(unit = DP) float margin) {
      return marginPx(edge, mResourceResolver.dipsToPixels(margin));
    }

    /**
     * @param percent a value between 0 and 100.
     * @see #marginPx
     */
    public T marginPercent(YogaEdge edge, float percent) {
      mComponent.getOrCreateCommonProps().marginPercent(edge, percent);
      return getThis();
    }

    /**
     * Effects the spacing around the outside of a node. A node with margin will offset itself from
     * the bounds of its parent but also offset the location of any siblings. See <a
     * href="https://yogalayout.dev/docs/margins-paddings-borders">https://yogalayout.dev/docs/margins-paddings-borders</a>
     * for more information
     */
    public T marginPx(YogaEdge edge, @Px int margin) {
      mComponent.getOrCreateCommonProps().marginPx(edge, margin);
      return getThis();
    }

    /**
     * @see #marginPx
     */
    public T marginRes(YogaEdge edge, @DimenRes int resId) {
      return marginPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
    }

    /**
     * @see #minWidthPx
     */
    public T maxHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return maxHeightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #minWidthPx
     */
    public T maxHeightAttr(@AttrRes int resId) {
      return maxHeightAttr(resId, 0);
    }

    /**
     * @see #minWidthPx
     */
    public T maxHeightDip(@Dimension(unit = DP) float maxHeight) {
      return maxHeightPx(mResourceResolver.dipsToPixels(maxHeight));
    }

    /**
     * @param percent a value between 0 and 100.
     * @see #minWidthPx
     */
    public T maxHeightPercent(float percent) {
      mComponent.getOrCreateCommonProps().maxHeightPercent(percent);
      return getThis();
    }

    /**
     * @see #minWidthPx
     */
    public T maxHeightPx(@Px int maxHeight) {
      mComponent.getOrCreateCommonProps().maxHeightPx(maxHeight);
      return getThis();
    }

    /**
     * @see #minWidthPx
     */
    public T maxHeightRes(@DimenRes int resId) {
      return maxHeightPx(mResourceResolver.resolveDimenSizeRes(resId));
    }

    /**
     * @see #minWidthPx
     */
    public T maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return maxWidthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #minWidthPx
     */
    public T maxWidthAttr(@AttrRes int resId) {
      return maxWidthAttr(resId, 0);
    }

    /**
     * @see #minWidthPx
     */
    public T maxWidthDip(@Dimension(unit = DP) float maxWidth) {
      return maxWidthPx(mResourceResolver.dipsToPixels(maxWidth));
    }

    /**
     * @param percent a value between 0 and 100.
     * @see #minWidthPx
     */
    public T maxWidthPercent(float percent) {
      mComponent.getOrCreateCommonProps().maxWidthPercent(percent);
      return getThis();
    }

    /**
     * @see #minWidthPx
     */
    public T maxWidthPx(@Px int maxWidth) {
      mComponent.getOrCreateCommonProps().maxWidthPx(maxWidth);
      return getThis();
    }

    /**
     * @see #minWidthPx
     */
    public T maxWidthRes(@DimenRes int resId) {
      return maxWidthPx(mResourceResolver.resolveDimenSizeRes(resId));
    }

    /**
     * @see #minWidthPx
     */
    public T minHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return minHeightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #minWidthPx
     */
    public T minHeightAttr(@AttrRes int resId) {
      return minHeightAttr(resId, 0);
    }

    /**
     * @see #minWidthPx
     */
    public T minHeightDip(@Dimension(unit = DP) float minHeight) {
      return minHeightPx(mResourceResolver.dipsToPixels(minHeight));
    }

    /**
     * @param percent a value between 0 and 100.
     * @see #minWidthPx
     */
    public T minHeightPercent(float percent) {
      mComponent.getOrCreateCommonProps().minHeightPercent(percent);
      return getThis();
    }

    /**
     * @see #minWidthPx
     */
    public T minHeightPx(@Px int minHeight) {
      mComponent.getOrCreateCommonProps().minHeightPx(minHeight);
      return getThis();
    }

    /**
     * @see #minWidthPx
     */
    public T minHeightRes(@DimenRes int resId) {
      return minHeightPx(mResourceResolver.resolveDimenSizeRes(resId));
    }

    /**
     * @see #minWidthPx
     */
    public T minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return minWidthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #minWidthPx
     */
    public T minWidthAttr(@AttrRes int resId) {
      return minWidthAttr(resId, 0);
    }

    /**
     * @see #minWidthPx
     */
    public T minWidthDip(@Dimension(unit = DP) float minWidth) {
      return minWidthPx(mResourceResolver.dipsToPixels(minWidth));
    }

    /**
     * @param percent a value between 0 and 100.
     * @see #minWidthPx
     */
    public T minWidthPercent(float percent) {
      mComponent.getOrCreateCommonProps().minWidthPercent(percent);
      return getThis();
    }

    /**
     * This property has higher priority than all other properties and will always be respected. See
     * <a href="https://yogalayout.dev/docs/min-max/">https://yogalayout.dev/docs/min-max/</a> for
     * more information
     */
    public T minWidthPx(@Px int minWidth) {
      mComponent.getOrCreateCommonProps().minWidthPx(minWidth);
      return getThis();
    }

    /**
     * @see #minWidthPx
     */
    public T minWidthRes(@DimenRes int resId) {
      return minWidthPx(mResourceResolver.resolveDimenSizeRes(resId));
    }

    public T onInitializeAccessibilityEventHandler(
        @Nullable
            EventHandler<OnInitializeAccessibilityEventEvent>
                onInitializeAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
      return getThis();
    }

    public T onInitializeAccessibilityNodeInfoHandler(
        @Nullable
            EventHandler<OnInitializeAccessibilityNodeInfoEvent>
                onInitializeAccessibilityNodeInfoHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
      return getThis();
    }

    public T onPopulateAccessibilityEventHandler(
        @Nullable
            EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
      return getThis();
    }

    public T onRequestSendAccessibilityEventHandler(
        @Nullable
            EventHandler<OnRequestSendAccessibilityEventEvent>
                onRequestSendAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
      return getThis();
    }

    public T outlineProvider(@Nullable ViewOutlineProvider outlineProvider) {
      mComponent.getOrCreateCommonProps().outlineProvider(outlineProvider);
      return getThis();
    }

    /**
     * @see #paddingPx
     */
    public T paddingAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      return paddingPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #paddingPx
     */
    public T paddingAttr(YogaEdge edge, @AttrRes int resId) {
      return paddingAttr(edge, resId, 0);
    }

    /**
     * @see #paddingPx
     */
    public T paddingDip(YogaEdge edge, @Dimension(unit = DP) float padding) {
      return paddingPx(edge, mResourceResolver.dipsToPixels(padding));
    }

    /**
     * @param percent a value between 0 and 100.
     * @see #paddingPx
     */
    public T paddingPercent(YogaEdge edge, float percent) {
      mComponent.getOrCreateCommonProps().paddingPercent(edge, percent);
      return getThis();
    }

    /**
     * Affects the size of the node it is applied to. Padding will not add to the total size of an
     * element if it has an explicit size set. See <a
     * href="https://yogalayout.dev/docs/margins-paddings-borders">https://yogalayout.dev/docs/margins-paddings-borders</a>
     * for more information
     */
    public T paddingPx(YogaEdge edge, @Px int padding) {
      mComponent.getOrCreateCommonProps().paddingPx(edge, padding);
      return getThis();
    }

    /**
     * @see #paddingPx
     */
    public T paddingRes(YogaEdge edge, @DimenRes int resId) {
      return paddingPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
    }

    public T performAccessibilityActionHandler(
        @Nullable EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
      mComponent
          .getOrCreateCommonProps()
          .performAccessibilityActionHandler(performAccessibilityActionHandler);
      return getThis();
    }

    /**
     * @see #positionPx
     */
    public T positionAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      return positionPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #positionPx
     */
    public T positionAttr(YogaEdge edge, @AttrRes int resId) {
      return positionAttr(edge, resId, 0);
    }

    /**
     * @see #positionPx
     */
    public T positionDip(YogaEdge edge, @Dimension(unit = DP) float position) {
      return positionPx(edge, mResourceResolver.dipsToPixels(position));
    }

    /**
     * @param percent a value between 0 and 100.
     * @see #positionPx
     */
    public T positionPercent(YogaEdge edge, float percent) {
      mComponent.getOrCreateCommonProps().positionPercent(edge, percent);
      return getThis();
    }

    /**
     * When used in combination with {@link #positionType} of {@link YogaPositionType#ABSOLUTE},
     * allows the component to specify how it should be positioned within its parent. See <a
     * href="https://yogalayout.dev/docs/absolute-relative-layout">https://yogalayout.dev/docs/absolute-relative-layout</a>
     * for more information.
     */
    public T positionPx(YogaEdge edge, @Px int position) {
      mComponent.getOrCreateCommonProps().positionPx(edge, position);
      return getThis();
    }

    /**
     * @see #positionPx
     */
    public T positionRes(YogaEdge edge, @DimenRes int resId) {
      return positionPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
    }

    /**
     * Controls how this component will be positioned within its parent. See <a
     * href="https://yogalayout.dev/docs/absolute-relative-layout">https://yogalayout.dev/docs/absolute-relative-layout</a>
     * for more details.
     *
     * <p>Default: {@link YogaPositionType#RELATIVE}
     */
    public T positionType(YogaPositionType positionType) {
      mComponent.getOrCreateCommonProps().positionType(positionType);
      return getThis();
    }

    /**
     * Sets the degree that this component is rotated around the pivot point. Increasing the value
     * results in clockwise rotation. By default, the pivot point is centered on the component.
     */
    public T rotation(float rotation) {
      mComponent.getOrCreateCommonProps().rotation(rotation);
      return getThis();
    }

    /**
     * Links a {@link DynamicValue} object to the rotation value for this Component
     *
     * @param rotation controller for the rotation value
     */
    public T rotation(@Nullable DynamicValue<Float> rotation) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_ROTATION, rotation);
      return getThis();
    }

    /**
     * Sets the degree that this component is rotated around the horizontal axis through the pivot
     * point.
     */
    public T rotationX(float rotationX) {
      mComponent.getOrCreateCommonProps().rotationX(rotationX);
      return getThis();
    }

    /**
     * Sets the degree that this component is rotated around the vertical axis through the pivot
     * point.
     */
    public T rotationY(float rotationY) {
      mComponent.getOrCreateCommonProps().rotationY(rotationY);
      return getThis();
    }

    /**
     * Sets the scale (scaleX and scaleY) on this component. This is mostly relevant for animations
     * and being able to animate size changes. Otherwise for non-animation usecases, you should use
     * the standard layout properties to control the size of your component.
     */
    public T scale(float scale) {
      mComponent.getOrCreateCommonProps().scale(scale);
      return getThis();
    }

    /**
     * Links a {@link DynamicValue} object to the scaleX value for this Component
     *
     * @param value controller for the scaleX value
     */
    public T scaleX(DynamicValue<Float> value) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_SCALE_X, value);
      return getThis();
    }

    /**
     * Links a {@link DynamicValue} object to the scaleY value for this Component
     *
     * @param value controller for the scaleY value
     */
    public T scaleY(DynamicValue<Float> value) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_SCALE_Y, value);
      return getThis();
    }

    public T selected(boolean isSelected) {
      mComponent.getOrCreateCommonProps().selected(isSelected);
      return getThis();
    }

    public T sendAccessibilityEventHandler(
        @Nullable EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .sendAccessibilityEventHandler(sendAccessibilityEventHandler);
      return getThis();
    }

    public T sendAccessibilityEventUncheckedHandler(
        @Nullable
            EventHandler<SendAccessibilityEventUncheckedEvent>
                sendAccessibilityEventUncheckedHandler) {
      mComponent
          .getOrCreateCommonProps()
          .sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);
      return getThis();
    }

    public T onPerformActionForVirtualViewHandler(
        @Nullable
            EventHandler<PerformActionForVirtualViewEvent> onPerformActionForVirtualViewHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onPerformActionForVirtualViewHandler(onPerformActionForVirtualViewHandler);
      return getThis();
    }

    public T onVirtualViewKeyboardFocusChangedHandler(
        @Nullable
            EventHandler<VirtualViewKeyboardFocusChangedEvent>
                onVirtualViewKeyboardFocusChangedHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onVirtualViewKeyboardFocusChangedHandler(onVirtualViewKeyboardFocusChangedHandler);
      return getThis();
    }

    public T shadowElevationAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return shadowElevationPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    public T shadowElevationAttr(@AttrRes int resId) {
      return shadowElevationAttr(resId, 0);
    }

    public T shadowElevationDip(@Dimension(unit = DP) float shadowElevation) {
      return shadowElevationPx(mResourceResolver.dipsToPixels(shadowElevation));
    }

    /**
     * Shadow elevation and outline provider methods are only functional on {@link
     * android.os.Build.VERSION_CODES#LOLLIPOP} and above.
     */
    public T shadowElevationPx(float shadowElevation) {
      mComponent.getOrCreateCommonProps().shadowElevationPx(shadowElevation);
      return getThis();
    }

    public T shadowElevationRes(@DimenRes int resId) {
      return shadowElevationPx(mResourceResolver.resolveDimenSizeRes(resId));
    }

    /**
     * Links a {@link DynamicValue} object to the elevation value for this Component
     *
     * @param value controller for the elevation value
     */
    public T shadowElevation(DynamicValue<Float> value) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mComponent.getOrCreateCommonDynamicProps().put(KEY_ELEVATION, value);
      }
      return getThis();
    }

    /**
     * Ports {@link android.view.View#setStateListAnimator(android.animation.StateListAnimator)}
     * into components world. However, since the aforementioned view's method is available only on
     * API 21 and above, calling this method on lower APIs will have no effect. On the legit
     * versions, on the other hand, calling this method will lead to the component being wrapped
     * into a view
     */
    public T stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mComponent.getOrCreateCommonProps().stateListAnimator(stateListAnimator);
      }
      return getThis();
    }

    /**
     * Ports {@link android.view.View#setStateListAnimator(android.animation.StateListAnimator)}
     * into components world. However, since the aforementioned view's method is available only on
     * API 21 and above, calling this method on lower APIs will have no effect. On the legit
     * versions, on the other hand, calling this method will lead to the component being wrapped
     * into a view
     */
    public T stateListAnimatorRes(@DrawableRes int resId) {
      if (Build.VERSION.SDK_INT >= 26) {
        // We cannot do it on the versions prior to Android 8.0 since there is a possible race
        // condition when loading state list animators, thus we will avoid doing it off the UI
        // thread
        return stateListAnimator(
            AnimatorInflater.loadStateListAnimator(mContext.getAndroidContext(), resId));
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mComponent.getOrCreateCommonProps().stateListAnimatorRes(resId);
      }
      return getThis();
    }

    public T testKey(@Nullable String testKey) {
      mComponent.getOrCreateCommonProps().testKey(testKey);
      return getThis();
    }

    public T componentTag(@Nullable Object componentTag) {
      mComponent.getOrCreateCommonProps().componentTag(componentTag);
      return getThis();
    }

    public T touchExpansionAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      return touchExpansionPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    public T touchExpansionAttr(YogaEdge edge, @AttrRes int resId) {
      return touchExpansionAttr(edge, resId, 0);
    }

    public T touchExpansionDip(YogaEdge edge, @Dimension(unit = DP) float touchExpansion) {
      return touchExpansionPx(edge, mResourceResolver.dipsToPixels(touchExpansion));
    }

    public T touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
      mComponent.getOrCreateCommonProps().touchExpansionPx(edge, touchExpansion);
      return getThis();
    }

    public T touchExpansionRes(YogaEdge edge, @DimenRes int resId) {
      return touchExpansionPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
    }

    public T touchHandler(@Nullable EventHandler<TouchEvent> touchHandler) {
      mComponent.getOrCreateCommonProps().touchHandler(touchHandler);
      return getThis();
    }

    public T transitionKey(@Nullable String key) {
      mComponent.getOrCreateCommonProps().transitionKey(key, mComponent.getOwnerGlobalKey());
      if (mComponent.getOrCreateCommonProps().getTransitionKeyType() == null) {
        // If TransitionKeyType isn't set, set to default type
        transitionKeyType(Transition.DEFAULT_TRANSITION_KEY_TYPE);
      }
      return getThis();
    }

    public T transitionName(@Nullable String transitionName) {
      mComponent.getOrCreateCommonProps().transitionName(transitionName);
      return getThis();
    }

    public T transitionKeyType(Transition.TransitionKeyType type) {
      if (type == null) {
        throw new IllegalArgumentException("TransitionKeyType must not be null");
      }
      mComponent.getOrCreateCommonProps().transitionKeyType(type);
      return getThis();
    }

    /**
     * Links a {@link DynamicValue} object to the translationX value for this Component
     *
     * @param value controller for the translationY value
     */
    public T translationX(DynamicValue<Float> value) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_TRANSLATION_X, value);
      return getThis();
    }

    /**
     * Links a {@link DynamicValue} object to the translationY value for this Component
     *
     * @param value controller for the translationY value
     */
    public T translationY(DynamicValue<Float> value) {
      mComponent.getOrCreateCommonDynamicProps().put(KEY_TRANSLATION_Y, value);
      return getThis();
    }

    public T unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
      mComponent.getOrCreateCommonProps().unfocusedHandler(unfocusedHandler);
      return getThis();
    }

    /**
     * When set to true, overrides the default behaviour of baseline calculation and uses height of
     * component as baseline. By default the baseline of a component is the baseline of first child
     * of component (If the component does not have any child then baseline is height of the
     * component)
     */
    public T useHeightAsBaseline(boolean useHeightAsBaseline) {
      mComponent.getOrCreateCommonProps().useHeightAsBaseline(useHeightAsBaseline);
      return getThis();
    }

    public T viewId(int id) {
      mComponent.getOrCreateCommonProps().viewId(id);
      return getThis();
    }

    public T viewTag(@Nullable Object viewTag) {
      mComponent.getOrCreateCommonProps().viewTag(viewTag);
      return getThis();
    }

    public T viewTags(@Nullable SparseArray<Object> viewTags) {
      mComponent.getOrCreateCommonProps().viewTags(viewTags);
      return getThis();
    }

    public T visibilityChangedHandler(
        @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
      mComponent.getOrCreateCommonProps().visibilityChangedHandler(visibilityChangedHandler);
      return getThis();
    }

    public T visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
      mComponent.getOrCreateCommonProps().visibleHandler(visibleHandler);
      return getThis();
    }

    public T visibleHeightRatio(float visibleHeightRatio) {
      mComponent.getOrCreateCommonProps().visibleHeightRatio(visibleHeightRatio);
      return getThis();
    }

    public T visibleWidthRatio(float visibleWidthRatio) {
      mComponent.getOrCreateCommonProps().visibleWidthRatio(visibleWidthRatio);
      return getThis();
    }

    /**
     * @see #widthPx
     */
    public T widthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return widthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
    }

    /**
     * @see #widthPx
     */
    public T widthAttr(@AttrRes int resId) {
      return widthAttr(resId, 0);
    }

    /**
     * @see #widthPx
     */
    public T widthDip(@Dimension(unit = DP) float width) {
      return widthPx(mResourceResolver.dipsToPixels(width));
    }

    /**
     * Sets the width of the Component to be a percentage of its parent's width. Note that if the
     * parent has unspecified width (e.g. it is an HScroll), then setting this will have no effect.
     *
     * @param percent a value between 0 and 100.
     * @see #widthPx
     */
    public T widthPercent(float percent) {
      mComponent.getOrCreateCommonProps().widthPercent(percent);
      return getThis();
    }

    /**
     * Specifies the width of the element's content area. See <a
     * href="https://yogalayout.dev/docs/width-height">https://yogalayout.dev/docs/width-height</a>
     * for more information
     */
    public T widthPx(@Px int width) {
      mComponent.getOrCreateCommonProps().widthPx(width);
      return getThis();
    }

    /**
     * @see #widthPx
     */
    public T widthRes(@DimenRes int resId) {
      return widthPx(mResourceResolver.resolveDimenSizeRes(resId));
    }

    public T wrapInView() {
      mComponent.getOrCreateCommonProps().wrapInView();
      return getThis();
    }

    public T layerType(@LayerType int type, @Nullable Paint paint) {
      mComponent.getOrCreateCommonProps().layerType(type, paint);
      return getThis();
    }

    public T keyboardNavigationCluster(boolean isKeyboardNavigationCluster) {
      mComponent.getOrCreateCommonProps().keyboardNavigationCluster(isKeyboardNavigationCluster);
      return getThis();
    }

    public T addSystemGestureExclusionZone(Function1<Rect, Rect> exclusion) {
      mComponent.getOrCreateCommonProps().addSystemGestureExclusionZone(exclusion);
      return getThis();
    }

    public T tooltipText(@Nullable String tooltipText) {
      mComponent.getOrCreateCommonProps().tooltipText(tooltipText);
      return getThis();
    }

    @Deprecated
    public T visibilityOutputTag(@Nullable String visibilityOutputTag) {
      mComponent.getOrCreateCommonProps().visibilityOutputTag(visibilityOutputTag);
      return getThis();
    }

    private @Nullable Component getOwner() {
      return mContext.getComponentScope();
    }

    /**
     * Note: This is exposed for backwards compatibility with the Kotlin API to allow applying
     * common props via Style without moving Style into Java. Use with caution since at this point
     * the Component is still being built and should not escape the Builder.
     */
    final Component getComponent() {
      return mComponent;
    }

    /**
     * Checks that all the required props are supplied, and if not throws a useful exception
     *
     * @param requiredPropsCount expected number of props
     * @param required the bit set that identifies which props have been supplied
     * @param requiredPropsNames the names of all props used for a useful error message
     */
    protected static void checkArgs(
        int requiredPropsCount, BitSet required, String[] requiredPropsNames) {
      if (required != null && required.nextClearBit(0) < requiredPropsCount) {
        List<String> missingProps = new ArrayList<>();
        for (int i = 0; i < requiredPropsCount; i++) {
          if (!required.get(i)) {
            missingProps.add(requiredPropsNames[i]);
          }
        }
        throw new IllegalStateException(
            "The following props are not marked as optional and were not supplied: "
                + Arrays.toString(missingProps.toArray()));
      }
    }
  }

  public abstract static class ContainerBuilder<T extends ContainerBuilder<T>> extends Builder<T> {

    protected ContainerBuilder(
        ComponentContext c, int defStyleAttr, int defStyleRes, Component component) {
      super(c, defStyleAttr, defStyleRes, component);
    }

    /**
     * The AlignSelf property has the same options and effect as AlignItems but instead of affecting
     * the children within a container, you can apply this property to a single child to change its
     * alignment within its parent. See <a
     * href="https://yogalayout.dev/docs/align-content">https://yogalayout.dev/docs/align-content</a>
     * for more information.
     *
     * <p>Default: {@link YogaAlign#AUTO}
     */
    public abstract T alignContent(@Nullable YogaAlign alignContent);

    /**
     * The AlignItems property describes how to align children along the cross axis of their
     * container. AlignItems is very similar to JustifyContent but instead of applying to the main
     * axis, it applies to the cross axis. See <a
     * href="https://yogalayout.dev/docs/align-items">https://yogalayout.dev/docs/align-items</a>
     * for more information.
     *
     * <p>Default: {@link YogaAlign#STRETCH}
     */
    public abstract T alignItems(@Nullable YogaAlign alignItems);

    public abstract T child(@Nullable Component child);

    public abstract T child(@Nullable Component.Builder<?> child);

    /**
     * The JustifyContent property describes how to align children within the main axis of a
     * container. For example, you can use this property to center a child horizontally within a
     * container with FlexDirection = Row or vertically within one with FlexDirection = Column. See
     * <a
     * href="https://yogalayout.dev/docs/justify-content">https://yogalayout.dev/docs/justify-content</a>
     * for more information.
     *
     * <p>Default: {@link YogaJustify#FLEX_START}
     */
    public abstract T justifyContent(@Nullable YogaJustify justifyContent);

    /** Set this to true if you want the container to be laid out in reverse. */
    public abstract T reverse(boolean reverse);

    /**
     * The FlexWrap property is set on containers and controls what happens when children overflow
     * the size of the container along the main axis. If a container specifies {@link YogaWrap#WRAP}
     * then its children will wrap to the next line instead of overflowing.
     *
     * <p>The next line will have the same FlexDirection as the first line and will appear next to
     * the first line along the cross axis - below it if using FlexDirection = Column and to the
     * right if using FlexDirection = Row. See <a
     * href="https://yogalayout.dev/docs/flex-wrap">https://yogalayout.dev/docs/flex-wrap</a> for
     * more information.
     *
     * <p>Default: {@link YogaWrap#NO_WRAP}
     */
    public abstract T wrap(@Nullable YogaWrap wrap);

    /**
     * The Gap property is set on containers and spaces children evenly by a given length along a
     * given axis
     */
    public abstract T gapDip(YogaGutter gutter, float dip);

    public abstract T gapPx(YogaGutter gutter, int px);
  }

  @Nullable
  public static <T> T getTreePropFromParent(
      TreePropContainer parentTreePropContainer, Class<T> key) {
    return parentTreePropContainer == null ? null : parentTreePropContainer.get(key);
  }

  static LinkedList<String> generateHierarchy(String globalKey) {
    LinkedList<String> list = new LinkedList<>();
    String[] keys = globalKey.split(",");

    synchronized (sTypeIdByComponentType) {
      for (String key : keys) {
        String name = ComponentKeyUtils.mapToSimpleName(key, sTypeIdByComponentType);
        list.add(name);
      }
    }

    return list;
  }

  /**
   * Holds the attributes for this component. These attributes are used in the scope of the testing
   * API, which can verify if a given Component has a given attribute.
   *
   * <p>In release builds it should be null, as this code is only used for testing purposes.
   */
  @Nullable
  private final AttributesHolder mDebugAttributesHolder =
      LithoDebugConfigurations.isDebugModeEnabled ? new AttributesHolder() : null;

  @Override
  public <T> void setDebugAttributeKey(AttributeKey<T> attributeKey, T value) {
    if (mDebugAttributesHolder != null) {
      mDebugAttributesHolder.setDebugAttributeKey(attributeKey, value);
    }
  }

  public <T> T getDebugAttribute(AttributeKey<T> attributeKey) {
    if (mDebugAttributesHolder == null) {
      throw new RuntimeException("This shouldn't get accessed when not initialized");
    }

    return mDebugAttributesHolder.get(attributeKey);
  }

  public Map<AttributeKey<?>, Object> getDebugAttributes() {
    if (mDebugAttributesHolder == null) {
      throw new RuntimeException("This shouldn't get accessed when not initialized");
    }
    return mDebugAttributesHolder.getAttributes();
  }
}
