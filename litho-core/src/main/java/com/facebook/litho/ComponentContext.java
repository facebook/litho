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

import static com.facebook.litho.ComponentContextUtils.buildDefaultLithoConfiguration;
import static com.facebook.litho.StateContainer.StateUpdate;
import static com.facebook.rendercore.debug.DebugEventAttribute.Source;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.annotations.EventHandlerRebindMode;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.debug.DebugInfoReporter;
import com.facebook.litho.state.ComponentState;
import com.facebook.litho.state.StateProvider;
import com.facebook.rendercore.LayoutCache;
import com.facebook.rendercore.ResourceCache;
import com.facebook.rendercore.ResourceResolver;
import java.util.LinkedList;
import java.util.Queue;
import kotlin.Unit;

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ComponentContext {

  static final String NO_SCOPE_EVENT_HANDLER = "ComponentContext:NoScopeEventHandler";

  private final Context mContext;

  public LithoConfiguration mLithoConfiguration;

  private @Nullable String mNoStateUpdatesMethod;

  // Hold a reference to the component which scope we are currently within.
  @Nullable
  @ThreadConfined(ThreadConfined.ANY)
  private Component mComponentScope;

  @ThreadConfined(ThreadConfined.ANY)
  @Nullable
  String mGlobalKey;

  @ThreadConfined(ThreadConfined.ANY)
  private final ResourceResolver mResourceResolver;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable TreePropContainer mTreePropContainer;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable TreePropContainer mParentTreePropContainer;

  @ThreadConfined(ThreadConfined.ANY)
  private boolean mIsParentTreePropsCloned;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable final LithoTree mLithoTree;

  private @Nullable LithoVisibilityEventsController mLithoVisibilityEventsController;

  // Used to hold styling information applied to components
  @StyleRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleRes = 0;

  @AttrRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleAttr = 0;

  private @Nullable ScopedComponentInfo mScopedComponentInfo;

  private boolean isNestedTreeContext;

  private final ThreadLocal<CalculationContext> mCalculationStateContextThreadLocal;

  /**
   * This constructors takes a {@link Context}. This should only be used to create {@link
   * ComponentTree}
   */
  public ComponentContext(Context context) {
    this(context, null, null);
  }

  public ComponentContext(
      Context context,
      @Nullable LithoConfiguration lithoConfiguration,
      @Nullable TreePropContainer treePropContainer) {
    this(context, treePropContainer, lithoConfiguration, null, null, null, null, null);
  }

  ComponentContext(
      Context androidContext,
      @Nullable TreePropContainer treePropContainer,
      @Nullable LithoConfiguration lithoConfiguration,
      @Nullable LithoTree lithoTree,
      @Nullable String globalKey,
      @Nullable LithoVisibilityEventsController lithoVisibilityEventsController,
      @Nullable Component componentScope,
      @Nullable TreePropContainer parentTreePropContainer) {
    mCalculationStateContextThreadLocal = new ThreadLocal<>();
    mContext =
        Preconditions.checkNotNull(
            androidContext, "ComponentContext requires a non null Android Context");
    mResourceResolver =
        new ResourceResolver(
            androidContext,
            ResourceCache.getLatest(androidContext.getResources().getConfiguration()));
    mTreePropContainer = treePropContainer;
    mLithoConfiguration =
        lithoConfiguration != null
            ? lithoConfiguration
            : buildDefaultLithoConfiguration(
                mContext, ComponentsConfiguration.defaultInstance, null);

    if (mLithoConfiguration.componentsConfig.componentsLogger != null
        && mLithoConfiguration.componentsConfig.logTag == null) {
      throw new IllegalStateException("When a ComponentsLogger is set, a LogTag must be set");
    }

    mLithoTree = lithoTree;
    mGlobalKey = globalKey;
    mLithoVisibilityEventsController = lithoVisibilityEventsController;
    mComponentScope = componentScope;
    mParentTreePropContainer = parentTreePropContainer;
  }

  public ComponentContext(ComponentContext context) {
    this(context, context.mTreePropContainer);
  }

  protected ComponentContext(
      ComponentContext context, @Nullable TreePropContainer treePropContainer) {
    mContext = context.mContext;
    mResourceResolver = context.mResourceResolver;
    mComponentScope = context.mComponentScope;
    mLithoVisibilityEventsController = context.mLithoVisibilityEventsController;
    mLithoTree = context.mLithoTree;
    mTreePropContainer = treePropContainer != null ? treePropContainer : context.mTreePropContainer;
    mParentTreePropContainer = context.mParentTreePropContainer;
    mGlobalKey = context.mGlobalKey;
    mCalculationStateContextThreadLocal = context.mCalculationStateContextThreadLocal;
    mLithoConfiguration = context.mLithoConfiguration;
  }

  ComponentContext makeNewCopy() {
    return new ComponentContext(this);
  }

  /**
   * Creates a new ComponentContext instance scoped to the given component and sets it on the
   * component.
   *
   * @param parentContext context scoped to the parent component
   * @param scope component associated with the newly created scoped context
   * @return a new ComponentContext instance scoped to the given component
   */
  static ComponentContext withComponentScope(
      final ComponentContext parentContext,
      final Component scope,
      final @Nullable String globalKey) {
    ComponentContext componentContext = parentContext.makeNewCopy();
    componentContext.mComponentScope = scope;
    componentContext.mGlobalKey = globalKey;
    componentContext.mParentTreePropContainer = parentContext.mTreePropContainer;
    // TODO: T124275447 make these Component Context fields final
    // Either this component is nested tree or descendant of nested tree component
    componentContext.isNestedTreeContext =
        Component.isNestedTree(scope) || parentContext.isNestedTreeContext;

    final EventHandler<ErrorEvent> errorEventHandler =
        ComponentUtils.createOrGetErrorEventHandler(scope, parentContext, componentContext);
    componentContext.mScopedComponentInfo =
        new ScopedComponentInfo(scope, componentContext, errorEventHandler);

    return componentContext;
  }

  /**
   * @deprecated introduced for legacy test cases - don't add new callers
   */
  @VisibleForTesting
  @Deprecated
  public static ComponentContext createScopedComponentContextWithStateForTest(
      ComponentContext parent, Component component, String key) {
    final ComponentContext context = ComponentContext.withComponentScope(parent, component, key);
    // SpecGeneratedComponents expect a StateContainer to be set on all scoped ComponentContexts
    // before any lifecycle methods are invoked.
    if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).hasState()) {
      ComponentState<StateContainer> state =
          new ComponentState(
              ((SpecGeneratedComponent) component).createInitialStateContainer(context), null);
      context.getScopedComponentInfo().setState(state);
    }
    return context;
  }

  /**
   * Creates a new ComponentContext based on the given ComponentContext, propagating log tag,
   * logger, and tree props. This should be used when creating a ComponentContext for a nested
   * ComponentTree (e.g. see HorizontalScrollSpec).
   */
  public static ComponentContext makeCopyForNestedTree(ComponentContext parentTreeContext) {
    return new ComponentContext(
        parentTreeContext.getAndroidContext(),
        parentTreeContext.getTreePropContainerCopy(),
        parentTreeContext.mLithoConfiguration,
        null,
        null,
        null,
        null,
        null);
  }

  /** Returns the current calculate state context */
  @Nullable
  CalculationContext getCalculationStateContext() {
    return mCalculationStateContextThreadLocal.get();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setLithoLayoutContext(LithoLayoutContext lithoLayoutContext) {
    mCalculationStateContextThreadLocal.set(lithoLayoutContext);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void clearCalculationStateContext() {
    mCalculationStateContextThreadLocal.set(null);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setLayoutStateContextForTesting() {
    setLithoLayoutContext(
        new LithoLayoutContext(
            -1,
            new MeasuredResultCache(),
            this,
            new TreeState(),
            0,
            -1,
            true,
            new LayoutCache(),
            null,
            null));
  }

  /**
   * For test usage. This method will ensure this ComponentContext has a RenderStateContex. This is
   * critical for tests that trigger layout calculation functionality outside of a LayoutState
   * calculation (i.e., including willRender, Layout API, caching, etc).
   */
  @VisibleForTesting
  public ResolveContext setRenderStateContextForTests() {
    final ResolveContext resolveContext =
        new ResolveContext(
            -1, new MeasuredResultCache(), new TreeState(), 0, -1, true, null, null, false);
    setRenderStateContext(resolveContext);

    return resolveContext;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setRenderStateContext(@Nullable ResolveContext renderStateContext) {
    mCalculationStateContextThreadLocal.set(renderStateContext);
  }

  /**
   * Sets the calculation state context (either a RenderStateContext or LayoutStateContext) on a
   * thread local, making it unique per ComponentTree, per Thread.
   */
  void setCalculationStateContext(@Nullable CalculationContext context) {
    mCalculationStateContextThreadLocal.set(context);
  }

  /** Returns true if this method is called during layout creation. */
  boolean isCreateLayoutInProgress() {
    return mCalculationStateContextThreadLocal.get() != null;
  }

  public final Context getAndroidContext() {
    return mContext;
  }

  public final Context getApplicationContext() {
    return mContext.getApplicationContext();
  }

  public Resources getResources() {
    return mContext.getResources();
  }

  public ResourceResolver getResourceResolver() {
    return mResourceResolver;
  }

  public CharSequence getText(@StringRes int resId) {
    CharSequence text = mResourceResolver.resolveText(resId);
    if (text == null) {
      throw new RuntimeException(
          "String resource not found for ID #0x" + Integer.toHexString(resId));
    }
    return text;
  }

  public String getString(@StringRes int resId) {
    String text = mResourceResolver.resolveStringRes(resId);
    if (text == null) {
      throw new RuntimeException(
          "String resource not found for ID #0x" + Integer.toHexString(resId));
    }

    return text;
  }

  public String getString(@StringRes int resId, Object... formatArgs) {
    String text = mResourceResolver.resolveStringRes(resId, formatArgs);
    if (text == null) {
      throw new RuntimeException(
          "String resource not found for ID #0x" + Integer.toHexString(resId));
    }

    return text;
  }

  public int getColor(@ColorRes int id) {
    return mResourceResolver.resolveColorRes(id);
  }

  @Nullable
  public Component getComponentScope() {
    return mComponentScope;
  }

  public String getGlobalKey() {
    if (mComponentScope == null) {
      throw new RuntimeException(
          "getGlobalKey cannot be accessed from a ComponentContext without a scope");
    }

    if (mGlobalKey == null) {
      return "undefined";
    }
    return mGlobalKey;
  }

  public EventHandler<ErrorEvent> getErrorEventHandler() {
    if (mComponentScope != null) {
      try {
        final EventHandler<ErrorEvent> errorEventHandler =
            getScopedComponentInfo().getErrorEventHandler();
        if (errorEventHandler != null) {
          return errorEventHandler;
        }
      } catch (IllegalStateException e) {
        return mLithoConfiguration.componentsConfig.errorEventHandler;
      }
    }

    return mLithoConfiguration.componentsConfig.errorEventHandler;
  }

  @Nullable
  @VisibleForTesting
  public TreeFuture getLayoutStateFuture() {
    return mCalculationStateContextThreadLocal.get() != null
        ? mCalculationStateContextThreadLocal.get().getTreeFuture()
        : null;
  }

  public LithoConfiguration getLithoConfiguration() {
    return mLithoConfiguration;
  }

  /**
   * Notify the Component Tree that it needs to synchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  public void updateStateSync(StateUpdate stateUpdate, String attribution) {
    checkIfNoStateUpdatesMethod();

    if (mLithoTree == null) {
      return;
    }

    mLithoTree
        .getStateUpdater()
        .updateStateSync(getGlobalKey(), stateUpdate, attribution, isNestedTreeContext());
  }

  /**
   * Notify the Component Tree that it needs to asynchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  public void updateStateAsync(StateUpdate stateUpdate, String attribution) {
    checkIfNoStateUpdatesMethod();

    if (mLithoTree == null) {
      return;
    }

    mLithoTree
        .getStateUpdater()
        .updateStateAsync(getGlobalKey(), stateUpdate, attribution, isNestedTreeContext());
  }

  public void updateStateWithTransition(StateUpdate stateUpdate, String attribution) {
    updateStateAsync(stateUpdate, attribution);
  }

  public void updateStateLazy(StateUpdate stateUpdate) {
    if (mLithoTree == null) {
      return;
    }

    mLithoTree
        .getStateUpdater()
        .updateStateLazy(getGlobalKey(), stateUpdate, isNestedTreeContext());
  }

  final void updateHookStateAsync(String globalKey, HookUpdater updateBlock) {
    checkIfNoStateUpdatesMethod();

    if (mLithoTree == null) {
      return;
    }

    final Component scope = getComponentScope();
    mLithoTree
        .getStateUpdater()
        .updateHookStateAsync(
            globalKey,
            updateBlock,
            scope != null ? scope.getSimpleName() : "hook",
            isNestedTreeContext());
  }

  final void updateHookStateSync(String globalKey, HookUpdater updateBlock) {
    checkIfNoStateUpdatesMethod();

    if (mLithoTree == null) {
      return;
    }

    final Component scope = getComponentScope();
    mLithoTree
        .getStateUpdater()
        .updateHookStateSync(
            globalKey,
            updateBlock,
            scope != null ? scope.getSimpleName() : "hook",
            isNestedTreeContext());
  }

  /**
   * @return A StateContainer with lazy state updates applied. This may be the same container passed
   *     in if there were no updates to apply. This method won't mutate the passed container.
   */
  public StateContainer applyLazyStateUpdatesForContainer(StateContainer container) {
    if (mLithoTree == null) {
      return container;
    }

    return mLithoTree
        .getStateUpdater()
        .applyLazyStateUpdatesForContainer(getGlobalKey(), container, isNestedTreeContext());
  }

  void enterNoStateUpdatesMethod(String noStateUpdatesMethod) {
    mNoStateUpdatesMethod = noStateUpdatesMethod;
  }

  void exitNoStateUpdatesMethod() {
    mNoStateUpdatesMethod = null;
  }

  private void checkIfNoStateUpdatesMethod() {
    if (mNoStateUpdatesMethod != null) {
      throw new IllegalStateException(
          "Updating the state of a component during "
              + mNoStateUpdatesMethod
              + " leads to unexpected behaviour, consider using lazy state updates.");
    }
  }

  void setDefStyle(@AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    mDefStyleAttr = defStyleAttr;
    mDefStyleRes = defStyleRes;
  }

  public TypedArray obtainStyledAttributes(int[] attrs, @AttrRes int defStyleAttr) {
    return mContext.obtainStyledAttributes(
        null, attrs, defStyleAttr != 0 ? defStyleAttr : mDefStyleAttr, mDefStyleRes);
  }

  @Nullable
  public String getLogTag() {
    return mLithoConfiguration.componentsConfig.logTag;
  }

  @Nullable
  public ComponentsLogger getLogger() {
    return mLithoConfiguration.componentsConfig.componentsLogger;
  }

  /**
   * A utility function to find the View with a given tag under the current Component's LithoView.
   * To set a view tag, use the .viewTag() common prop or Style.viewTag. An appropriate time to call
   * this is in your Component's onVisible callback.
   *
   * <p>As with View.findViewWithTag in general, this must be called on the main thread.
   *
   * <p>Note that null may be returned if the associated View doesn't exist or isn't mounted: with
   * incremental mount turned on (which is the default), if the component is off-screen, it won't be
   * mounted.
   *
   * <p>Finally, note that you should never hold a reference to the view returned by this function
   * as Litho may unmount your Component and mount it to a different View.
   */
  public @Nullable <T extends View> T findViewWithTag(Object tag) {
    ThreadUtils.assertMainThread();

    if (mLithoTree == null) {
      throw new RuntimeException(
          "Calling findViewWithTag on a ComponentContext which isn't associated with a Tree. Make"
              + " sure it's one received in `render` or `onCreateLayout`");
    }
    final View mountedView = mLithoTree.getMountedViewReference().getMountedView();
    // The tree isn't mounted
    if (mountedView == null) {
      return null;
    }

    return mountedView.findViewWithTag(tag);
  }

  /**
   * A utility function to find the View with a given tag under the current Component's LithoView.
   * To set a view tag, use the .viewTags() common prop or Style.viewTags. An appropriate time to
   * call this is in your Component's onVisible callback.
   *
   * <p>As with View.findViewWithTag in general, this must be called on the main thread.
   *
   * <p>Note that null may be returned if the associated View doesn't exist or isn't mounted: with
   * incremental mount turned on (which is the default), if the component is off-screen, it won't be
   * mounted.
   *
   * <p>Finally, note that you should never hold a reference to the view returned by this function
   * as Litho may unmount your Component and mount it to a different View.
   */
  public @Nullable View findViewWithTagValue(int key, Object tag) {
    if (mLithoTree == null) {
      return null;
    }
    final View mountedView = mLithoTree.getMountedViewReference().getMountedView();
    // The tree isn't mounted
    if (mountedView == null) {
      return null;
    }

    Queue<View> queue = new LinkedList<>();
    queue.add(mountedView);

    while (!queue.isEmpty()) {
      View currentView = queue.poll();
      if (currentView == null) {
        continue;
      }
      if (currentView.getTag(key) == tag) {
        return currentView;
      } else if (currentView instanceof ViewGroup) {
        ViewGroup groupView = (ViewGroup) currentView;
        final int childCount = groupView.getChildCount();
        for (int i = 0; i < childCount; i++) {
          queue.add(groupView.getChildAt(i));
        }
      }
    }
    return null;
  }

  public @Nullable ErrorComponentReceiver getErrorComponentReceiver() {
    if (mLithoTree == null) {
      return null;
    }
    return mLithoTree.getErrorComponentReceiver();
  }

  /**
   * Calculates a returns a unique ID for a given component key and output type. The IDs will be
   * unique for components in the ComponentTree this ID generator is linked with. If an ID was
   * already generated for a given component, the same ID will be returned. Otherwise, a new unique
   * ID will be generated.
   *
   * <p>ComponentContext must have a ComponentTree instance.
   *
   * @param componentKey The component key
   * @param type The output type @see OutputUnitType
   */
  public long calculateLayoutOutputId(final String componentKey, final @OutputUnitType int type) {
    if (mLithoConfiguration.renderUnitIdGenerator == null) {
      throw new IllegalStateException("Cannot generate IDs with a null renderUnitIdGenerator");
    }

    return mLithoConfiguration.renderUnitIdGenerator.calculateLayoutOutputId(componentKey, type);
  }

  @Nullable
  LithoTree getLithoTree() {
    return mLithoTree;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public void setTreePropContainer(@Nullable TreePropContainer treePropContainer) {
    mTreePropContainer = treePropContainer;
  }

  public void setParentTreePropContainer(@Nullable TreePropContainer treePropContainer) {
    mParentTreePropContainer = treePropContainer;
  }

  public @Nullable TreePropContainer getTreePropContainer() {
    return mTreePropContainer;
  }

  public @Nullable TreePropContainer getParentTreePropContainer() {
    return mParentTreePropContainer;
  }

  /**
   * @return true if parent's TreeProps are cloned and assigned to mTreeProps. Notice this method
   *     should be accessed by Kotlin API only.
   */
  protected boolean isParentTreePropContainerCloned() {
    return mIsParentTreePropsCloned;
  }

  protected void setParentTreePropContainerCloned(boolean isParentTreePropsCloned) {
    mIsParentTreePropsCloned = isParentTreePropsCloned;
  }

  @Nullable
  public <T> T getTreeProp(Class<T> key) {
    return mTreePropContainer == null ? null : mTreePropContainer.get(key);
  }

  <T> T getTreeProp(TreeProp<T> key) {
    return mTreePropContainer == null ? key.getDefaultValue() : mTreePropContainer.get(key);
  }

  @Nullable
  public <T> T getParentTreeProp(Class<T> key) {
    return mParentTreePropContainer == null ? null : mParentTreePropContainer.get(key);
  }

  /** Obtain a copy of the tree props currently held by this context. */
  @Nullable
  public TreePropContainer getTreePropContainerCopy() {
    return TreePropContainer.copy(mTreePropContainer);
  }

  public int getLayoutVersion() {
    final CalculationContext calculationContext = mCalculationStateContextThreadLocal.get();
    if (calculationContext != null) {
      return calculationContext.getLayoutVersion();
    }

    throw new IllegalStateException(
        "LayoutVersion is only available during layout calculation."
            + "Please only invoke getLayoutVersion from OnCreateLayout/OnMeasure/OnPrepare");
  }

  @Nullable
  public ResourceCache getResourceCache() {
    return mResourceResolver.getResourceCache();
  }

  @Nullable
  public Object getCachedValue(String globalKey, int index, Object cachedValueInputs) {
    if (mLithoTree == null) {
      return null;
    }
    return mLithoTree
        .getStateUpdater()
        .getCachedValue(globalKey, index, cachedValueInputs, isNestedTreeContext());
  }

  public void putCachedValue(
      String globalKey, int index, Object cachedValueInputs, @Nullable Object cachedValue) {
    if (mLithoTree == null) {
      return;
    }
    mLithoTree
        .getStateUpdater()
        .putCachedValue(globalKey, index, cachedValueInputs, cachedValue, isNestedTreeContext());
  }

  @Nullable
  StateUpdater getStateUpdater() {
    return mLithoTree != null ? mLithoTree.getStateUpdater() : null;
  }

  @Nullable
  StateProvider getStateProvider() {
    return mLithoTree != null ? mLithoTree.getStateProvider() : null;
  }

  /**
   * @return New instance of {@link EventTrigger} that is created by the current mComponentScope.
   */
  <E> EventTrigger<E> newEventTrigger(int id, String childKey, @Nullable Handle handle) {
    String parentKey = mComponentScope == null ? "" : getGlobalKey();
    return new EventTrigger<>(parentKey, id, childKey, handle);
  }

  void applyStyle(LithoNode node, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    if (defStyleAttr != 0 || defStyleRes != 0) {
      setDefStyle(defStyleAttr, defStyleRes);
      node.applyAttributes(mContext, defStyleAttr, defStyleRes);
      setDefStyle(0, 0);
    }
  }

  /**
   * Checks if incremental mount is enabled given a ComponentContext, so you can throw an error if
   * you require that incremental mount is enabled (e.g. you use visibility callbacks). This is
   * static to avoid polluting the ComponentContext API.
   */
  public static boolean isIncrementalMountEnabled(ComponentContext c) {
    return c.mLithoConfiguration.componentsConfig.incrementalMountEnabled;
  }

  public static boolean isVisibilityProcessingEnabled(ComponentContext c) {
    return c.mLithoConfiguration.componentsConfig.visibilityProcessingEnabled;
  }

  @Nullable
  public LithoLayoutContext getLayoutStateContext() {
    final CalculationContext stateContext = mCalculationStateContextThreadLocal.get();
    if (stateContext instanceof LithoLayoutContext) {
      return (LithoLayoutContext) stateContext;
    }

    return null;
  }

  @Nullable
  public ResolveContext getRenderStateContext() {
    final CalculationContext stateContext = mCalculationStateContextThreadLocal.get();
    if (stateContext instanceof ResolveContext) {
      return (ResolveContext) stateContext;
    }

    return null;
  }

  public ScopedComponentInfo getScopedComponentInfo() {
    return Preconditions.checkNotNull(mScopedComponentInfo);
  }

  public final boolean shouldUseNonRebindingEventHandlers() {
    return mLithoConfiguration.componentsConfig.getUseNonRebindingEventHandlers();
  }

  boolean isNestedTreeContext() {
    return isNestedTreeContext;
  }

  /**
   * This method determine if transitions are enabled for the user. If the experiment is enabled for
   * the user then they will get cached value else it will be determined using the utility method.
   *
   * @return true if transitions are enabled.
   */
  boolean areTransitionsEnabled() {
    return mLithoConfiguration.areTransitionsEnabled;
  }

  @Nullable
  RenderUnitIdGenerator getRenderUnitIdGenerator() {
    return mLithoConfiguration.renderUnitIdGenerator;
  }

  @Nullable
  LithoVisibilityEventsController getLithoVisibilityEventsController() {
    return mLithoVisibilityEventsController;
  }

  @VisibleForTesting
  @Nullable
  View getMountedView() {
    if (mLithoTree == null) {
      return null;
    }
    return mLithoTree.getMountedViewReference().getMountedView();
  }

  void removePendingStateUpdate(String key, boolean nestedTreeContext) {
    if (mLithoTree != null) {
      mLithoTree.getStateUpdater().removePendingStateUpdate(key, nestedTreeContext);
    }
  }

  static ComponentsConfiguration getComponentsConfig(ComponentContext c) {
    return c.mLithoConfiguration.componentsConfig;
  }

  <E> EventHandler<E> createEventHandler(
      final int id,
      final EventHandlerRebindMode mode,
      final @Nullable Object[] params,
      final Class<? extends Component> reference,
      final String source) {

    final @Nullable Component component = mComponentScope;
    final @Nullable ScopedComponentInfo info = mScopedComponentInfo;

    // EventHandler no-ops if ComponentContext is not associated with a Component.
    if (!(component instanceof HasEventDispatcher)) {
      return NoOpEventHandler.getNoOpEventHandler();
    }

    // Log a debug event if the event handler is created using the correct context
    if (reference != component.getClass()) {
      DebugInfoReporter.report(
          "WrongContextForEventHandler",
          attributes -> {
            attributes.put(Source, source);
            attributes.put("reference", reference);
            return Unit.INSTANCE;
          });
    }

    final @Nullable CalculationContext context = getCalculationStateContext();
    final EventDispatchInfo dispatchInfo;
    if (canUseStateForEventDispatchInfo(getComponentsConfig(this), mode, context, info)) {
      ComponentState<?> state = Preconditions.checkNotNull(getScopedComponentInfo().getState());
      dispatchInfo = Preconditions.checkNotNull(state.getEventDispatchInfo());
    } else {
      dispatchInfo = new EventDispatchInfo((HasEventDispatcher) component, this);
    }

    final EventHandler<E> eventHandler = new EventHandler<>(id, mode, dispatchInfo, params);

    if (context == null) {
      eventHandler.dispatchInfo.isBound = true;
      if (ComponentsConfiguration.isEventHandlerRebindLoggingEnabled) {
        DebugInfoReporter.report(
            "EventHandlerCreatedAfterLayout",
            attributes -> {
              attributes.put(Source, component);
              return Unit.INSTANCE;
            });
      }
    } else if (shouldRecordEventHandler(this, mode)) {
      context.recordEventHandler(getGlobalKey(), eventHandler);
    }

    return eventHandler;
  }

  private static boolean canUseStateForEventDispatchInfo(
      final ComponentsConfiguration config,
      final EventHandlerRebindMode mode,
      final @Nullable CalculationContext calculationContext,
      final @Nullable ScopedComponentInfo info) {

    if (mode != EventHandlerRebindMode.REBIND) {
      return false;
    }

    if (!config.useStateForEventDispatchInfo) {
      return false;
    }

    if (calculationContext == null) {
      return false;
    }

    if (info == null || info.getState() == null || info.getState().getEventDispatchInfo() == null) {
      return false;
    }

    return true;
  }

  private static boolean shouldRecordEventHandler(ComponentContext c, EventHandlerRebindMode mode) {
    if (c.getLithoConfiguration().componentsConfig.useStateForEventDispatchInfo) {
      return false;
    }

    if (c.shouldUseNonRebindingEventHandlers() && mode != EventHandlerRebindMode.REBIND) {
      return false;
    }

    return true;
  }
}
