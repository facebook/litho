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
import com.facebook.yoga.YogaNode;

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
public class ComponentContext {

  public interface YogaNodeFactory {
    YogaNode create();
  };

  static final InternalNode NULL_LAYOUT = new NoOpInternalNode();

  private final Context mContext;
  private final @Nullable String mLogTag;
  private final ComponentsLogger mLogger;
  private final @Nullable StateHandler mStateHandler;
  final YogaNodeFactory mYogaNodeFactory;

  /** TODO: (T38237241) remove the usage of the key handler post the nested tree experiment */
  private final @Nullable KeyHandler mKeyHandler;

  private String mNoStateUpdatesMethod;

  // Hold a reference to the component which scope we are currently within.
  @ThreadConfined(ThreadConfined.ANY)
  private Component mComponentScope;
  @ThreadConfined(ThreadConfined.ANY)
  private final ResourceCache mResourceCache;
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

  private ComponentTree.LayoutStateFuture mLayoutStateFuture;

  public ComponentContext(
      Context context,
      @Nullable String logTag,
      ComponentsLogger logger,
      @Nullable StateHandler stateHandler,
      @Nullable KeyHandler keyHandler,
      @Nullable TreeProps treeProps,
      YogaNodeFactory yogaNodeFactory) {

    if (logger != null && logTag == null) {
      throw new IllegalStateException("When a ComponentsLogger is set, a LogTag must be set");
    }

    mContext = context;
    mResourceCache = ResourceCache.getLatest(context.getResources().getConfiguration());
    mTreeProps = treeProps;
    mLogger = logger;
    mLogTag = logTag;
    mStateHandler = stateHandler;
    mKeyHandler = keyHandler;
    mYogaNodeFactory = yogaNodeFactory;
  }

  public ComponentContext(
      ComponentContext context,
      @Nullable StateHandler stateHandler,
      @Nullable KeyHandler keyHandler,
      @Nullable TreeProps treeProps,
      @Nullable ComponentTree.LayoutStateFuture layoutStateFuture) {

    mContext = context.getAndroidContext();
    mResourceCache = context.mResourceCache;
    mWidthSpec = context.mWidthSpec;
    mHeightSpec = context.mHeightSpec;
    mComponentScope = context.mComponentScope;
    mComponentTree = context.mComponentTree;
    mLogger = context.mLogger;
    mLogTag =
        context.mLogTag != null || mComponentTree == null
            ? context.mLogTag
            : mComponentTree.getSimpleName();
    mYogaNodeFactory = context.mYogaNodeFactory;

    mStateHandler = stateHandler != null ? stateHandler : context.mStateHandler;
    mKeyHandler = keyHandler != null ? keyHandler : context.mKeyHandler;
    mTreeProps = treeProps != null ? treeProps : context.mTreeProps;
    mLayoutStateFuture = layoutStateFuture == null ? context.mLayoutStateFuture : layoutStateFuture;
  }

  public ComponentContext(
      Context context,
      @Nullable String logTag,
      ComponentsLogger logger,
      @Nullable StateHandler stateHandler,
      @Nullable KeyHandler keyHandler,
      @Nullable TreeProps treeProps) {
    this(context, logTag, logger, stateHandler, keyHandler, treeProps, null);
  }

  public ComponentContext(
      Context context,
      @Nullable String logTag,
      ComponentsLogger logger,
      @Nullable TreeProps treeProps) {
    this(context, logTag, logger, null, null, treeProps);
  }

  /**
   * Constructor that can be used to receive log data from components. Check {@link
   * ComponentsLogger} for the type of events you can listen for.
   *
   * @param context Android context.
   * @param logTag Specify a log tag, to be used with the logger.
   * @param logger Specify the lifecycle logger to be used.
   */
  public ComponentContext(Context context, @Nullable String logTag, ComponentsLogger logger) {
    this(context, logTag, logger, null, null, null);
  }

  public ComponentContext(Context context, StateHandler stateHandler) {
    this(context, null, null, stateHandler, null, null);
  }

  public ComponentContext(Context context, YogaNodeFactory yogaNodeFactory) {
    this(context, null, null, null, null, null, yogaNodeFactory);
  }

  public ComponentContext(ComponentContext context) {
    this(
        context,
        context.mStateHandler,
        context.mKeyHandler,
        context.mTreeProps,
        context.mLayoutStateFuture);
  }

  public ComponentContext(Context context) {
    this(context, null, null, null, null, null);
  }

  ComponentContext makeNewCopy() {
    return new ComponentContext(this);
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

  /**
   * Notify the Component Tree that it needs to synchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  public void updateStateSync(ComponentLifecycle.StateUpdate stateUpdate, String attribution) {
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
  public void updateStateAsync(ComponentLifecycle.StateUpdate stateUpdate, String attribution) {
    checkIfNoStateUpdatesMethod();

    if (mComponentTree == null) {
      return;
    }

    mComponentTree.updateStateAsync(mComponentScope.getGlobalKey(), stateUpdate, attribution);
  }

  public void updateStateWithTransition(
      ComponentLifecycle.StateUpdate stateUpdate, String attribution) {
    updateStateAsync(stateUpdate, attribution);
  }

  public void updateStateLazy(ComponentLifecycle.StateUpdate stateUpdate) {
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

  public String getLogTag() {
    return mLogTag;
  }

  public YogaNodeFactory getYogaNodeFactory() {
    return mYogaNodeFactory;
  }

  @Nullable
  public ComponentsLogger getLogger() {
    return mLogger;
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

  /** Obtain a copy of the tree props currently held by this context. */
  @Nullable
  public TreeProps getTreePropsCopy() {
    return TreeProps.copy(mTreeProps);
  }

  public ResourceCache getResourceCache() {
    return mResourceCache;
  }

  EventHandler newEventHandler(int id) {
    return new EventHandler(mComponentScope, id);
  }

  public <E> EventHandler<E> newEventHandler(int id, Object[] params) {
    return new EventHandler<>(mComponentScope, id, params);
  }

  @Nullable
  public Object getCachedValue(Object cachedValueInputs) {
    return mComponentTree.getCachedValue(cachedValueInputs);
  }

  public void putCachedValue(Object cachedValueInputs, Object cachedValue) {
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

    final InternalNode node = component.createLayout(component.getScopedContext(), false);

    if (node != NULL_LAYOUT) {
      applyStyle(node, defStyleAttr, defStyleRes);
    }

    return node;
  }

  InternalNode resolveLayout(Component component) {
    final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();
    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender;
    }

    component = component.getThreadSafeInstance();

    component.updateInternalChildState(this);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(this, component);
    }

    final InternalNode node = (InternalNode) component.resolve(component.getScopedContext());
    if (component.canResolve()) {
      final CommonPropsCopyable props = component.getCommonPropsCopyable();
      if (props != null) {
        props.copyInto(component.getScopedContext(), node);
      }
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

  @Nullable
  KeyHandler getKeyHandler() {
    return mKeyHandler;
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
    return c.getComponentTree().isIncrementalMountEnabled();
  }

  /** Whether the refactored implementation of nested tree resolution should be used. */
  public boolean isNestedTreeResolutionExperimentEnabled() {
    if (getComponentTree() != null) {
      return getComponentTree().isNestedTreeResolutionExperimentEnabled();
    } else {
      return false;
    }
  }
}
