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
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
public class ComponentContext extends ContextWrapper {

  static final InternalNode NULL_LAYOUT = new NoOpInternalNode();

  private final String mLogTag;
  private final ComponentsLogger mLogger;
  @Nullable private final StateHandler mStateHandler;
  private final KeyHandler mKeyHandler;
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
   *
   * @param stateUpdate state update to perform
   */
  public void updateStateSync(ComponentLifecycle.StateUpdate stateUpdate, String attribution) {
    checkIfNoStateUpdatesMethod();

    if (mComponentTree == null) {
      return;
    }

    if (ComponentsConfiguration.updateStateAsync) {
      mComponentTree.updateStateAsync(mComponentScope.getGlobalKey(), stateUpdate, attribution);
    } else {
      mComponentTree.updateStateSync(mComponentScope.getGlobalKey(), stateUpdate, attribution);
    }
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

  void enterNoStateUpdatesMethod(String noStateUpdatesMethod) {
    mNoStateUpdatesMethod = noStateUpdatesMethod;
  }

  void exitNoStateUpdatesMethod() {
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

  public @Nullable String getSplitLayoutTag() {
    if (mComponentTree == null) {
      return null;
    }

    return mComponentTree.getSplitLayoutTag();
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

  public <E> EventHandler<E> newEventHandler(int id, Object[] params) {
    return new EventHandler<>(mComponentScope, id, params);
  }

  /**
   * @return New instance of {@link EventTrigger} that is created by the current mComponentScope.
   */
  <E> EventTrigger<E> newEventTrigger(String childKey, int id) {
    String parentKey = mComponentScope == null ? "" : mComponentScope.getGlobalKey();
    return new EventTrigger<>(parentKey, id, childKey);
  }

  InternalNode newLayoutBuilder(
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    final InternalNode node = ComponentsPools.acquireInternalNode(this);
    applyStyle(node, defStyleAttr, defStyleRes);
    return node;
  }

  InternalNode newLayoutBuilder(
      Component component, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    if (component.mLayoutCreatedInWillRender != null) {
      return component.mLayoutCreatedInWillRender;
    }

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
    if (component.mLayoutCreatedInWillRender != null) {
      return component.mLayoutCreatedInWillRender;
    }

    component.updateInternalChildState(this, true);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(this, component);
    }

    final InternalNode node = (InternalNode) component.resolve(component.getScopedContext());
    if (component.isInternalComponent()) {
      final CommonPropsCopyable props = component.getCommonPropsCopyable();
      if (props != null) {
        props.copyInto(component.getScopedContext(), node);
      }
    }

    return node;
  }

  InternalNode resolveInternalComponent(Component component) {
    if (!component.isInternalComponent()) {
      throw new IllegalArgumentException("Component must be internal!");
    }

    return (InternalNode) component.resolve(this);
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

  void applyStyle(InternalNode node, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
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

  /**
   * Checks if incremental mount is enabled given a ComponentContext, so you can throw an error if
   * you require that incremental mount is enabled (e.g. you use visibility callbacks). This is
   * static to avoid polluting the ComponentContext API.
   */
  public static boolean isIncrementalMountEnabled(ComponentContext c) {
    return c.getComponentTree().isIncrementalMountEnabled();
  }
}
