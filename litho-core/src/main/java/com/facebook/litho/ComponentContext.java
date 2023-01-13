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

import static com.facebook.litho.StateContainer.StateUpdate;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Looper;
import android.view.View;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.ComponentTree.LithoConfiguration;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.RunnableHandler;

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
public class ComponentContext implements Cloneable {

  static final String NO_SCOPE_EVENT_HANDLER = "ComponentContext:NoScopeEventHandler";
  private final Context mContext;
  private LithoConfiguration mLithoConfiguration;

  private @Nullable String mNoStateUpdatesMethod;

  // Hold a reference to the component which scope we are currently within.
  @ThreadConfined(ThreadConfined.ANY)
  private Component mComponentScope;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable String mGlobalKey;

  @ThreadConfined(ThreadConfined.ANY)
  private final ResourceResolver mResourceResolver;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable TreeProps mTreeProps;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable TreeProps mParentTreeProps;

  @ThreadConfined(ThreadConfined.ANY)
  private boolean mIsParentTreePropsCloned;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable ComponentTree mComponentTree;

  // Used to hold styling information applied to components
  @StyleRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleRes = 0;

  @AttrRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleAttr = 0;

  private @Nullable ScopedComponentInfo mScopedComponentInfo;

  private boolean isNestedTreeContext;

  private final ThreadLocal<CalculationStateContext> mCalculationStateContextThreadLocal;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable StateUpdater mStateUpdater;

  public ComponentContext(Context context) {
    this(context, null, null);
  }

  /**
   * Constructor that can be used to receive log data from components. Check {@link
   * ComponentsLogger} for the type of events you can listen for.
   *
   * @param context Android context.
   * @param logTag a log tag to be used with the logger.
   * @param logger a lifecycle logger to be used.
   */
  public ComponentContext(
      Context context, @Nullable String logTag, @Nullable ComponentsLogger logger) {
    this(context, logTag, logger, null);
  }

  public ComponentContext(
      Context context,
      @Nullable String logTag,
      @Nullable ComponentsLogger logger,
      @Nullable TreeProps treeProps) {
    this(context, treeProps, buildDefaultLithoConfiguration(context, logTag, logger), null);
  }

  private static LithoConfiguration buildDefaultLithoConfiguration(
      Context context, @Nullable String logTag, @Nullable ComponentsLogger logger) {
    return new LithoConfiguration(
        AnimationsDebug.areTransitionsEnabled(context),
        false,
        ComponentsConfiguration.reuseLastMeasuredNodeInComponentMeasure,
        false,
        ComponentsConfiguration.overrideReconciliation != null
            ? ComponentsConfiguration.overrideReconciliation
            : true,
        true,
        null,
        !ComponentsConfiguration.isIncrementalMountGloballyDisabled,
        DefaultErrorEventHandler.INSTANCE,
        logTag,
        logger,
        null); // TODO check if we can make this not nullable and always instantiate one
  }

  public ComponentContext(
      Context context,
      @Nullable TreeProps treeProps,
      LithoConfiguration lithoConfiguration,
      @Nullable StateUpdater stateUpdater) {
    mCalculationStateContextThreadLocal = new ThreadLocal<>();
    if (lithoConfiguration.logger != null && lithoConfiguration.logTag == null) {
      throw new IllegalStateException("When a ComponentsLogger is set, a LogTag must be set");
    }

    mContext = context;
    mResourceResolver =
        new ResourceResolver(
            context, ResourceCache.getLatest(context.getResources().getConfiguration()));
    mTreeProps = treeProps;
    mLithoConfiguration = lithoConfiguration;
    mStateUpdater = stateUpdater;
  }

  public ComponentContext(ComponentContext context) {
    this(context, context.mTreeProps);
  }

  public ComponentContext(ComponentContext context, @Nullable TreeProps treeProps) {
    mContext = context.mContext;
    mResourceResolver = context.mResourceResolver;
    mComponentScope = context.mComponentScope;
    mComponentTree = context.mComponentTree;
    mStateUpdater = context.mStateUpdater;
    mTreeProps = treeProps != null ? treeProps : context.mTreeProps;
    mParentTreeProps = context.mParentTreeProps;
    mGlobalKey = context.mGlobalKey;
    mCalculationStateContextThreadLocal = context.mCalculationStateContextThreadLocal;
    mLithoConfiguration = context.mLithoConfiguration;
  }

