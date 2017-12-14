/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.support.annotation.Dimension.DP;
import static com.facebook.litho.FrameworkLogEvents.EVENT_WARNING;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MESSAGE;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.reference.DrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

/**
 * Represents a unique instance of a component. To create new {@link Component} instances, use the
 * {@code create()} method in the generated subclass which returns a builder that allows you to set
 * values for individual props. {@link Component} instances are immutable after creation.
 */
public abstract class Component extends ComponentLifecycle
    implements Cloneable, HasEventDispatcher, HasEventTrigger, ComponentLayout {

  private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
  private int mId = sIdGenerator.getAndIncrement();
  private String mGlobalKey;
  @Nullable private String mKey;
  private boolean mHasManualKey;

  @ThreadConfined(ThreadConfined.ANY)
  private ComponentContext mScopedContext;

  private boolean mIsLayoutStarted = false;

  // If we have a cachedLayout, onPrepare and onMeasure would have been called on it already.
  @ThreadConfined(ThreadConfined.ANY)
  private InternalNode mLastMeasuredLayout;

  @Nullable private CommonProps mCommonProps;

  /**
   * Holds onto how many direct component children of each type this Component has. Used for
   * automatically generating unique global keys for all sibling components of the same type.
   */
  @Nullable private Map<String, Integer> mChildCounters;

  // Keep hold of the layout that we resolved during will render in order to use it again in
  // createLayout.
  @Nullable ActualComponentLayout mLayoutCreatedInWillRender;

  protected Component() {
    this(null);
  }

  /**
   * This constructor should be called only if working with a manually crafted "special" Component.
   * This should NOT be used in general use cases. Use the standard {@link #Component()} instead.
   */
  protected Component(Class classType) {
    super(classType);
    if (!ComponentsConfiguration.lazyInitializeComponent) {
    mChildCounters = new HashMap<>();
      mKey = Integer.toString(getTypeId());
    }
  }

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
  public boolean isEquivalentTo(Component other) {
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
    if (mKey == null && !mHasManualKey) {
      mKey = Integer.toString(getTypeId());
    }
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
    if (keyHandler == null) {
      return childKey;
    }

    /** If the key is already unique, return it. */
    if (!keyHandler.hasKey(childKey)) {
      return childKey;
    }

    /** The component has a manual key set on it but that key is a duplicate * */
    if (component.mHasManualKey) {
      final ComponentsLogger logger = mScopedContext.getLogger();
      if (logger != null) {
        final LogEvent event = logger.newEvent(EVENT_WARNING);
        event.addParam(
            PARAM_MESSAGE,
            "The manual key "
                + key
                + " you are setting on this "
                + component.getSimpleName()
                + " is a duplicate and will be changed into a unique one. "
                + "This will result in unexpected behavior if you don't change it.");
        logger.log(event);
      }
    }

    final String childType = component.getSimpleName();

    if (mChildCounters == null) {
      mChildCounters = new HashMap<>();
    }

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

  Component makeCopyWithNullContext() {
    try {
      final Component component = (Component) super.clone();
      component.mScopedContext = null;
      return component;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public Component makeShallowCopy() {
    try {
      final Component component = (Component) super.clone();
      component.mIsLayoutStarted = false;
      if (!ComponentsConfiguration.lazyInitializeComponent) {
        component.mChildCounters = new HashMap<>();
      }
      component.mHasManualKey = false;

      return component;
    } catch (CloneNotSupportedException e) {
      // This class implements Cloneable, so this is impossible
      throw new RuntimeException(e);
    }
  }

  Component makeShallowCopyWithNewId() {
    final Component component = makeShallowCopy();
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

  protected void copyInterStageImpl(Component component) {

  }

  static boolean isHostSpec(Component component) {
    return (component instanceof HostComponent);
  }

  static boolean isLayoutSpec(Component component) {
    return (component != null && component.getMountType() == MountType.NONE);
  }

  static boolean isMountSpec(Component component) {
    return (component != null && component.getMountType() != MountType.NONE);
  }

  static boolean isMountDrawableSpec(Component component) {
    return (component != null && component.getMountType() == MountType.DRAWABLE);
  }

  static boolean isMountViewSpec(Component component) {
    return (component != null && component.getMountType() == MountType.VIEW);
  }

  static boolean isLayoutSpecWithSizeSpec(Component component) {
    return (isLayoutSpec(component) && component.canMeasure());
  }

  static boolean isNestedTree(Component component) {
    return (isLayoutSpecWithSizeSpec(component)
        || (component != null && component.hasCachedLayout()));
  }

  /**
   * @return whether the given component will render because it returns non-null from its resolved
   *     onCreateLayout, based on its current props and state. Returns true if the resolved layout
   *     is non-null, otherwise false.
   */
  public static boolean willRender(ComponentContext c, Component component) {
    if (component == null) {
      return false;
    }

    component.mLayoutCreatedInWillRender = Layout.create(c, component).build();
    return willRender(component.mLayoutCreatedInWillRender);
  }

  public static boolean willRender(ComponentContext c, ComponentLayout componentLayout) {
    return componentLayout != null && willRender(c, ((Component) componentLayout));
  }

  private static boolean willRender(ActualComponentLayout componentLayout) {
    if (componentLayout == null || ComponentContext.NULL_LAYOUT.equals(componentLayout)) {
      return false;
    }

    if (componentLayout instanceof InternalNode &&
        ((InternalNode) componentLayout).isNestedTreeHolder()) {
      // Components using @OnCreateLayoutWithSizeSpec are lazily resolved after the rest of the tree
      // has been measured (so that we have the proper measurements to pass in). This means we can't
      // eagerly check the result of OnCreateLayoutWithSizeSpec.
      throw new IllegalArgumentException(
          "Cannot check willRender on a component that uses @OnCreateLayoutWithSizeSpec! "
              + "Try wrapping this component in one that uses @OnCreateLayout if possible.");
    }

    return true;
  }

  void generateKey(ComponentContext c) {
    if (ComponentsConfiguration.useGlobalKeys) {
      final Component parentScope = c.getComponentScope();
      final String key = getKey();
      setGlobalKey(
          parentScope == null ? key : parentScope.generateUniqueGlobalKeyForChild(this, key));
    }
  }

  /**
   * Prepares a component for calling any pending state updates on it by setting the TreeProps which
   * the component requires from its parent, setting a scoped component context and applies the
   * pending state updates.
   *
   * @param c component context
   */
  void applyStateUpdates(ComponentContext c) {
    setScopedContext(ComponentContext.withComponentScope(c, this));

    populateTreeProps(this, getScopedContext().getTreeProps());

    if (ComponentsConfiguration.useGlobalKeys) {
      final KeyHandler keyHandler = getScopedContext().getKeyHandler();
      /** This is for testing, the keyHandler should never be null here otherwise. */
      if (keyHandler != null && !ComponentsConfiguration.isEndToEndTestRun) {
        keyHandler.registerKey(this);
      }
    }

    if (hasState()) {
      c.getStateHandler().applyStateUpdatesForComponent(this);
    }
  }

  @Override
  public void recordEventTrigger(EventTriggersContainer container) {
    // Do nothing by default
  }

  CommonProps getCommonProps() {
    return mCommonProps;
  }

  private CommonProps getOrCreateCommonProps() {
    if (mCommonProps == null) {
      mCommonProps = new CommonProps();
    }

    return mCommonProps;
  }

  @Deprecated
  @Override
  public EventDispatcher getEventDispatcher() {
    return this;
  }

  static boolean isInternalComponent(Component component) {
    return component instanceof Column
        || component instanceof Row
        || component instanceof Wrapper
        || component instanceof ColumnReverse
        || component instanceof RowReverse;
  }

  /**
   * @param <T> the type of this builder. Required to ensure methods defined here in the abstract
   *     class correctly return the type of the concrete subclass.
   */
  public abstract static class Builder<T extends Builder<T>> extends ResourceResolver
      implements ComponentLayout.Builder {

    private ComponentContext mContext;
    @AttrRes private int mDefStyleAttr;
    @StyleRes private int mDefStyleRes;
    private Component mComponent;

    protected void init(
        ComponentContext c,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        Component component) {
      super.init(c, c.getResourceCache());

      mComponent = component;
      mContext = c;
      mDefStyleAttr = defStyleAttr;
      mDefStyleRes = defStyleRes;

      if (defStyleAttr != 0 || defStyleRes != 0) {
        mComponent.getOrCreateCommonProps().setStyle(defStyleAttr, defStyleRes);
        component.loadStyle(c, defStyleAttr, defStyleRes, component);
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

    public final Component buildWithLayout() {
      return this.build();
    }

    @ReturnsOwnership
    public abstract Component build();

    public T layoutDirection(YogaDirection layoutDirection) {
      mComponent.getOrCreateCommonProps().layoutDirection(layoutDirection);
      return getThis();
    }

    public T alignSelf(YogaAlign alignSelf) {
      mComponent.getOrCreateCommonProps().alignSelf(alignSelf);
      return getThis();
    }

    public T positionType(YogaPositionType positionType) {
      mComponent.getOrCreateCommonProps().positionType(positionType);
      return getThis();
    }

    public T flex(float flex) {
      mComponent.getOrCreateCommonProps().flex(flex);
      return getThis();
    }

    public T flexGrow(float flexGrow) {
      mComponent.getOrCreateCommonProps().flexGrow(flexGrow);
      return getThis();
    }

    public T flexShrink(float flexShrink) {
      mComponent.getOrCreateCommonProps().flexShrink(flexShrink);
      return getThis();
    }

    public T flexBasisPx(@Px int flexBasis) {
      mComponent.getOrCreateCommonProps().flexBasisPx(flexBasis);
      return getThis();
    }

    public T flexBasisPercent(float percent) {
      mComponent.getOrCreateCommonProps().flexBasisPercent(percent);
      return getThis();
    }

    public T flexBasisAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return flexBasisPx(resolveDimenSizeAttr(resId, defaultResId));
    }

    public T flexBasisAttr(@AttrRes int resId) {
      return flexBasisAttr(resId, 0);
    }

    public T flexBasisRes(@DimenRes int resId) {
      return flexBasisPx(resolveDimenSizeRes(resId));
    }

    public T flexBasisDip(@Dimension(unit = DP) float flexBasis) {
      return flexBasisPx(dipsToPixels(flexBasis));
    }

    public T importantForAccessibility(int importantForAccessibility) {
      mComponent.getOrCreateCommonProps().importantForAccessibility(importantForAccessibility);
      return getThis();
    }

    public T duplicateParentState(boolean duplicateParentState) {
      mComponent.getOrCreateCommonProps().duplicateParentState(duplicateParentState);
      return getThis();
    }

    public T marginPx(YogaEdge edge, @Px int margin) {
      mComponent.getOrCreateCommonProps().marginPx(edge, margin);
      return getThis();
    }

    public T marginPercent(YogaEdge edge, float percent) {
      mComponent.getOrCreateCommonProps().marginPercent(edge, percent);
      return getThis();
    }

    public T marginAuto(YogaEdge edge) {
      mComponent.getOrCreateCommonProps().marginAuto(edge);
      return getThis();
    }

    public T marginAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      return marginPx(edge, resolveDimenSizeAttr(resId, defaultResId));
    }

    public T marginAttr(YogaEdge edge, @AttrRes int resId) {
      return marginAttr(edge, resId, 0);
    }

    public T marginRes(YogaEdge edge, @DimenRes int resId) {
      return marginPx(edge, resolveDimenSizeRes(resId));
    }

    public T marginDip(YogaEdge edge, @Dimension(unit = DP) float margin) {
      return marginPx(edge, dipsToPixels(margin));
    }

    public T paddingPx(YogaEdge edge, @Px int padding) {
      mComponent.getOrCreateCommonProps().paddingPx(edge, padding);
      return getThis();
    }

    public T paddingPercent(YogaEdge edge, float percent) {
      mComponent.getOrCreateCommonProps().paddingPercent(edge, percent);
      return getThis();
    }

    public T paddingAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      return paddingPx(edge, resolveDimenSizeAttr(resId, defaultResId));
    }

    public T paddingAttr(YogaEdge edge, @AttrRes int resId) {
      return paddingAttr(edge, resId, 0);
    }

    public T paddingRes(YogaEdge edge, @DimenRes int resId) {
      return paddingPx(edge, resolveDimenSizeRes(resId));
    }

    public T paddingDip(YogaEdge edge, @Dimension(unit = DP) float padding) {
      return paddingPx(edge, dipsToPixels(padding));
    }

    public T border(Border border) {
      mComponent.getOrCreateCommonProps().border(border);
      return getThis();
    }

    public T positionPx(YogaEdge edge, @Px int position) {
      mComponent.getOrCreateCommonProps().positionPx(edge, position);
      return getThis();
    }

    public T positionPercent(YogaEdge edge, float percent) {
      mComponent.getOrCreateCommonProps().positionPercent(edge, percent);
      return getThis();
    }

    public T positionAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      return positionPx(edge, resolveDimenSizeAttr(resId, defaultResId));
    }

    public T positionAttr(YogaEdge edge, @AttrRes int resId) {
      return positionAttr(edge, resId, 0);
    }

    public T positionRes(YogaEdge edge, @DimenRes int resId) {
      return positionPx(edge, resolveDimenSizeRes(resId));
    }

    public T positionDip(YogaEdge edge, @Dimension(unit = DP) float position) {
      return positionPx(edge, dipsToPixels(position));
    }

    public T widthPx(@Px int width) {
      mComponent.getOrCreateCommonProps().widthPx(width);
      return getThis();
    }

    public T widthPercent(float percent) {
      mComponent.getOrCreateCommonProps().widthPercent(percent);
      return getThis();
    }

    public T widthRes(@DimenRes int resId) {
      return widthPx(resolveDimenSizeRes(resId));
    }

    public T widthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return widthPx(resolveDimenSizeAttr(resId, defaultResId));
    }

    public T widthAttr(@AttrRes int resId) {
      return widthAttr(resId, 0);
    }

    public T widthDip(@Dimension(unit = DP) float width) {
      return widthPx(dipsToPixels(width));
    }

    public T minWidthPx(@Px int minWidth) {
      mComponent.getOrCreateCommonProps().minWidthPx(minWidth);
      return getThis();
    }

    public T minWidthPercent(float percent) {
      mComponent.getOrCreateCommonProps().minWidthPercent(percent);
      return getThis();
    }

    public T minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return minWidthPx(resolveDimenSizeAttr(resId, defaultResId));
    }

    public T minWidthAttr(@AttrRes int resId) {
      return minWidthAttr(resId, 0);
    }

    public T minWidthRes(@DimenRes int resId) {
      return minWidthPx(resolveDimenSizeRes(resId));
    }

    public T minWidthDip(@Dimension(unit = DP) float minWidth) {
      return minWidthPx(dipsToPixels(minWidth));
    }

    public T maxWidthPx(@Px int maxWidth) {
      mComponent.getOrCreateCommonProps().maxWidthPx(maxWidth);
      return getThis();
    }

    public T maxWidthPercent(float percent) {
      mComponent.getOrCreateCommonProps().maxWidthPercent(percent);
      return getThis();
    }

    public T maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return maxWidthPx(resolveDimenSizeAttr(resId, defaultResId));
    }

    public T maxWidthAttr(@AttrRes int resId) {
      return maxWidthAttr(resId, 0);
    }

    public T maxWidthRes(@DimenRes int resId) {
      return maxWidthPx(resolveDimenSizeRes(resId));
    }

    public T maxWidthDip(@Dimension(unit = DP) float maxWidth) {
      return maxWidthPx(dipsToPixels(maxWidth));
    }

    public T heightPx(@Px int height) {
      mComponent.getOrCreateCommonProps().heightPx(height);
      return getThis();
    }

    public T heightPercent(float percent) {
      mComponent.getOrCreateCommonProps().heightPercent(percent);
      return getThis();
    }

    public T heightRes(@DimenRes int resId) {
      return heightPx(resolveDimenSizeRes(resId));
    }

    public T heightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return heightPx(resolveDimenSizeAttr(resId, defaultResId));
    }

    public T heightAttr(@AttrRes int resId) {
      return heightAttr(resId, 0);
    }

    public T heightDip(@Dimension(unit = DP) float height) {
      return heightPx(dipsToPixels(height));
    }

    public T minHeightPx(@Px int minHeight) {
      mComponent.getOrCreateCommonProps().minHeightPx(minHeight);
      return getThis();
    }

    public T minHeightPercent(float percent) {
      mComponent.getOrCreateCommonProps().minHeightPercent(percent);
      return getThis();
    }

    public T minHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return minHeightPx(resolveDimenSizeAttr(resId, defaultResId));
    }

    public T minHeightAttr(@AttrRes int resId) {
      return minHeightAttr(resId, 0);
    }

    public T minHeightRes(@DimenRes int resId) {
      return minHeightPx(resolveDimenSizeRes(resId));
    }

    public T minHeightDip(@Dimension(unit = DP) float minHeight) {
      return minHeightPx(dipsToPixels(minHeight));
    }

    public T maxHeightPx(@Px int maxHeight) {
      mComponent.getOrCreateCommonProps().maxHeightPx(maxHeight);
      return getThis();
    }

    public T maxHeightPercent(float percent) {
      mComponent.getOrCreateCommonProps().maxHeightPercent(percent);
      return getThis();
    }

    public T maxHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return maxHeightPx(resolveDimenSizeAttr(resId, defaultResId));
    }

    public T maxHeightAttr(@AttrRes int resId) {
      return maxHeightAttr(resId, 0);
    }

    public T maxHeightRes(@DimenRes int resId) {
      return maxHeightPx(resolveDimenSizeRes(resId));
    }

    public T maxHeightDip(@Dimension(unit = DP) float maxHeight) {
      return maxHeightPx(dipsToPixels(maxHeight));
    }

    public T aspectRatio(float aspectRatio) {
      mComponent.getOrCreateCommonProps().aspectRatio(aspectRatio);
      return getThis();
    }

    public T touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
      mComponent.getOrCreateCommonProps().touchExpansionPx(edge, touchExpansion);
      return getThis();
    }

    public T touchExpansionAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      return touchExpansionPx(edge, resolveDimenSizeAttr(resId, defaultResId));
    }

    public T touchExpansionAttr(YogaEdge edge, @AttrRes int resId) {
      return touchExpansionAttr(edge, resId, 0);
    }

    public T touchExpansionRes(YogaEdge edge, @DimenRes int resId) {
      return touchExpansionPx(edge, resolveDimenSizeRes(resId));
    }

    public T touchExpansionDip(YogaEdge edge, @Dimension(unit = DP) float touchExpansion) {
      return touchExpansionPx(edge, dipsToPixels(touchExpansion));
    }

    public T background(Reference<? extends Drawable> background) {
      mComponent.getOrCreateCommonProps().background(background);
      return getThis();
    }

    public T background(Reference.Builder<? extends Drawable> builder) {
      return background(builder.build());
    }

    public T background(Drawable background) {
      return background(DrawableReference.create().drawable(background));
    }

    public T backgroundAttr(@AttrRes int resId, @DrawableRes int defaultResId) {
      return backgroundRes(resolveResIdAttr(resId, defaultResId));
    }

    public T backgroundAttr(@AttrRes int resId) {
      return backgroundAttr(resId, 0);
    }

    public T backgroundRes(@DrawableRes int resId) {
      if (resId == 0) {
        return background((Reference<? extends Drawable>) null);
      }

      return background(mContext.getResources().getDrawable(resId));
    }

    public T backgroundColor(@ColorInt int backgroundColor) {
      return background(new ColorDrawable(backgroundColor));
    }

    public T foreground(Drawable foreground) {
      mComponent.getOrCreateCommonProps().foreground(foreground);
      return getThis();
    }

    public T foregroundAttr(@AttrRes int resId, @DrawableRes int defaultResId) {
      return foregroundRes(resolveResIdAttr(resId, defaultResId));
    }

    public T foregroundAttr(@AttrRes int resId) {
      return foregroundAttr(resId, 0);
    }

    public T foregroundRes(@DrawableRes int resId) {
      if (resId == 0) {
        return foreground(null);
      }

      return foreground(mContext.getResources().getDrawable(resId));
    }

    public T foregroundColor(@ColorInt int foregroundColor) {
      return foreground(new ColorDrawable(foregroundColor));
    }

    public T wrapInView() {
      mComponent.getOrCreateCommonProps().wrapInView();
      return getThis();
    }

    public T clickHandler(EventHandler<ClickEvent> clickHandler) {
      mComponent.getOrCreateCommonProps().clickHandler(clickHandler);
      return getThis();
    }

    public T longClickHandler(EventHandler<LongClickEvent> longClickHandler) {
      mComponent.getOrCreateCommonProps().longClickHandler(longClickHandler);
      return getThis();
    }

    public T focusChangeHandler(EventHandler<FocusChangedEvent> focusChangeHandler) {
      mComponent.getOrCreateCommonProps().focusChangeHandler(focusChangeHandler);
      return getThis();
    }

    public T touchHandler(EventHandler<TouchEvent> touchHandler) {
      mComponent.getOrCreateCommonProps().touchHandler(touchHandler);
      return getThis();
    }

    public T interceptTouchHandler(EventHandler<InterceptTouchEvent> interceptTouchHandler) {
      mComponent.getOrCreateCommonProps().interceptTouchHandler(interceptTouchHandler);
      return getThis();
    }

    public T focusable(boolean isFocusable) {
      mComponent.getOrCreateCommonProps().focusable(isFocusable);
      return getThis();
    }

    public T enabled(boolean isEnabled) {
      mComponent.getOrCreateCommonProps().enabled(isEnabled);
      return getThis();
    }

    public T visibleHeightRatio(float visibleHeightRatio) {
      mComponent.getOrCreateCommonProps().visibleHeightRatio(visibleHeightRatio);
      return getThis();
    }

    public T visibleWidthRatio(float visibleWidthRatio) {
      mComponent.getOrCreateCommonProps().visibleWidthRatio(visibleWidthRatio);
      return getThis();
    }

    public T visibleHandler(EventHandler<VisibleEvent> visibleHandler) {
      mComponent.getOrCreateCommonProps().visibleHandler(visibleHandler);
      return getThis();
    }

    public T focusedHandler(EventHandler<FocusedVisibleEvent> focusedHandler) {
      mComponent.getOrCreateCommonProps().focusedHandler(focusedHandler);
      return getThis();
    }

    public T unfocusedHandler(EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
      mComponent.getOrCreateCommonProps().unfocusedHandler(unfocusedHandler);
      return getThis();
    }

    public T fullImpressionHandler(EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
      mComponent.getOrCreateCommonProps().fullImpressionHandler(fullImpressionHandler);
      return getThis();
    }

    public T invisibleHandler(EventHandler<InvisibleEvent> invisibleHandler) {
      mComponent.getOrCreateCommonProps().invisibleHandler(invisibleHandler);
      return getThis();
    }

    public T contentDescription(CharSequence contentDescription) {
      mComponent.getOrCreateCommonProps().contentDescription(contentDescription);
      return getThis();
    }

    public T contentDescription(@StringRes int stringId) {
      return contentDescription(mContext.getResources().getString(stringId));
    }

    public T contentDescription(@StringRes int stringId, Object... formatArgs) {
      return contentDescription(mContext.getResources().getString(stringId, formatArgs));
    }

    public T viewTag(Object viewTag) {
      mComponent.getOrCreateCommonProps().viewTag(viewTag);
      return getThis();
    }

    public T viewTags(SparseArray<Object> viewTags) {
      mComponent.getOrCreateCommonProps().viewTags(viewTags);
      return getThis();
    }

    public T shadowElevationPx(float shadowElevation) {
      mComponent.getOrCreateCommonProps().shadowElevationPx(shadowElevation);
      return getThis();
    }

    public T shadowElevationAttr(@AttrRes int resId, @DimenRes int defaultResId) {
      return shadowElevationPx(resolveDimenSizeAttr(resId, defaultResId));
    }

    public T shadowElevationAttr(@AttrRes int resId) {
      return shadowElevationAttr(resId, 0);
    }

    public T shadowElevationRes(@DimenRes int resId) {
      return shadowElevationPx(resolveDimenSizeRes(resId));
    }

    public T shadowElevationDip(@Dimension(unit = DP) float shadowElevation) {
      return shadowElevationPx(dipsToPixels(shadowElevation));
    }

    public T outlineProvider(ViewOutlineProvider outlineProvider) {
      mComponent.getOrCreateCommonProps().outlineProvider(outlineProvider);
      return getThis();
    }

    public T clipToOutline(boolean clipToOutline) {
      mComponent.getOrCreateCommonProps().clipToOutline(clipToOutline);
      return getThis();
    }

    public T testKey(String testKey) {
      mComponent.getOrCreateCommonProps().testKey(testKey);
      return getThis();
    }

    public T dispatchPopulateAccessibilityEventHandler(
        EventHandler<DispatchPopulateAccessibilityEventEvent>
            dispatchPopulateAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .dispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
      return getThis();
    }

    public T onInitializeAccessibilityEventHandler(
        EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
      return getThis();
    }

    public T onInitializeAccessibilityNodeInfoHandler(
        EventHandler<OnInitializeAccessibilityNodeInfoEvent>
            onInitializeAccessibilityNodeInfoHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
      return getThis();
    }

    public T onPopulateAccessibilityEventHandler(
        EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
      return getThis();
    }

    public T onRequestSendAccessibilityEventHandler(
        EventHandler<OnRequestSendAccessibilityEventEvent> onRequestSendAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
      return getThis();
    }

    public T performAccessibilityActionHandler(
        EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
      mComponent
          .getOrCreateCommonProps()
          .performAccessibilityActionHandler(performAccessibilityActionHandler);
      return getThis();
    }

    public T sendAccessibilityEventHandler(
        EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
      mComponent
          .getOrCreateCommonProps()
          .sendAccessibilityEventHandler(sendAccessibilityEventHandler);
      return getThis();
    }

    public T sendAccessibilityEventUncheckedHandler(
        EventHandler<SendAccessibilityEventUncheckedEvent> sendAccessibilityEventUncheckedHandler) {
      mComponent
          .getOrCreateCommonProps()
          .sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);
      return getThis();
    }

    public T transitionKey(String key) {
      mComponent.getOrCreateCommonProps().transitionKey(key);
      return getThis();
    }

    public T alpha(float alpha) {
      mComponent.getOrCreateCommonProps().alpha(alpha);
      return getThis();
    }

    public T scale(float scale) {
      mComponent.getOrCreateCommonProps().scale(scale);
      return getThis();
    }
  }
}
