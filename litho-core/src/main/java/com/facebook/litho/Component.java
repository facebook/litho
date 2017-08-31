/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.ComponentLifecycle.MountType;
import com.facebook.litho.ComponentLifecycle.StateContainer;
import com.facebook.litho.config.ComponentsConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a unique instance of a component that is driven by its matching
 * {@link ComponentLifecycle}. To create new {@link Component} instances, use the
 * {@code create()} method in the generated {@link ComponentLifecycle} subclass which
 * returns a builder that allows you to set values for individual props. {@link Component}
 * instances are immutable after creation.
 */
public abstract class Component<L extends ComponentLifecycle> implements HasEventDispatcher {
  private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
  private int mId = sIdGenerator.getAndIncrement();
  private String mGlobalKey;
  private String mKey;
  private boolean mHasManualKey;

  private final L mLifecycle;
  @ThreadConfined(ThreadConfined.ANY)
  private ComponentContext mScopedContext;

  private boolean mIsLayoutStarted = false;

  // If we have a cachedLayout, onPrepare and onMeasure would have been called on it already.
  @ThreadConfined(ThreadConfined.ANY)
  private InternalNode mLastMeasuredLayout;

  // This is just being used for an experiment right now. Do not use for anything else.
  private LayoutAttributes mLayoutAttributes;

  /**
   * Holds onto how many direct component children of each type this Component has. Used for
   * automatically generating unique global keys for all sibling components of the same type.
   */
  private Map<String, Integer> mChildCounters = new HashMap<>();

  /**
   * Mostly used by logging to provide more readable messages.
   */
  public abstract String getSimpleName();

  /**
   * Compares this component to a different one to check if they are the same
   *
   * This is used to be able to skip rendering a component again. We avoid using the
   * {@link Object#equals(Object)} so we can optimize the code better over time since we don't have
   * to adhere to the contract required for a equals method.
   *
   * @param other the component to compare to
   * @return true if the components are of the same type and have the same props
   */
  public boolean isEquivalentTo(Component<?> other) {
    return this == other;
  }

  protected StateContainer getStateContainer() {
    return null;
  }

  public ComponentContext getScopedContext() {
    return mScopedContext;
  }

  public void setScopedContext(ComponentContext scopedContext) {
    mScopedContext = scopedContext;
  }

  synchronized void markLayoutStarted() {
    if (mIsLayoutStarted) {
      throw new IllegalStateException("Duplicate layout of a component: " + this);
    }
    mIsLayoutStarted = true;
  }

  // Get an id that is identical across cloned instances, but otherwise unique
  protected int getId() {
    return mId;
  }

  /**
   * Get a key that is unique to this component within its tree.
   * @return
   */
  String getGlobalKey() {
    return mGlobalKey;
  }

  /**
   * Set a key for this component that is unique within its tree.
   * @param key
   *
   */
  // thread-safe because the one write is before all the reads
  @ThreadSafe(enableChecks = false)
  private void setGlobalKey(String key) {
    mGlobalKey = key;
  }

  /**
   *
   * @return a key that is local to the component's parent.
   */
  String getKey() {
    return mKey;
  }

  /**
   * Set a key that is local to the parent of this component.
   * @param key key
   */
  void setKey(String key) {
    mHasManualKey = true;
    mKey = key;
  }

