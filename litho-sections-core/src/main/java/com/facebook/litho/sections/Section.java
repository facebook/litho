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

package com.facebook.litho.sections;

import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;
import com.facebook.litho.Equivalence;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.EventTriggersContainer;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.HasEventTrigger;
import com.facebook.litho.ResourceResolver;
import com.facebook.litho.StateContainer;
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
 * Represents a unique instance of a {@link Section} that is driven by its matching {@link
 * SectionLifecycle}. To create new {@link Section} instances, use the {@code create()} method in
 * the generated {@link SectionLifecycle} subclass which returns a {@link Builder} that allows you
 * to set values for individual props. {@link Section} instances are immutable after creation.
 */
public abstract class Section extends SectionLifecycle
    implements Cloneable, HasEventDispatcher, HasEventTrigger, Equivalence<Section> {

  private Section mParent;
  private boolean mInvalidated;
  private SectionContext mScopedContext;
  EventHandler<LoadingEvent> loadingEventHandler;

  /**
   * Holds onto how many direct section children of each type this Section has. Used for
   * automatically generating unique global keys for all sibling sections of the same type.
   */
  @Nullable private Map<String, Integer> mChildCounters;

  /** Simple name to identify the generated section. */
  private final String mSimpleName;

  protected Section(String simpleName) {
    mSimpleName = simpleName;
    mKey = getLogTag();
  }

  @Override
  public EventDispatcher getEventDispatcher() {
    return this;
  }

  @Override
  public void recordEventTrigger(EventTriggersContainer container) {
    // Do nothing by default
  }

  /**
   * A builder to build a Section with a {@link SectionLifecycle} L. Generated lifecycle classes
   * will expose a create() method to access a builder and will add methods to the builder to set
   * all the props defined in the {@link GroupSectionSpec}/ {@link DiffSectionSpec}. By default the
   * builder exposes a method that the parent can use to specify an key for its children. The key
   * should be set every time a parent might have more children with the same {@link
   * SectionLifecycle}.
   */
  public abstract static class Builder<T extends Builder<T>> {

    private Section mSection;
    protected ResourceResolver mResourceResolver;

    protected void init(SectionContext context, Section section) {
      mSection = section;
      mResourceResolver = new ResourceResolver(context);
    }

    /** Sets the key of this {@link Section} local to its parent. */
    public T key(String key) {
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

    protected void release() {
      mSection = null;
      mResourceResolver.release();
      mResourceResolver = null;
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
  private String mGlobalKey;
  private String mKey;

  /** @return a unique key for this {@link Section} within its tree. */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public String getGlobalKey() {
    return mGlobalKey;
  }

  /** Set a unique key for this {@link Section} within its tree. */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setGlobalKey(String key) {
    mGlobalKey = key;
  }

  /**
   * @return a key for this {@link Section} that is local between its siblings. A parent is
   *     responsible to set different localScopes to children with the same {@link
   *     SectionLifecycle}.
   */
  String getKey() {
    return mKey;
  }

  /**
   * Sets the key for this Section. This is only used for testing as the key will be set from the
   * {@link Builder}
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setKey(String key) {
    mKey = key;
  }

  /**
   * @return te total number of {@link com.facebook.litho.Component} that the subtree of {@link
   *     Section}s having its root in this {@link Section} generated.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  int getCount() {
    return mCount;
  }

  /**
   * Sets the total number of {@link com.facebook.litho.Component} that the subtree of {@link
   * Section}s having its root in this {@link Section} generated.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setCount(int count) {
    mCount = count;
  }

  /** @return the direct children of this {@link Section}. */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public List<Section> getChildren() {
    return mChildren;
  }

  /** @return the parent of this {@link Section} in the tree. */
  public Section getParent() {
    return mParent;
  }

  /**
   * Sets the parent of this {@link Section} in the tree.
   */
  void setParent(Section parent) {
    mParent = parent;
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

  /** @return true if this Section or any of its children were invalidated. */
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
  public Section makeShallowCopy(boolean deepCopy) {
    try {
      final Section clone = (Section) super.clone();

      if (!deepCopy) {
        if (clone.mChildren != null) {
          clone.mChildren = new ArrayList<>();
        }
        clone.mCount = 0;
        clone.setInvalidated(false);
      }

      return clone;
    } catch (CloneNotSupportedException e) {
      // This class implements Cloneable, so this is impossible
      throw new RuntimeException(e);
    }
  }

  public Section makeShallowCopy() {
    return makeShallowCopy(false);
  }

  int getId() {
    return mId;
  }

  public SectionContext getScopedContext() {
    return mScopedContext;
  }

  @VisibleForTesting
  public void setScopedContext(SectionContext scopedContext) {
    mScopedContext = scopedContext;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setChildren(Children children) {
    mChildren = children == null ? new ArrayList<Section>() : children.getChildren();
  }

  /** Mostly used by logging to provide more readable messages. */
  public final String getSimpleName() {
    return mSimpleName;
  }

  /**
   * Compares this section to a different one to check if they are the same
   *
   * <p>This is used to be able to skip rendering a section again. We avoid using the {@link
   * Section#equals(Object)} so we can optimize the code better over time since we don't have to
   * adhere to the contract required for a equals method.
   *
   * @param other the component to compare to
   * @return true if the components are of the same type and have the same props
   */
  @Override
  public boolean isEquivalentTo(Section other) {
    return this.equals(other);
  }

  @Nullable
  protected StateContainer getStateContainer() {
    return null;
  }

  /**
   * Called when this {@link Section} is not in use anymore to release its resources.
   */
  void release() {
    //TODO release list into a pool t11953296
  }

  void generateKeyAndSet(SectionContext c, String globalKey) {
    final Section parentScope = c.getSectionScope();
    final String uniqueGlobalKey =
        parentScope == null
            ? globalKey
            : parentScope.generateUniqueGlobalKeyForChild(this, globalKey);
    setGlobalKey(uniqueGlobalKey);

    c.getKeyHandler().registerKey(uniqueGlobalKey);
  }

  private String generateUniqueGlobalKeyForChild(Section section, String childKey) {
    final KeyHandler keyHandler = mScopedContext.getKeyHandler();

    /** If the key is already unique, return it. */
    if (!keyHandler.hasKey(childKey)) {
      return childKey;
    }

    final String childType = section.getSimpleName();

    if (mChildCounters == null) {
      mChildCounters = new HashMap<>();
    }

    /**
     * If the key is a duplicate, we start appending an index based on the child component's type
     * that would uniquely identify it.
     */
    final int childIndex =
        mChildCounters.containsKey(childType) ? mChildCounters.get(childType) : 0;
    mChildCounters.put(childType, childIndex + 1);

    return childKey + childIndex;
  }

  static Map<String, Pair<Section, Integer>> acquireChildrenMap(
      @Nullable Section currentComponent) {
    //TODO use pools instead t11953296
    final HashMap<String, Pair<Section, Integer>> childrenMap = new HashMap<>();
    if (currentComponent == null) {
      return childrenMap;
    }

    final List<Section> children = currentComponent.getChildren();
    if (children == null) {
      throw new IllegalStateException(
          "Children of current section " + currentComponent + " is null!");
    }

    for (int i = 0, size = children.size(); i < size; i++) {
      final Section child = children.get(i);
      childrenMap.put(child.getGlobalKey(), new Pair<Section, Integer>(child, i));
    }

    return childrenMap;
  }

  static void releaseChildrenMap(Map<String, Pair<Section, Integer>> newChildren) {
    //TODO use pools t11953296
  }

  @VisibleForTesting
  public String getLogTag() {
    return getSimpleName();
  }
}
