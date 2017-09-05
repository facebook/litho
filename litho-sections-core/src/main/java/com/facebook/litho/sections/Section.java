/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import android.support.v4.util.Pair;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.ResourceResolver;
import com.facebook.litho.sections.SectionLifecycle.StateContainer;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

/**
 * Represents a unique instance of a {@link Section} that is driven by its matching
 * {@link SectionLifecycle}. To create new {@link Section} instances, use the
 * {@code create()} method in the generated {@link SectionLifecycle} subclass which
 * returns a {@link Builder} that allows you to set values for individual props.
 * {@link Section} instances are immutable after creation.
 */
public abstract class Section<L extends SectionLifecycle> implements Cloneable, HasEventDispatcher {
  private Section mParent;
  private boolean mInvalidated;
  private SectionContext mScopedContext;
  EventHandler<LoadingEvent> loadingEventHandler;

  @Override
  public EventDispatcher getEventDispatcher() {
    return mLifecycle;
  }

  /**
   * A builder to build a Section with a {@link SectionLifecycle} L. Generated lifecycle classes
   * will expose a create() method to access a builder and will add methods to the builder to set
   * all the props defined in the {@link GroupSectionSpec}/ {@link DiffSectionSpec}. By default the
   * builder exposes a method that the parent can use to specify an key for its children. The key
   * should be set every time a parent might have more children with the same {@link
   * SectionLifecycle}.
   */
  public abstract static class Builder<L extends SectionLifecycle, T extends Builder<L, T>>
      extends ResourceResolver {

    private Section mSection;

    protected void init(SectionContext context, Section section) {
      super.init(context, context.getResourceCache());
      mSection = section;
    }

    /** Sets the key of this {@link Section} local to his parent. */
    protected T key(String key) {
      mSection.setKey(key);
      return getThis();
    }

    protected T loadingEventHandler(EventHandler<LoadingEvent> loadingEventHandler) {
      mSection.loadingEventHandler = loadingEventHandler;
      return getThis();
    }

    public abstract T getThis();

    /**
     * @return The immutable {@link Section}.
     */
    public abstract Section build();

    @Override
    protected void release() {
      super.release();
      mSection = null;
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
  }

  private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
  private final int mId = sIdGenerator.getAndIncrement();

  // The total count of leaf Components this subtree added to the global list.
  private int mCount;
  private List<Section> mChildren;
  private final L mLifecycle;
  private String mGlobalKey;
  private String mKey;

  protected Section(L lifecycle) {
    mLifecycle = lifecycle;

    mKey = mLifecycle.getLogTag();
  }

  /**
   * @return true is this {@link Section} can produce a changeSet, meaning it's a {@link Section}
   * whose {@link SectionLifecycle} implements
   * {@link OnDiff}.
   */
  boolean isDiffSectionSpec() {
    return mLifecycle.isDiffSectionSpec();
  }

  /**
   * @return a unique key for this {@link Section} within its tree.
   */
  String getGlobalKey() {
    return mGlobalKey;
  }

  /**
   * Set a unique key for this {@link Section} within its tree.
   */
  void setGlobalKey(String key) {
    mGlobalKey = key;
  }

  /**
   * @return a key for this {@link Section} that is local between its siblings. A parent is
   * responsible to set different localScopes to children with the same
   * {@link SectionLifecycle}.
   */
  String getKey() {
    return mKey;
  }

  /**
   * Sets the key for this Component. This is only used for testing as the key will be set from the
   * {@link Builder}
   */
  void setKey(String key) {
    mKey = key;
  }

  /**
   * @return te total number of {@link com.facebook.litho.Component} that the subtree of
   * {@link Section}s having its root in this {@link Section} generated.
   */
  int getCount() {
    return mCount;
  }

  /**
   * Sets te total number of {@link com.facebook.litho.Component} that the subtree of
   * {@link Section}s having its root in this {@link Section} generated.
   */
  void setCount(int count) {
    mCount = count;
  }

  /**
   * @return the direct children of this {@link Section}.
   */
  List<Section> getChildren() {
    return mChildren;
  }

  /**
   * @return the parent of this {@link Section} in the tree.
   */
  Section getParent() {
    return mParent;
  }

  /**
   * Sets the parent of this {@link Section} in the tree.
   */
  void setParent(Section parent) {
    mParent = parent;
  }

  /**
   * @return the {@link SectionLifecycle} for this {@link Section}.
   */
  public final L getLifecycle() {
    return mLifecycle;
  }

  /**
   * Invalidates The subtree having its root in this {@link Section}. When a subtree is invalidated,
   * the {@link OnDiff} will be invoked
   * regardless of whether the {@link com.facebook.litho.annotations.Prop}s changed or not.
   */
  void invalidate() {
    invalidateInternal(this);
  }

  private static void invalidateInternal(Section section) {
    section.setInvalidated(true);
    if (section.getParent() != null) {
      invalidateInternal(section.getParent());
    }
  }

  /**
   * @return true if this LisComponent or any of its children were invalidated.
   */
  boolean isInvalidated() {
    return mInvalidated;
  }

  void setInvalidated(boolean invalidated) {
    mInvalidated = invalidated;
  }

  /**
   * @return a clone of this {@link Section}.
   * if deepCopy is false the clone won't contain any children or count as it will
   * be returned in a pre - ChangeSet generation state.
   */
  public Section<L> makeShallowCopy(boolean deepCopy) {
    try {
      final Section<L> clone = (Section<L>) super.clone();

      if (!deepCopy) {
        if (clone.mChildren != null) {
          clone.mChildren = new ArrayList<>();
        }
        clone.mCount = 0;
        clone.setInvalidated(false);
      }

      return clone;
    } catch (CloneNotSupportedException e) {
      // Subclasses implement Cloneable, so this is impossible
      throw new RuntimeException(e);
    }
  }

  public Section<L> makeShallowCopy() {
    return makeShallowCopy(false);
  }

  int getId() {
    return mId;
  }

  SectionContext getScopedContext() {
    return mScopedContext;
  }

  void setScopedContext(SectionContext scopedContext) {
    mScopedContext = scopedContext;
  }

  void setChildren(Children children) {
    mChildren = children == null ? new ArrayList<Section>() : children.getChildren();
  }

  /**
   * Mostly used by logging to provide more readable messages.
   */
  public abstract String getSimpleName();

  /**
   * Compares this component to a different one to check if they are the same
   *
   * <p>This is used to be able to skip rendering a component again. We avoid using the {@link
   * Section#equals(Object)} so we can optimize the code better over time since we don't have to
   * adhere to the contract required for a equals method.
   *
   * @param other the component to compare to
   * @return true if the components are of the same type and have the same props
   */
  public boolean isEquivalentTo(Section<?> other) {
    return this.equals(other);
  }

  protected StateContainer getStateContainer() {
    return null;
  }

  /**
   * Called when this {@link Section} is not in use anymore to release its resources.
   */
  void release() {
    //TODO release list into a pool t11953296
  }

  static Map<String, Pair<Section, Integer>> acquireChildrenMap(
      @Nullable Section currentComponent) {
    //TODO use pools instead t11953296
    final HashMap<String, Pair<Section, Integer>> childrenMap = new HashMap<>();
    if (currentComponent == null) {
      return childrenMap;
    }

    final List<Section> children = currentComponent.getChildren();
    for (int i = 0, size = children.size(); i < size; i++) {
      final Section child = children.get(i);
      childrenMap.put(child.getGlobalKey(), new Pair<Section, Integer>(child, i));
    }

    return childrenMap;
  }

  static void releaseChildrenMap(Map<String, Pair<Section, Integer>> newChildren) {
    //TODO use pools t11953296
  }
}