  /**
   * Generate a global key for the given component that is unique among all of this component's
   * children of the same type. If a manual key has been set on the child component using the .key()
   * method, return the manual key.
   *
   * @param component the child component for which we're finding a unique global key
   * @param key the key of the child component as determined by its lifecycle id or manual setting
   * @return a unique global key for this component relative to its siblings.
   */
  private String generateUniqueGlobalKeyForChild(Component component, String key) {

    final String childKey = getGlobalKey() + key;
    final KeyHandler keyHandler = mScopedContext.getKeyHandler();

    /** Null check is for testing only, the keyHandler should never be null here otherwise. */
    if (component.mHasManualKey || keyHandler == null) {
      return childKey;
    }

    /** If the key is already unique, return it. */
    if (!keyHandler.hasKey(childKey)) {
      return childKey;
    }

    final String childType = component.getSimpleName();

    /**
     * If the key is a duplicate, we start appending an index based on the child component's type
     * that would uniquely identify it.
     */
    int childIndex = mChildCounters.containsKey(childType) ? mChildCounters.get(childType) : 0;

    /**
     * Specs that implement {@link com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec} will
     * call onCreateLayout more than once, so we might record a key in the key handler that doesn't
     * end up being used in the valid layout output. We'll need to try increasing the index until we
     * hit a unique key.
     */
    String uniqueKey = childKey + childIndex;
    while (keyHandler.hasKey(uniqueKey)) {
      uniqueKey = childKey + (childIndex++);
    }

    mChildCounters.put(childType, childIndex + 1);

    return uniqueKey;
  }

