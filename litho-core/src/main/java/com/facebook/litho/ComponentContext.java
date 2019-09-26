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
import com.facebook.litho.LayoutState.LayoutStateReferenceWrapper;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
public class ComponentContext {

  public static final InternalNode NULL_LAYOUT = new NoOpInternalNode();

  static final String NO_SCOPE_EVENT_HANDLER = "ComponentContext:NoScopeEventHandler";
  private final Context mContext;
  // TODO: T48229786 move to CT
  private final @Nullable String mLogTag;
  private final @Nullable ComponentsLogger mLogger;
  private final @Nullable StateHandler mStateHandler;

  private @Nullable String mNoStateUpdatesMethod;

  // Hold a reference to the component which scope we are currently within.
  @ThreadConfined(ThreadConfined.ANY)
  private Component mComponentScope;

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
  private ComponentTree mComponentTree;

  // Used to hold styling information applied to components
  @StyleRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleRes = 0;

  @AttrRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleAttr = 0;

  private @Nullable ComponentTree.LayoutStateFuture mLayoutStateFuture;

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable LayoutStateReferenceWrapper mLayoutStateReferenceWrapper;

  public ComponentContext(Context context) {
    this(context, null, null, null);
  }

  public ComponentContext(
      Context context, LayoutStateReferenceWrapper layoutStateReferenceWrapper) {
    this(context, null, null, null);
  }

  public ComponentContext(Context context, StateHandler stateHandler) {
    this(context, null, null, stateHandler, null);
  }

  /**
   * Constructor that can be used to receive log data from components. Check {@link
   * ComponentsLogger} for the type of events you can listen for.
   *
   * @param context Android context.
   * @param logTag a log tag to be used with the logger.
   * @param logger a lifecycle logger to be used.
   */
  public ComponentContext(Context context, String logTag, @Nullable ComponentsLogger logger) {
    this(context, logTag, logger, null);
  }

  public ComponentContext(
      Context context,
      @Nullable String logTag,
      @Nullable ComponentsLogger logger,
      @Nullable TreeProps treeProps) {
    this(context, logTag, logger, null, treeProps);
  }

  public ComponentContext(
      Context context,
      @Nullable String logTag,
      @Nullable ComponentsLogger logger,
      @Nullable StateHandler stateHandler,
      @Nullable TreeProps treeProps) {

    if (logger != null && logTag == null) {
      throw new IllegalStateException("When a ComponentsLogger is set, a LogTag must be set");
    }

    mContext = context;
    mResourceCache = ResourceCache.getLatest(context.getResources().getConfiguration());
    mResourceResolver = new ResourceResolver(this);
    mTreeProps = treeProps;
    mLogger = logger;
    mLogTag = logTag;
    mStateHandler = stateHandler;
  }

  public ComponentContext(ComponentContext context) {
    this(
        context,
        context.mStateHandler,
        context.mTreeProps,
        context.mLayoutStateFuture,
        context.mLayoutStateReferenceWrapper);
  }

  public ComponentContext(
      ComponentContext context,
      @Nullable StateHandler stateHandler,
      @Nullable TreeProps treeProps,
      @Nullable ComponentTree.LayoutStateFuture layoutStateFuture,
      @Nullable LayoutStateReferenceWrapper layoutStateReferenceWrapper) {

    mContext = context.mContext;
    mResourceCache = context.mResourceCache;
    mResourceResolver = context.mResourceResolver;
    mWidthSpec = context.mWidthSpec;
    mHeightSpec = context.mHeightSpec;
    mComponentScope = context.mComponentScope;
    mComponentTree = context.mComponentTree;
    mLayoutStateReferenceWrapper = layoutStateReferenceWrapper;
    mLogger = context.mLogger;
    mLogTag =
        context.mLogTag != null || mComponentTree == null
            ? context.mLogTag
            : mComponentTree.getSimpleName();

    mStateHandler = stateHandler != null ? stateHandler : context.mStateHandler;
    mTreeProps = treeProps != null ? treeProps : context.mTreeProps;
    mLayoutStateFuture = layoutStateFuture != null ? layoutStateFuture : context.mLayoutStateFuture;
  }

  ComponentContext makeNewCopy() {
    return new ComponentContext(this);
  }

  void setLayoutStateReferenceWrapper(LayoutStateReferenceWrapper layoutStateReferenceWrapper) {
    mLayoutStateReferenceWrapper = layoutStateReferenceWrapper;
  }

  public void setLayoutStateReferenceWrapperForTesting() {
    setLayoutStateReferenceWrapper(LayoutStateReferenceWrapper.getTestInstance(this));
  }

  /**
   * Returns true if this method is called during a layout state calculation and the LayoutState
   * reference hasn't been nullified.
   */
  boolean hasLayoutState() {
    return mLayoutStateReferenceWrapper != null
        && mLayoutStateReferenceWrapper.getLayoutState() != null;
  }

