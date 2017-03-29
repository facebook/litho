/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import java.util.concurrent.atomic.AtomicInteger;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;

import com.facebook.litho.ComponentLifecycle.MountType;
import com.facebook.litho.ComponentLifecycle.StateContainer;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;

/**
 * Represents a unique instance of a component that is driven by its matching
 * {@link ComponentLifecycle}. To create new {@link Component} instances, use the
 * {@code create()} method in the generated {@link ComponentLifecycle} subclass which
 * returns a builder that allows you to set values for individual props. {@link Component}
 * instances are immutable after creation.
 */
public abstract class Component<L extends ComponentLifecycle> implements HasEventDispatcher {

  public static abstract class Builder<L extends ComponentLifecycle>
      extends ResourceResolver {
    private ComponentContext mContext;
    private @AttrRes int mDefStyleAttr;
    private @StyleRes int mDefStyleRes;
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

    /**
     * Set a key on the component that is local to its parent.
     */
    protected void setKey(String key) {
      mComponent.setKey(key);
    }

    public abstract Component.Builder<L> key(String key);

    @Override
    protected void release() {
      super.release();

      mContext = null;
      mDefStyleAttr = 0;
      mDefStyleRes = 0;
      mComponent = null;
    }

    public final ComponentLayout buildWithLayout() {
      return this.withLayout().flexShrink(0).build();
    }

    public final ComponentLayout.Builder withLayout() {
      // calling build() which will release this builder setting these members to null/0.
      // We must capture their value before that happens.
      final ComponentContext context = mContext;
      final int defStyleAttr = mDefStyleAttr;
      final int defStyleRes = mDefStyleRes;

      return Layout.create(context, build(), defStyleAttr, defStyleRes);
    }

    public abstract Component<L> build();
  }

  private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
  private int mId = sIdGenerator.getAndIncrement();
  private String mGlobalKey;
  private String mKey;

  private final L mLifecycle;
  private @ThreadConfined(ThreadConfined.ANY) ComponentContext mScopedContext;

  private boolean mIsLayoutStarted = false;

  // If we have a cachedLayout, onPrepare and onMeasure would have been called on it already.
  private @ThreadConfined(ThreadConfined.ANY) InternalNode mLastMeasuredLayout;

  abstract public String getSimpleName();

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
    mKey = key;
  }

  Component<L> makeCopyWithNullContext() {
    try {
      Component<L> component = (Component<L>) super.clone();
      component.mScopedContext = null;
      return component;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public Component<L> makeShallowCopy() {
    try {
      Component<L> component = (Component<L>) super.clone();
      component.mIsLayoutStarted = false;

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

  void releaseCachedLayout() {
    if (mLastMeasuredLayout != null) {
      LayoutState.releaseNodeTree(mLastMeasuredLayout, true /* isNestedTree */);
      mLastMeasuredLayout = null;
    }
  }

  void clearCachedLayout() {
    mLastMeasuredLayout = null;
  }

  void release() {
    mIsLayoutStarted = false;
  }

  protected Component(L lifecycle) {
    mLifecycle = lifecycle;
    mKey = Integer.toString(mLifecycle.getId());
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

