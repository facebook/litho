/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadConfined;

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
public class ComponentContext extends ContextWrapper {

  static final InternalNode NULL_LAYOUT = new NoOpInternalNode();

  private final String mLogTag;
  private final ComponentsLogger mLogger;
  private final StateHandler mStateHandler;
  private final KeyHandler mKeyHandler;
  private String mNoStateUpdatesMethod;

  // Hold a reference to the component which scope we are currently within.
  @ThreadConfined(ThreadConfined.ANY)
  private Component<?> mComponentScope;
  @ThreadConfined(ThreadConfined.ANY)
  private final ResourceCache mResourceCache;
  @ThreadConfined(ThreadConfined.ANY)
  private int mWidthSpec;
  @ThreadConfined(ThreadConfined.ANY)
  private int mHeightSpec;
  @ThreadConfined(ThreadConfined.ANY)
  protected TreeProps mTreeProps;

  @ThreadConfined(ThreadConfined.ANY)
  private ComponentTree mComponentTree;

  // Used to hold styling information applied to components
  @StyleRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleRes = 0;
  @AttrRes
  @ThreadConfined(ThreadConfined.ANY)
  private int mDefStyleAttr = 0;

  public ComponentContext(Context context) {
    this(context, null, null, null, null);
  }

  public ComponentContext(Context context, StateHandler stateHandler) {
    this(context, stateHandler, null);
  }

  public ComponentContext(Context context, StateHandler stateHandler, KeyHandler keyHandler) {
    this(context, null, null, stateHandler, keyHandler);
  }

  /**
   *  Constructor that can be used to receive log data from components.
   *  Check {@link ComponentsLogger} for the type of events you can listen for.
   *
   * @param context Android context.
   * @param logTag Specify a log tag, to be used with the logger.
   * @param logger Specify the lifecycle logger to be used.
   */
  public ComponentContext(Context context, String logTag, ComponentsLogger logger) {
    this(context, logTag, logger, null, null);
  }

  private ComponentContext(
      Context context,
      String logTag,
      ComponentsLogger logger,
      StateHandler stateHandler,
      KeyHandler keyHandler) {
    super((context instanceof ComponentContext)
        ? ((ComponentContext) context).getBaseContext()
        : context);

    if (logger != null && logTag == null) {
      throw new IllegalStateException("When a ComponentsLogger is set, a LogTag must be set");
    }

    final ComponentContext componentContext = (context instanceof ComponentContext)
        ? (ComponentContext) context
        : null;
    final boolean transferLogging = (componentContext != null && logTag == null && logger == null);
    final boolean transferStateHandler = (componentContext != null && stateHandler == null);
    final boolean transferKeyHandler = (componentContext != null && keyHandler == null);

    if (componentContext != null) {
      mTreeProps = componentContext.mTreeProps;
      mResourceCache = componentContext.mResourceCache;
      mWidthSpec = componentContext.mWidthSpec;
      mHeightSpec = componentContext.mHeightSpec;
      mComponentScope = componentContext.mComponentScope;
      mComponentTree = componentContext.mComponentTree;
    } else {
      mResourceCache = ResourceCache.getLatest(context.getResources().getConfiguration());
    }

    mLogger = transferLogging ? componentContext.mLogger : logger;
    mLogTag = transferLogging ? componentContext.mLogTag : logTag;
    mStateHandler = transferStateHandler ? componentContext.mStateHandler : stateHandler;
    mKeyHandler = transferKeyHandler ? componentContext.mKeyHandler : keyHandler;
  }

  static ComponentContext withComponentTree(
      ComponentContext context,
      ComponentTree componentTree) {
    ComponentContext componentContext =
        new ComponentContext(context, ComponentsPools.acquireStateHandler(), context.mKeyHandler);
    componentContext.mComponentTree = componentTree;

    return componentContext;
  }

  /**
   * Creates a new ComponentContext instance scoped to the given component and sets it on the
   *  component.
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

  ComponentContext makeNewCopy() {
    return new ComponentContext(this);
  }

  public Component getComponentScope() {
    return mComponentScope;
  }

  /**
   * Notify the Component Tree that it needs to synchronously perform a state update.
   * @param stateUpdate state update to perform
   */
  public void updateState(ComponentLifecycle.StateUpdate stateUpdate) {
    checkIfNoStateUpdatesMethod();

    if (mComponentTree == null) {
      return;
    }

    mComponentTree.updateState(mComponentScope.getGlobalKey(), stateUpdate);
  }