  @Nullable
  LayoutState getLayoutState() {
    return mLayoutStateReferenceWrapper == null
        ? null
        : mLayoutStateReferenceWrapper.getLayoutState();
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

  @Nullable
  @VisibleForTesting
  public ComponentTree.LayoutStateFuture getLayoutStateFuture() {
    return mLayoutStateFuture;
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

    mComponentTree.updateStateSync(mComponentScope.getGlobalKey(), stateUpdate, attribution);
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

    mComponentTree.updateStateAsync(mComponentScope.getGlobalKey(), stateUpdate, attribution);
  }

  public void updateStateWithTransition(StateUpdate stateUpdate, String attribution) {
    updateStateAsync(stateUpdate, attribution);
  }

  public void updateStateLazy(StateUpdate stateUpdate) {
    if (mComponentTree == null) {
      return;
    }

    mComponentTree.updateStateLazy(mComponentScope.getGlobalKey(), stateUpdate);
  }

  public void applyLazyStateUpdatesForContainer(StateContainer container) {
    if (mComponentTree == null) {
      return;
    }

    mComponentTree.applyLazyStateUpdatesForContainer(mComponentScope.getGlobalKey(), container);
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

  protected void setTreeProps(@Nullable TreeProps treeProps) {
    mTreeProps = treeProps;
  }

  @Nullable
  protected TreeProps getTreeProps() {
    return mTreeProps;
  }

  @Nullable
  public <T> T getTreeProp(Class<T> key) {
    return mTreeProps == null ? null : mTreeProps.get(key);
  }

  /** Obtain a copy of the tree props currently held by this context. */
  @Nullable
  public TreeProps getTreePropsCopy() {
    return TreeProps.copy(mTreeProps);
  }

  public ResourceCache getResourceCache() {
    return mResourceCache;
  }

  EventHandler newEventHandler(int id) {
    checkNotNullScope();
    return new EventHandler(mComponentScope, id);
  }

  public <E> EventHandler<E> newEventHandler(int id, Object[] params) {
    checkNotNullScope();
    return new EventHandler<>(mComponentScope, id, params);
  }

  private void checkNotNullScope() {
    if (mComponentScope == null) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          NO_SCOPE_EVENT_HANDLER,
          "Creating event handler without scope.");
    }
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
  <E> EventTrigger<E> newEventTrigger(String childKey, int id) {
    String parentKey = mComponentScope == null ? "" : mComponentScope.getGlobalKey();
    return new EventTrigger<>(parentKey, id, childKey);
  }

  InternalNode newLayoutBuilder(@AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    final InternalNode node = InternalNodeUtils.create(this);
    applyStyle(node, defStyleAttr, defStyleRes);
    return node;
  }

  InternalNode newLayoutBuilder(
      Component component, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();
    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender;
    }

    component = component.getThreadSafeInstance();

    component.updateInternalChildState(this);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(this, component);
    }

    final InternalNode node =
        LayoutState.createLayout(component.getScopedContext(), component, false);
    if (node != NULL_LAYOUT) {
      applyStyle(node, defStyleAttr, defStyleRes);
    }

    return node;
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

      final TypedArray typedArray =
          mContext.obtainStyledAttributes(
              null, R.styleable.ComponentLayout, defStyleAttr, defStyleRes);
      node.applyAttributes(typedArray);
      typedArray.recycle();

      setDefStyle(0, 0);
    }
  }

  static ComponentContext withComponentTree(ComponentContext context, ComponentTree componentTree) {
    ComponentContext componentContext =
        new ComponentContext(context, new StateHandler(), null, null, null);
    componentContext.mComponentTree = componentTree;
    componentContext.mComponentScope = null;
    componentContext.mLayoutStateFuture = null;

    return componentContext;
  }

  /**
   * Creates a new ComponentContext instance scoped to the given component and sets it on the
   * component.
   *
   * @param context context scoped to the parent component
   * @param scope component associated with the newly created scoped context
   * @return a new ComponentContext instance scoped to the given component
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static ComponentContext withComponentScope(ComponentContext context, Component scope) {
    ComponentContext componentContext = context.makeNewCopy();
    componentContext.mComponentScope = scope;
    componentContext.mComponentTree = context.mComponentTree;

    return componentContext;
  }

  /**
   * Checks if incremental mount is enabled given a ComponentContext, so you can throw an error if
   * you require that incremental mount is enabled (e.g. you use visibility callbacks). This is
   * static to avoid polluting the ComponentContext API.
   */
  public static boolean isIncrementalMountEnabled(ComponentContext c) {
    return c.mComponentTree == null || c.mComponentTree.isIncrementalMountEnabled();
  }

  boolean isLayoutStateCachingEnabled() {
    return mComponentTree != null && mComponentTree.shouldCacheInternalNodeOnLayoutState();
  }

  boolean wasLayoutCanceled() {
    return mLayoutStateFuture != null && mLayoutStateFuture.isReleased();
  }

  boolean wasLayoutInterrupted() {
    return mLayoutStateFuture != null && mLayoutStateFuture.isInterrupted();
  }

  public boolean isReconciliationEnabled() {
    if (getComponentTree() != null) {
      return getComponentTree().isReconciliationEnabled();
    } else {
      return ComponentsConfiguration.isReconciliationEnabled;
    }
  }
}
