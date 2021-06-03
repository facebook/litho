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

import static com.facebook.litho.StateContainer.StateUpdate;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Looper;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
public class ComponentContext implements Cloneable {

  public static final NoOpInternalNode NULL_LAYOUT = new NoOpInternalNode();

  private final boolean mWasStatelessWhenCreated;
  private final @Nullable Boolean mCreatedFromStatelessStatelessContext;

  static final String NO_SCOPE_EVENT_HANDLER = "ComponentContext:NoScopeEventHandler";
  private final Context mContext;
  // TODO: T48229786 move to CT
  private final @Nullable String mLogTag;
  private final @Nullable ComponentsLogger mLogger;
  private @Nullable StateHandler mStateHandler;

  private @Nullable String mNoStateUpdatesMethod;

  // Hold a reference to the component which scope we are currently within.
  @ThreadConfined(ThreadConfined.ANY)
  private Component mComponentScope;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable String mGlobalKey;

  @ThreadConfined(ThreadConfined.ANY)
  private final ResourceCache mResourceCache;

  @ThreadConfined(ThreadConfined.ANY)
  private final ResourceResolver mResourceResolver;

  @ThreadConfined(ThreadConfined.ANY)
  private int mWidthSpec;

  @ThreadConfined(ThreadConfined.ANY)
  private int mHeightSpec;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable TreeProps mTreeProps;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable TreeProps mParentTreeProps;

  @ThreadConfined(ThreadConfined.ANY)
  private boolean mIsParentTreePropsCloned;

  @ThreadConfined(ThreadConfined.ANY)
  private ComponentTree mComponentTree;

  // Used to hold styling information applied to components
  @StyleRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleRes = 0;

  @AttrRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleAttr = 0;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable LayoutStateContext mLayoutStateContext;

  public ComponentContext(Context context) {
    this(context, null, null, null);
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
    if (logger != null && logTag == null) {
      throw new IllegalStateException("When a ComponentsLogger is set, a LogTag must be set");
    }

    mWasStatelessWhenCreated = ComponentsConfiguration.useStatelessComponent;
    mCreatedFromStatelessStatelessContext = null;

    mContext = context;
    mResourceCache = ResourceCache.getLatest(context.getResources().getConfiguration());
    mResourceResolver = new ResourceResolver(this);
    mTreeProps = treeProps;
    mLogger = logger;
    mLogTag = logTag;
    mStateHandler = null;
  }

  public ComponentContext(ComponentContext context) {
    this(context, context.mStateHandler, context.mTreeProps, context.mLayoutStateContext);
  }