  private static LithoConfiguration mergeConfigurationWithNewLogTagAndLogger(
      LithoConfiguration lithoConfiguration,
      @Nullable String logTag,
      @Nullable ComponentsLogger logger) {
    return new LithoConfiguration(
        lithoConfiguration.areTransitionsEnabled,
        lithoConfiguration.shouldKeepLithoNodeAndLayoutResultTreeWithReconciliation,
        lithoConfiguration.isReuseLastMeasuredNodeInComponentMeasureEnabled,
        lithoConfiguration.shouldReuseOutputs,
        lithoConfiguration.isReconciliationEnabled,
        lithoConfiguration.isVisibilityProcessingEnabled,
        lithoConfiguration.mountContentPreallocationHandler,
        lithoConfiguration.incrementalMountEnabled,
        lithoConfiguration.errorEventHandler,
        logTag != null ? logTag : lithoConfiguration.logTag,
        logger != null ? logger : lithoConfiguration.logger,
        lithoConfiguration.renderUnitIdGenerator);
  }

  ComponentContext makeNewCopy() {
    return new ComponentContext(this);
  }

  /**
   * Creates a new ComponentContext instance and sets the {@link ComponentTree} on the component.
   *
   * @param context context scoped to the parent component
   * @param componentTree component tree associated with the newly created context
   * @return a new ComponentContext instance
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static ComponentContext withComponentTree(
      ComponentContext context, ComponentTree componentTree) {
    final String contextLogTag = context.mLithoConfiguration.logTag;
    final ComponentsLogger contextLogger = context.mLithoConfiguration.logger;

    final LithoConfiguration lithoConfiguration =
        (contextLogTag != null || contextLogger != null)
            ? mergeConfigurationWithNewLogTagAndLogger(
                componentTree.getLithoConfiguration(), contextLogTag, contextLogger)
            : componentTree.getLithoConfiguration();

    ComponentContext componentContext =
        new ComponentContext(
            context.getAndroidContext(), context.mTreeProps, lithoConfiguration, componentTree);
    componentContext.mParentTreeProps = context.mParentTreeProps;
    componentContext.mGlobalKey = context.mGlobalKey;
    componentContext.mComponentTree = componentTree;
    componentContext.mStateUpdater = componentTree;
    componentContext.mComponentScope = null;

    return componentContext;
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
    componentContext.mParentTreeProps = parentContext.mTreeProps;
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

  /** @deprecated introduced for legacy test cases - don't add new callers */
  @VisibleForTesting
  @Deprecated
  public static ComponentContext createScopedComponentContextWithStateForTest(
      ComponentContext parent, Component component, String key) {
    final ComponentContext context = ComponentContext.withComponentScope(parent, component, key);
    // SpecGeneratedComponents expect a StateContainer to be set on all scoped ComponentContexts
    // before any lifecycle methods are invoked.
    if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).hasState()) {
      context
          .getScopedComponentInfo()
          .setStateContainer(
              ((SpecGeneratedComponent) component).createInitialStateContainer(context));
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
        parentTreeContext.getTreePropsCopy(),
        parentTreeContext.mLithoConfiguration,
        parentTreeContext.mStateUpdater);
  }

  /** Returns the current calculate state context */
  @Nullable
  CalculationStateContext getCalculationStateContext() {
    return mCalculationStateContextThreadLocal.get();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setLayoutStateContext(LayoutStateContext layoutStateContext) {
    mCalculationStateContextThreadLocal.set(layoutStateContext);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void clearCalculationStateContext() {
    mCalculationStateContextThreadLocal.set(null);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void setLayoutStateContextForTesting() {
    setLayoutStateContext(LayoutStateContext.getTestInstance(this));
  }

  /**
   * For test usage. This method will ensure this ComponentContext has a ComponentTree, and then
   * generate a RenderStateContext, set it on the ComponentTree, and return the same RSC. This is
   * critical for tests that trigger layout calculation functionality outside of a LayoutState
   * calculation (i.e., including willRender, Layout API, caching, etc).
   */
  @VisibleForTesting
  public ResolveStateContext setRenderStateContextForTests() {
    if (mComponentTree == null) {
      mComponentTree = ComponentTree.create(this).build();
      mStateUpdater = mComponentTree;
    }

    final ResolveStateContext resolveStateContext =
        new ResolveStateContext(new MeasuredResultCache(), new TreeState(), 0, null, null, null);
    setRenderStateContext(resolveStateContext);

    return resolveStateContext;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setRenderStateContext(ResolveStateContext renderStateContext) {
    mCalculationStateContextThreadLocal.set(renderStateContext);
  }

  /**
   * Sets the calculation state context (either a RenderStateContext or LayoutStateContext) on a
   * thread local, making it unique per ComponentTree, per Thread.
   */
  void setCalculationStateContext(@Nullable CalculationStateContext context) {
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

  public final Looper getMainLooper() {
    return mContext.getMainLooper();
  }

  public CharSequence getText(@StringRes int resId) {
    return mContext.getResources().getText(resId);
  }

  public String getString(@StringRes int resId) {
    return mContext.getResources().getString(resId);
  }

  public String getString(@StringRes int resId, Object... formatArgs) {
    return mContext.getResources().getString(resId, formatArgs);
  }

  public int getColor(@ColorRes int id) {
    return mContext.getResources().getColor(id);
  }

  public Component getComponentScope() {
    return mComponentScope;
  }

  public String getGlobalKey() {
    if (mComponentScope == null) {
      throw new RuntimeException(
          "getGlobalKey cannot be accessed from a ComponentContext without a scope");
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
        return mLithoConfiguration.errorEventHandler;
      }
    }

    return mLithoConfiguration.errorEventHandler;
  }

  @Nullable
  @VisibleForTesting
  public TreeFuture getLayoutStateFuture() {
    return mCalculationStateContextThreadLocal.get() != null
        ? mCalculationStateContextThreadLocal.get().getLayoutStateFuture()
        : null;
  }

  /**
   * Notify the Component Tree that it needs to synchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  public void updateStateSync(StateUpdate stateUpdate, String attribution) {
    checkIfNoStateUpdatesMethod();

    if (mStateUpdater == null) {
      return;
    }

    mStateUpdater.updateStateSync(
        getGlobalKey(),
        stateUpdate,
        attribution,
        isCreateLayoutInProgress(),
        isNestedTreeContext());
  }

  /**
   * Notify the Component Tree that it needs to asynchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  public void updateStateAsync(StateUpdate stateUpdate, String attribution) {
    checkIfNoStateUpdatesMethod();

    if (mStateUpdater == null) {
      return;
    }

    mStateUpdater.updateStateAsync(
        getGlobalKey(),
        stateUpdate,
        attribution,
        isCreateLayoutInProgress(),
        isNestedTreeContext());
  }

  public void updateStateWithTransition(StateUpdate stateUpdate, String attribution) {
    updateStateAsync(stateUpdate, attribution);
  }

  public void updateStateLazy(StateUpdate stateUpdate) {
    if (mStateUpdater == null) {
      return;
    }

    mStateUpdater.updateStateLazy(getGlobalKey(), stateUpdate, isNestedTreeContext());
  }

  final void updateHookStateAsync(String globalKey, HookUpdater updateBlock) {
    checkIfNoStateUpdatesMethod();

    if (mStateUpdater == null) {
      return;
    }

    final Component scope = getComponentScope();
    mStateUpdater.updateHookStateAsync(
        globalKey,
        updateBlock,
        scope != null ? "<cls>" + scope.getClass().getName() + "</cls>" : "hook",
        isCreateLayoutInProgress(),
        isNestedTreeContext());
  }

  final void updateHookStateSync(String globalKey, HookUpdater updateBlock) {
    checkIfNoStateUpdatesMethod();

    if (mStateUpdater == null) {
      return;
    }

    final Component scope = getComponentScope();
    mStateUpdater.updateHookStateSync(
        globalKey,
        updateBlock,
        scope != null ? scope.getSimpleName() : "hook",
        isCreateLayoutInProgress(),
        isNestedTreeContext());
  }

  /**
   * @return A StateContainer with lazy state updates applied. This may be the same container passed
   *     in if there were no updates to apply. This method won't mutate the passed container.
   */
  public StateContainer applyLazyStateUpdatesForContainer(StateContainer container) {
    if (mStateUpdater == null) {
      return container;
    }

    return mStateUpdater.applyLazyStateUpdatesForContainer(
        getGlobalKey(), container, isNestedTreeContext());
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
    return mLithoConfiguration.logTag;
  }

  @Nullable
  public ComponentsLogger getLogger() {
    return mLithoConfiguration.logger;
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

    if (mComponentTree == null) {
      throw new RuntimeException(
          "Calling findViewWithTag on a ComponentContext which isn't associated with a ComponentTree. Make sure it's one received in `render` or `onCreateLayout`");
    }
    final LithoView lithoView = mComponentTree.getLithoView();
    // LithoView isn't mounted
    if (lithoView == null) {
      return null;
    }

    return lithoView.findViewWithTag(tag);
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

  ComponentTree getComponentTree() {
    return mComponentTree;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public void setTreeProps(@Nullable TreeProps treeProps) {
    mTreeProps = treeProps;
  }

  public void setParentTreeProps(@Nullable TreeProps treeProps) {
    mParentTreeProps = treeProps;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public @Nullable TreeProps getTreeProps() {
    return mTreeProps;
  }

  public @Nullable TreeProps getParentTreeProps() {
    return mParentTreeProps;
  }

  /**
   * @return true if parent's TreeProps are cloned and assigned to mTreeProps. Notice this method
   *     should be accessed by Kotlin API only.
   */
  protected boolean isParentTreePropsCloned() {
    return mIsParentTreePropsCloned;
  }

  protected void setParentTreePropsCloned(boolean isParentTreePropsCloned) {
    mIsParentTreePropsCloned = isParentTreePropsCloned;
  }

  @Nullable
  public <T> T getTreeProp(Class<T> key) {
    return mTreeProps == null ? null : mTreeProps.get(key);
  }

  @Nullable
  public <T> T getParentTreeProp(Class<T> key) {
    return mParentTreeProps == null ? null : mParentTreeProps.get(key);
  }

  /** Obtain a copy of the tree props currently held by this context. */
  @Nullable
  public TreeProps getTreePropsCopy() {
    return TreeProps.copy(mTreeProps);
  }

  public int getLayoutVersion() {
    final CalculationStateContext calculationStateContext =
        mCalculationStateContextThreadLocal.get();
    if (calculationStateContext != null) {
      return calculationStateContext.getLayoutVersion();
    }

    throw new IllegalStateException(
        "LayoutVersion is only available during layout calculation."
            + "Please only invoke getLayoutVersion from OnCreateLayout/OnMeasure/OnPrepare");
  }

  public ResourceCache getResourceCache() {
    return mResourceResolver.getResourceCache();
  }

  EventHandler newEventHandler(int id) {
    return newEventHandler(id, null);
  }

  public <E> EventHandler<E> newEventHandler(int id, @Nullable Object[] params) {
    if (mComponentScope == null) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.FATAL,
          NO_SCOPE_EVENT_HANDLER,
          "Creating event handler without scope.");
      return NoOpEventHandler.getNoOpEventHandler();
    }

    return new EventHandler<>(id, new EventDispatchInfo(mComponentScope, this), params);
  }

  @Nullable
  public Object getCachedValue(Object cachedValueInputs) {
    if (mStateUpdater == null) {
      return null;
    }
    return mStateUpdater.getCachedValue(cachedValueInputs, isNestedTreeContext());
  }

  public void putCachedValue(Object cachedValueInputs, Object cachedValue) {
    if (mStateUpdater == null) {
      return;
    }
    mStateUpdater.putCachedValue(cachedValueInputs, cachedValue, isNestedTreeContext());
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
    return c.mLithoConfiguration.incrementalMountEnabled;
  }

  public static @Nullable RunnableHandler getMountContentPreallocationHandler(ComponentContext c) {
    return c.mLithoConfiguration.mountContentPreallocationHandler;
  }

  public static boolean isVisibilityProcessingEnabled(ComponentContext c) {
    return c.mLithoConfiguration.isVisibilityProcessingEnabled;
  }

  public boolean isReconciliationEnabled() {
    return mLithoConfiguration.isReconciliationEnabled;
  }

  @Nullable
  public LayoutStateContext getLayoutStateContext() {
    final CalculationStateContext stateContext = mCalculationStateContextThreadLocal.get();
    if (stateContext instanceof LayoutStateContext) {
      return (LayoutStateContext) stateContext;
    }

    return null;
  }

  @Nullable
  public ResolveStateContext getRenderStateContext() {
    final CalculationStateContext stateContext = mCalculationStateContextThreadLocal.get();
    if (stateContext instanceof ResolveStateContext) {
      return (ResolveStateContext) stateContext;
    }

    return null;
  }

  public ScopedComponentInfo getScopedComponentInfo() {
    return Preconditions.checkNotNull(mScopedComponentInfo);
  }

  @Override
  protected ComponentContext clone() {
    try {
      return (ComponentContext) super.clone();
    } catch (CloneNotSupportedException e) {
      // Should not be possible!
      throw new RuntimeException(e);
    }
  }

  boolean shouldReuseOutputs() {
    return mLithoConfiguration.shouldReuseOutputs;
  }

  boolean isReuseLastMeasuredNodeInComponentMeasureEnabled() {
    return mLithoConfiguration.isReuseLastMeasuredNodeInComponentMeasureEnabled;
  }

  boolean isNestedTreeContext() {
    return isNestedTreeContext;
  }

  boolean shouldKeepLithoNodeAndLayoutResultTreeWithReconciliation() {
    return mLithoConfiguration.shouldKeepLithoNodeAndLayoutResultTreeWithReconciliation;
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
}