  /**
   * Notify the Component Tree that it needs to asynchronously perform a state update.
   * @param stateUpdate state update to perform
   */
  public void updateStateAsync(ComponentLifecycle.StateUpdate stateUpdate) {
    checkIfNoStateUpdatesMethod();

    if (mComponentTree == null) {
      return;
    }

    mComponentTree.updateStateAsync(mComponentScope.getGlobalKey(), stateUpdate);
  }

  public void updateStateLazy(ComponentLifecycle.StateUpdate stateUpdate) {
    if (mComponentTree == null) {
      return;
    }

    mComponentTree.updateStateLazy(mComponentScope.getGlobalKey(), stateUpdate);
  }

  public void enterNoStateUpdatesMethod(String noStateUpdatesMethod) {
    mNoStateUpdatesMethod = noStateUpdatesMethod;
  }

  public void exitNoStateUpdatesMethod() {
    mNoStateUpdatesMethod = null;
  }

  private void checkIfNoStateUpdatesMethod() {
    if (mNoStateUpdatesMethod != null) {
      throw new IllegalStateException(
          "Updating the state of a component during " +
              mNoStateUpdatesMethod +
              " leads to unexpected behaviour, consider using lazy state updates.");
    }
  }

  void setDefStyle(@AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    mDefStyleAttr = defStyleAttr;
    mDefStyleRes = defStyleRes;
  }

  public TypedArray obtainStyledAttributes(int[] attrs, @AttrRes int defStyleAttr) {
    return obtainStyledAttributes(
        null,
        attrs,
        defStyleAttr != 0 ? defStyleAttr : mDefStyleAttr,
        mDefStyleRes);
  }

  public String getLogTag() {
    return mLogTag;
  }

  @Nullable
  public ComponentsLogger getLogger() {
    return mLogger;
  }

  ComponentTree getComponentTree() {
    return mComponentTree;
  }

  protected void setTreeProps(TreeProps treeProps) {
    mTreeProps = treeProps;
  }

  @Nullable
  protected TreeProps getTreeProps() {
    return mTreeProps;
  }

  public ResourceCache getResourceCache() {
    return mResourceCache;
  }

  EventHandler newEventHandler(int id) {
    return new EventHandler(mComponentScope, id);
  }

  <E> EventHandler<E> newEventHandler(String name, int id, Object[] params) {
    return new EventHandler<E>(mComponentScope, name, id, params);
  }

  /** @return New instance of {@link EventTrigger} that is owned by the current mComponentScope */
  <E> EventTrigger<E> newEventTrigger() {
    return new EventTrigger<>(mComponentScope);
  }

  /**
   * Keep a referenece to {@link EventTrigger} in {@link ComponentTree} to allow a retrieval of the
   * same reference with a key.
   */
  public void registerTrigger(EventTrigger trigger, String key) {
    if (mComponentTree == null) {
      return;
    }

    mComponentTree.recordEventTrigger(key, trigger);
  }

  /**
   * Remove a referenece of {@link EventTrigger} in {@link ComponentTree} with the key it was
   * registered with.
   */
  public void unregisterTrigger(String key) {
    if (mComponentTree == null) {
      return;
    }

    mComponentTree.releaseEventTrigger(key);
  }

  InternalNode newLayoutBuilder(
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    final InternalNode node = ComponentsPools.acquireInternalNode(this);
    applyStyle(node, defStyleAttr, defStyleRes);
    return node;
  }

  ComponentLayout.Builder newLayoutBuilder(
      Component<?> component,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    component.applyStateUpdates(this);

    final InternalNode node = (InternalNode) component.getLifecycle().createLayout(
        component.getScopedContext(),
        component,
        false);
    component.getScopedContext().setTreeProps(null);

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

  StateHandler getStateHandler() {
    return mStateHandler;
  }

  KeyHandler getKeyHandler() {
    return mKeyHandler;
  }

  private void applyStyle(InternalNode node, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    if (defStyleAttr != 0 || defStyleRes != 0) {
      setDefStyle(defStyleAttr, defStyleRes);

      final TypedArray typedArray = obtainStyledAttributes(
          null,
          R.styleable.ComponentLayout,
          defStyleAttr,
          defStyleRes);
      node.applyAttributes(typedArray);
      typedArray.recycle();

      setDefStyle(0, 0);
    }
  }
}