  Component<L> makeCopyWithNullContext() {
    try {
      final Component<L> component = (Component<L>) super.clone();
      component.mScopedContext = null;
      return component;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public Component<L> makeShallowCopy() {
    try {
      final Component<L> component = (Component<L>) super.clone();
      component.mIsLayoutStarted = false;
      component.mChildCounters = new HashMap<>();
      component.mHasManualKey = false;

      return component;
    } catch (CloneNotSupportedException e) {
      // Subclasses implement Cloneable, so this is impossible
      throw new RuntimeException(e);
    }
  }

  Component<L> makeShallowCopyWithNewId() {
    final Component<L> component = makeShallowCopy();
    component.mId = sIdGenerator.incrementAndGet();
    return component;
  }

  boolean hasCachedLayout() {
    return (mLastMeasuredLayout != null);
  }

  InternalNode getCachedLayout() {
    return mLastMeasuredLayout;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  protected void releaseCachedLayout() {
    if (mLastMeasuredLayout != null) {
      LayoutState.releaseNodeTree(mLastMeasuredLayout, true /* isNestedTree */);
      mLastMeasuredLayout = null;
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  protected void clearCachedLayout() {
    mLastMeasuredLayout = null;
  }

  void release() {
    mIsLayoutStarted = false;
  }

  protected Component(L lifecycle) {
    mLifecycle = lifecycle;
    mKey = Integer.toString(mLifecycle.getTypeId());
  }

  public L getLifecycle() {
    return mLifecycle;
  }

  /**
   * Measure a component with the given {@link SizeSpec} constrain.
   *
   * @param c {@link ComponentContext}.
   * @param widthSpec Width {@link SizeSpec} constrain.
   * @param heightSpec Height {@link SizeSpec} constrain.
   * @param outputSize Size object that will be set with the measured dimensions.
   */
  public void measure(ComponentContext c, int widthSpec, int heightSpec, Size outputSize) {
    releaseCachedLayout();

    mLastMeasuredLayout = LayoutState.createAndMeasureTreeForComponent(
        c,
        this,
        widthSpec,
        heightSpec);

    // This component resolution won't be deferred nor onMeasure called if it's a layout spec.
    // In that case it needs to manually save the latest saze specs.
    // The size specs will be checked during the calculation (or collection) of the main tree.
    if (Component.isLayoutSpec(this)) {
      mLastMeasuredLayout.setLastWidthSpec(widthSpec);
      mLastMeasuredLayout.setLastHeightSpec(heightSpec);
    }

    outputSize.width = mLastMeasuredLayout.getWidth();
    outputSize.height = mLastMeasuredLayout.getHeight();
  }

  protected void copyInterStageImpl(Component<L> component) {

  }

  static boolean isHostSpec(Component<?> component) {
    return (component != null && component.mLifecycle instanceof HostComponent);
  }

  static boolean isLayoutSpec(Component<?> component) {
    return (component != null && component.mLifecycle.getMountType() == MountType.NONE);
  }

  static boolean isMountSpec(Component<?> component) {
    return (component != null && component.mLifecycle.getMountType() != MountType.NONE);
  }

  static boolean isMountDrawableSpec(Component<?> component) {
    return (component != null && component.mLifecycle.getMountType() == MountType.DRAWABLE);
  }

  static boolean isMountViewSpec(Component<?> component) {
    return (component != null && component.mLifecycle.getMountType() == MountType.VIEW);
  }

  static boolean isLayoutSpecWithSizeSpec(Component<?> component) {
    return (isLayoutSpec(component) && component.mLifecycle.canMeasure());
  }

  static boolean isNestedTree(Component<?> component) {
    return (isLayoutSpecWithSizeSpec(component)
        || (component != null && component.hasCachedLayout()));
  }

  /**
   * Prepares a component for calling any pending state updates on it by setting a global key,
   * setting the TreeProps which the component requires from its parent,
   * setting a scoped component context and applies the pending state updates.
   * @param c component context
   */
  void applyStateUpdates(ComponentContext c) {
    final Component<?> parentScope = c.getComponentScope();
    final String key = getKey();

    setGlobalKey(
        parentScope == null ? key : parentScope.generateUniqueGlobalKeyForChild(this, key));

    setScopedContext(ComponentContext.withComponentScope(c, this));

    getLifecycle().populateTreeProps(this, getScopedContext().getTreeProps());

    final KeyHandler keyHandler = getScopedContext().getKeyHandler();
    /** This is for testing, the keyHandler should never be null here otherwise. */
    if (keyHandler != null && !ComponentsConfiguration.isEndToEndTestRun) {
      keyHandler.registerKey(this);
    }

    if (getLifecycle().hasState()) {
      c.getStateHandler().applyStateUpdatesForComponent(this);
    }
  }

  @Override
  public EventDispatcher getEventDispatcher() {
    return mLifecycle;
  }

  /**
   * @param <L> the {@link ComponentLifecycle} of the {@link Component} that this builder will
   *     build.
   * @param <T> the type of this builder. Required to ensure methods defined here in the abstract
   *     class correctly return the type of the concrete subclass.
   */
  public abstract static class Builder<L extends ComponentLifecycle, T extends Builder<L, T>>
      extends ResourceResolver {
    private ComponentContext mContext;
    @AttrRes private int mDefStyleAttr;
    @StyleRes private int mDefStyleRes;
    private Component mComponent;

    protected void init(
        ComponentContext c,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        Component<L> component) {
      super.init(c, c.getResourceCache());

      mComponent = component;
      mContext = c;
      mDefStyleAttr = defStyleAttr;
      mDefStyleRes = defStyleRes;

      if (defStyleAttr != 0 || defStyleRes != 0) {
        component.mLifecycle.loadStyle(c, defStyleAttr, defStyleRes, component);
      }
    }

    public abstract T getThis();

    /** Set a key on the component that is local to its parent. */
    public T key(String key) {
      mComponent.setKey(key);
      return getThis();
    }

    @Override
    protected void release() {
      super.release();

      mContext = null;
      mDefStyleAttr = 0;
      mDefStyleRes = 0;
      mComponent = null;
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

    public final ComponentLayout buildWithLayout() {
      return this.withLayout(false).build();
    }

    public final ComponentLayout.Builder withLayout() {
      return this.withLayout(ComponentsConfiguration.storeLayoutAttributesInSeparateObject);
    }

    private ComponentLayout.Builder withLayout(boolean useSeparateInternalNode) {
      // calling build() which will release this builder setting these members to null/0.
      // We must capture their value before that happens.
      final ComponentContext context = mContext;
      final Component<?> component = mComponent;
      final int defStyleAttr = mDefStyleAttr;
      final int defStyleRes = mDefStyleRes;

      InternalNode internalNode =
          (InternalNode) Layout.create(context, build(), defStyleAttr, defStyleRes);

      if (useSeparateInternalNode) {
        component.mLayoutAttributes = new LayoutAttributes();
        component.mLayoutAttributes.init(context, internalNode);
        return component.mLayoutAttributes;
      } else {
        return internalNode;
      }
    }

    @ReturnsOwnership
    public abstract Component<L> build();
  }
}
