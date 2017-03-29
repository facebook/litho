/**
 * Copyright (c) 2014-present, Facebook, Inc.
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

import com.facebook.R;
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

  // Hold a reference to the component which scope we are currently within.
  private @ThreadConfined(ThreadConfined.ANY) Component<?> mComponentScope;
  private @ThreadConfined(ThreadConfined.ANY) ResourceCache mResourceCache;
  private @ThreadConfined(ThreadConfined.ANY) int mWidthSpec;
  private @ThreadConfined(ThreadConfined.ANY) int mHeightSpec;
  private @ThreadConfined(ThreadConfined.ANY) TreeProps mTreeProps;

  private @ThreadConfined(ThreadConfined.ANY) ComponentTree mComponentTree;

  // Used to hold styling information applied to components
  private @ThreadConfined(ThreadConfined.ANY) @StyleRes int mDefStyleRes = 0;
  private @ThreadConfined(ThreadConfined.ANY) @AttrRes int mDefStyleAttr = 0;

  public ComponentContext(Context context) {
    this(context, null, null, null);
  }

  public ComponentContext(Context context, StateHandler stateHandler) {
    this(context, null, null, stateHandler);
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
   this(context, logTag, logger, null);
  }

  private ComponentContext(
      Context context,
      String logTag,
      ComponentsLogger logger,
      StateHandler stateHandler) {
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
  }

  static ComponentContext withComponentTree(
      ComponentContext context,
      ComponentTree componentTree) {
    ComponentContext componentContext = new ComponentContext(
        context,
        ComponentsPools.acquireStateHandler());
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
  static ComponentContext withComponentScope(ComponentContext context, Component scope) {
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

  public @Nullable ComponentsLogger getLogger() {
    return mLogger;
  }

  ComponentTree getComponentTree() {
    return mComponentTree;
  }

  protected void setTreeProps(TreeProps treeProps) {
    mTreeProps = treeProps;
  }

  protected @Nullable TreeProps getTreeProps() {
    return mTreeProps;
  }

  public ResourceCache getResourceCache() {
    return mResourceCache;
  }

  EventHandler newEventHandler(int id) {
    return new EventHandler(mComponentScope, id);
  }

  <E> EventHandler<E> newEventHandler(int id, Object[] params) {
    return new EventHandler<E>(mComponentScope, id, params);
  }

  ComponentLayout.ContainerBuilder newLayoutBuilder(ComponentContext c) {
    return newLayoutBuilder(0, 0);
  }

  ComponentLayout.Builder newLayoutBuilder(Component<?> component) {
    return newLayoutBuilder(component, 0, 0);
  }

  ComponentLayout.Builder newLayoutBuilder(Component.Builder componentBuilder) {
    return newLayoutBuilder(componentBuilder.build(), 0, 0);
  }

  ComponentLayout.ContainerBuilder newLayoutBuilder(
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    final InternalNode node = ComponentsPools.acquireInternalNode(this, getResources());
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

  ComponentLayout.Builder newLayoutBuilder(
      Component.Builder componentBuilder,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    return newLayoutBuilder(componentBuilder.build(), defStyleAttr, defStyleRes);
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