  public ComponentContext(
      ComponentContext context,
      @Nullable StateHandler stateHandler,
      @Nullable TreeProps treeProps,
      @Nullable LayoutStateContext layoutStateContext) {

    mContext = context.mContext;
    mResourceCache = context.mResourceCache;
    mResourceResolver = context.mResourceResolver;
    mWidthSpec = context.mWidthSpec;
    mHeightSpec = context.mHeightSpec;
    mComponentScope = context.mComponentScope;
    mComponentTree = context.mComponentTree;
    mLayoutStateContext = layoutStateContext;
    mLogger = context.mLogger;
    mLogTag =
        context.mLogTag != null || mComponentTree == null
            ? context.mLogTag
            : mComponentTree.getSimpleName();

    mStateHandler = stateHandler != null ? stateHandler : context.mStateHandler;
    mTreeProps = treeProps != null ? treeProps : context.mTreeProps;
    mGlobalKey = context.mGlobalKey;
    mWasStatelessWhenCreated = ComponentsConfiguration.useStatelessComponent;
    mCreatedFromStatelessStatelessContext = context.mWasStatelessWhenCreated;
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
    ComponentContext componentContext =
        new ComponentContext(context, new StateHandler(), null, null);
    componentContext.mComponentTree = componentTree;
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
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static ComponentContext withComponentScope(
      final ComponentContext parentContext, final Component scope, final String globalKey) {
    ComponentContext componentContext = parentContext.makeNewCopy();
    componentContext.mGlobalKey = null;
    componentContext.mComponentScope = scope;
    componentContext.mComponentTree = parentContext.mComponentTree;

    if (ComponentsConfiguration.useGlobalKeys && globalKey == null) {
      throw new IllegalStateException(
          "GlobalKey should not be null for component "
              + scope.getSimpleName()
              + ", parent global key: "
              + ((parentContext.getComponentScope() == null)
                  ? "NULL_COMPONENT_SCOPE"
                  : parentContext.getGlobalKey())
              + " useStateless: "
              + ComponentsConfiguration.useGlobalKeys);
    }

    componentContext.mGlobalKey = globalKey;

    if (ComponentsConfiguration.useStatelessComponent) {
      componentContext
          .getLayoutStateContext()
          .addScopedComponentInfo(globalKey, scope, componentContext, parentContext);
    }

    return componentContext;
  }

  /**
   * Creates a new ComponentContext based on the given ComponentContext, propagating log tag,
   * logger, and tree props. This should be used when creating a ComponentContext for a nested
   * ComponentTree (e.g. see HorizontalScrollSpec).
   */
  public static ComponentContext makeCopyForNestedTree(ComponentContext parentTreeContext) {
    return new ComponentContext(
        parentTreeContext.getAndroidContext(),
        parentTreeContext.getLogTag(),
        parentTreeContext.getLogger(),
        parentTreeContext.getTreePropsCopy());
  }

  void setLayoutStateContext(LayoutStateContext layoutStateContext) {
    if (mLayoutStateContext != null) {
      throw new IllegalStateException(
          "LayoutStateContext must not be overridden. " + getDebugString());
    } else {
      mLayoutStateContext = layoutStateContext;
    }
  }

  @Deprecated
  public void setLayoutStateContextSafely(LayoutStateContext layoutStateContext) {
    mLayoutStateContext = layoutStateContext;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void setLayoutStateContextForTesting() {
    setLayoutStateContextSafely(LayoutStateContext.getTestInstance(this));
  }

  /**
   * Returns true if this method is called during a layout state calculation and the LayoutState
   * reference hasn't been nullified.
   */
  boolean hasLayoutState() {
    return mLayoutStateContext != null && mLayoutStateContext.getLayoutState() != null;
  }

  /** Returns true if this method is called during layout creation. */
  boolean isCreateLayoutInProgress() {
    if (mLayoutStateContext == null || mLayoutStateContext.getLayoutState() == null) {
      return false;
    }
    return mLayoutStateContext.getLayoutState().isCreateLayoutInProgress();
  }

  @Nullable
  LayoutState getLayoutState() {
    return mLayoutStateContext == null ? null : mLayoutStateContext.getLayoutState();
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

    if (ComponentsConfiguration.useStatelessComponent) {
      return mGlobalKey;
    }

    return Component.getGlobalKey(this, mComponentScope);
  }

  public EventHandler<ErrorEvent> getErrorEventHandler() {
    if (mComponentScope != null) {
      if (ComponentsConfiguration.useStatelessComponent) {
        try {
          final EventHandler<ErrorEvent> errorEventHandler =
              getLayoutStateContext().getScopedComponentInfo(getGlobalKey()).getErrorEventHandler();
          if (errorEventHandler != null) {
            return errorEventHandler;
          }
        } catch (IllegalStateException e) {
          if (mComponentTree != null) {
            return mComponentTree.getErrorEventHandler();
          }
          return DefaultErrorEventHandler.INSTANCE;
        }
      } else if (mComponentScope.getErrorHandler() != null) {
        return mComponentScope.getErrorHandler();
      }
    }

    if (mComponentTree != null) {
      return mComponentTree.getErrorEventHandler();
    }
    return DefaultErrorEventHandler.INSTANCE;
  }

  @Nullable
  @VisibleForTesting
  public ComponentTree.LayoutStateFuture getLayoutStateFuture() {
    return mLayoutStateContext == null ? null : mLayoutStateContext.getLayoutStateFuture();
  }

  /**
   * Notify the Component Tree that it needs to synchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  public void updateStateSync(StateUpdate stateUpdate, String attribution) {
    checkIfNoStateUpdatesMethod();

    if (mComponentTree == null) {
      return;
    }

    mComponentTree.updateStateSync(
        getGlobalKey(), stateUpdate, attribution, isCreateLayoutInProgress());
  }

  /**
   * Notify the Component Tree that it needs to asynchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  public void updateStateAsync(StateUpdate stateUpdate, String attribution) {
    checkIfNoStateUpdatesMethod();

    if (mComponentTree == null) {
      return;
    }

    mComponentTree.updateStateAsync(
        getGlobalKey(), stateUpdate, attribution, isCreateLayoutInProgress());
  }

  public void updateStateWithTransition(StateUpdate stateUpdate, String attribution) {
    updateStateAsync(stateUpdate, attribution);
  }

  public void updateStateLazy(StateUpdate stateUpdate) {
    if (mComponentTree == null) {
      return;
    }

    mComponentTree.updateStateLazy(getGlobalKey(), stateUpdate);
  }

  final void updateHookStateAsync(String globalKey, HookUpdater updateBlock) {
    checkIfNoStateUpdatesMethod();

    if (mComponentTree == null) {
      return;
    }

    final Component scope = getComponentScope();
    mComponentTree.updateHookStateAsync(
        globalKey,
        updateBlock,
        scope != null ? scope.getSimpleName() : "hook",
        isCreateLayoutInProgress());
  }

  final void updateHookStateSync(String globalKey, HookUpdater updateBlock) {
    checkIfNoStateUpdatesMethod();

    if (mComponentTree == null) {
      return;
    }

    final Component scope = getComponentScope();
    mComponentTree.updateHookStateSync(
        globalKey,
        updateBlock,
        scope != null ? scope.getSimpleName() : "hook",
        isCreateLayoutInProgress());
  }

  public void applyLazyStateUpdatesForContainer(StateContainer container) {
    if (mComponentTree == null) {
      return;
    }

    mComponentTree.applyLazyStateUpdatesForContainer(getGlobalKey(), container);
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
    // TODO: T48229786 use CT field only
    return mComponentTree == null || mComponentTree.getLogTag() == null
        ? mLogTag
        : mComponentTree.getLogTag();
  }

  @Nullable
  public ComponentsLogger getLogger() {
    // TODO: T48229786 use CT field only
    return mComponentTree == null || mComponentTree.getLogger() == null
        ? mLogger
        : mComponentTree.getLogger();
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
    if (mLayoutStateContext == null || mLayoutStateContext.getLayoutState() == null) {
      throw new IllegalStateException(
          "LayoutVersion is only available during layout calculation."
              + "Please only invoke getLayoutVersion from OnCreateLayout/OnMeasure/OnPrepare");
    }

    return mLayoutStateContext.getLayoutState().mLayoutVersion;
  }

  public ResourceCache getResourceCache() {
    return mResourceCache;
  }

  EventHandler newEventHandler(int id) {
    if (mComponentScope == null) {
      warnNullScope();
      return NoOpEventHandler.getNoOpEventHandler();
    }
    return new EventHandler(mComponentScope, id);
  }

  public <E> EventHandler<E> newEventHandler(int id, Object[] params) {
    if (mComponentScope == null) {
      warnNullScope();
      return NoOpEventHandler.getNoOpEventHandler();
    }

    if (ComponentsConfiguration.useStatelessComponent) {
      if (getLayoutStateContext() != null) {
        try {
          getLayoutStateContext().getScopedComponentInfo(mGlobalKey);
        } catch (IllegalStateException e) {
          String thisGlobalKey = mGlobalKey != null ? mGlobalKey : "t-null";
          String componentGlobalKey =
              mComponentScope.getGlobalKeyForLogging() != null
                  ? mComponentScope.getGlobalKeyForLogging()
                  : "c-null";

          throw new IllegalStateException(
              "No scoped info found. "
                  + getDebugString()
                  + "\nglobal-key-mismatch: "
                  + thisGlobalKey.equals(componentGlobalKey)
                  + "\ncomponent-global-key: "
                  + componentGlobalKey);
        }
      }
    }

    return new EventHandler<>(mComponentScope, id, params);
  }

  private static void warnNullScope() {
    ComponentsReporter.emitMessage(
        ComponentsReporter.LogLevel.FATAL,
        NO_SCOPE_EVENT_HANDLER,
        "Creating event handler without scope.");
  }

  @Nullable
  public Object getCachedValue(Object cachedValueInputs) {
    if (mComponentTree == null) {
      return null;
    }
    return mComponentTree.getCachedValue(cachedValueInputs);
  }

  public void putCachedValue(Object cachedValueInputs, Object cachedValue) {
    if (mComponentTree == null) {
      return;
    }
    mComponentTree.putCachedValue(cachedValueInputs, cachedValue);
  }

  /**
   * @return New instance of {@link EventTrigger} that is created by the current mComponentScope.
   */
  <E> EventTrigger<E> newEventTrigger(int id, String childKey, @Nullable Handle handle) {
    String parentKey = mComponentScope == null ? "" : getGlobalKey();
    return new EventTrigger<>(parentKey, id, childKey, handle);
  }

  int getWidthSpec() {
    return mWidthSpec;
  }

  void setWidthSpec(int widthSpec) {
    mWidthSpec = widthSpec;
  }

  int getHeightSpec() {
    return mHeightSpec;
  }

  void setHeightSpec(int heightSpec) {
    mHeightSpec = heightSpec;
  }

  @Nullable
  StateHandler getStateHandler() {
    return mStateHandler;
  }

  void applyStyle(InternalNode node, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
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
    return c.mComponentTree == null || c.mComponentTree.isIncrementalMountEnabled();
  }

  public static boolean isVisibilityProcessingEnabled(ComponentContext c) {
    return c.mComponentTree == null || c.mComponentTree.isVisibilityProcessingEnabled();
  }

  boolean wasLayoutCanceled() {
    return mLayoutStateContext == null ? false : mLayoutStateContext.isLayoutReleased();
  }

  boolean wasLayoutInterrupted() {
    return mLayoutStateContext == null ? false : mLayoutStateContext.isLayoutInterrupted();
  }

  public boolean isReconciliationEnabled() {
    if (getComponentTree() != null) {
      return getComponentTree().isReconciliationEnabled();
    } else {
      return ComponentsConfiguration.isReconciliationEnabled;
    }
  }

  @Nullable
  public LayoutStateContext getLayoutStateContext() {
    return mLayoutStateContext;
  }

  void markLayoutUninterruptible() {
    if (mLayoutStateContext != null) {
      mLayoutStateContext.markLayoutUninterruptible();
    }
  }

  public @ComponentTree.RecyclingMode int getRecyclingMode() {
    if (mComponentTree == null) {
      return ComponentTree.RecyclingMode.DEFAULT;
    }
    return mComponentTree.getRecyclingMode();
  }

  boolean isInputOnlyInternalNodeEnabled() {
    return mComponentTree == null
        ? ComponentsConfiguration.useInputOnlyInternalNodes
        : mComponentTree.isInputOnlyInternalNodeEnabled();
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

  public ComponentContext createUpdatedComponentContext(
      LayoutStateContext layoutStateContext, StateHandler stateHandler) {
    final ComponentContext cloned = clone();
    cloned.mLayoutStateContext = layoutStateContext;
    cloned.mStateHandler = stateHandler;
    return cloned;
  }

  public void validate() {
    if (mLayoutStateContext == null) {
      throw new IllegalStateException("Using an uninitialised context. " + getDebugString());
    }

    if (mComponentScope != null && (mGlobalKey == null || mGlobalKey.equals("null"))) {
      throw new IllegalStateException("Global key must not be null. " + getDebugString());
    }

    if (mComponentScope != null) {
      final ComponentContext context = mLayoutStateContext.getScopedContext(mGlobalKey);
      if (context != this) {
        throw new IllegalStateException("Context mismatched. " + getDebugString());
      }
    }
  }

  String getDebugString() {
    return String.format(
        "\n  "
            + "component: %s\n  "
            + "globalKey: %s\n  "
            + "wasStatelessWhenCreated: %b\n  "
            + "createdFromStatelessStatelessContext: %s\n  "
            + "copied: %s",
        mComponentScope,
        mGlobalKey != null ? "'" + mGlobalKey + "'" : "NULL",
        mWasStatelessWhenCreated,
        mCreatedFromStatelessStatelessContext != null
            ? String.valueOf(mCreatedFromStatelessStatelessContext)
            : "'NULL'",
        mLayoutStateContext != null
            ? String.valueOf(mLayoutStateContext.mIsScopedInfoCopiedFromLSCInstance)
            : "'NULL'");
  }
}
