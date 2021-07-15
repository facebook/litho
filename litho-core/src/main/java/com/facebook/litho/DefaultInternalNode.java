/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static com.facebook.litho.CommonUtils.addOrCreateList;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.litho.Layout.isLayoutDirectionRTL;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.ENABLED_UNSET;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;

import android.animation.StateListAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.rendercore.Copyable;
import com.facebook.rendercore.RenderState;
import com.facebook.rendercore.RenderUnit;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Default implementation of {@link InternalNode}. */
@OkToExtend
@ThreadConfined(ThreadConfined.ANY)
public class DefaultInternalNode
    implements InternalNode, LithoLayoutResult, YogaNode.Inputs, LayoutProps, Cloneable {

  private static final String CONTEXT_SPECIFIC_STYLE_SET =
      "DefaultInternalNode:ContextSpecificStyleSet";

  // Used to check whether or not the framework can use style IDs for
  // paddingStart/paddingEnd due to a bug in some Android devices.
  private static final boolean SUPPORTS_RTL = (SDK_INT >= JELLY_BEAN_MR1);

  // Flags used to indicate that a certain attribute was explicitly set on the node.
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L;
  private static final long PFLAG_ALIGN_SELF_IS_SET = 1L << 1;
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 2;
  private static final long PFLAG_FLEX_IS_SET = 1L << 3;
  private static final long PFLAG_FLEX_GROW_IS_SET = 1L << 4;
  private static final long PFLAG_FLEX_SHRINK_IS_SET = 1L << 5;
  private static final long PFLAG_FLEX_BASIS_IS_SET = 1L << 6;
  private static final long PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1L << 7;
  protected static final long PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1L << 8;
  private static final long PFLAG_MARGIN_IS_SET = 1L << 9;
  protected static final long PFLAG_PADDING_IS_SET = 1L << 10;
  private static final long PFLAG_POSITION_IS_SET = 1L << 11;
  private static final long PFLAG_WIDTH_IS_SET = 1L << 12;
  private static final long PFLAG_MIN_WIDTH_IS_SET = 1L << 13;
  private static final long PFLAG_MAX_WIDTH_IS_SET = 1L << 14;
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 15;
  private static final long PFLAG_MIN_HEIGHT_IS_SET = 1L << 16;
  private static final long PFLAG_MAX_HEIGHT_IS_SET = 1L << 17;
  protected static final long PFLAG_BACKGROUND_IS_SET = 1L << 18;
  protected static final long PFLAG_FOREGROUND_IS_SET = 1L << 19;
  protected static final long PFLAG_VISIBLE_HANDLER_IS_SET = 1L << 20;
  protected static final long PFLAG_FOCUSED_HANDLER_IS_SET = 1L << 21;
  protected static final long PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1L << 22;
  protected static final long PFLAG_INVISIBLE_HANDLER_IS_SET = 1L << 23;
  protected static final long PFLAG_UNFOCUSED_HANDLER_IS_SET = 1L << 24;
  private static final long PFLAG_TOUCH_EXPANSION_IS_SET = 1L << 25;
  private static final long PFLAG_ASPECT_RATIO_IS_SET = 1L << 26;
  protected static final long PFLAG_TRANSITION_KEY_IS_SET = 1L << 27;
  protected static final long PFLAG_BORDER_IS_SET = 1L << 28;
  protected static final long PFLAG_STATE_LIST_ANIMATOR_SET = 1L << 29;
  protected static final long PFLAG_STATE_LIST_ANIMATOR_RES_SET = 1L << 30;
  protected static final long PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET = 1L << 31;
  protected static final long PFLAG_TRANSITION_KEY_TYPE_IS_SET = 1L << 32;
  protected static final long PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET = 1L << 33;

  private YogaNode mYogaNode;
  private ComponentContext mComponentContext;

  @ThreadConfined(ThreadConfined.ANY)
  private List<Component> mComponents = new ArrayList<>(1);

  @ThreadConfined(ThreadConfined.ANY)
  private List<String> mComponentGlobalKeys = new ArrayList<>(1);

  private @Nullable LithoLayoutResult mParent;

  protected final int[] mBorderColors = new int[Border.EDGE_COUNT];
  protected final float[] mBorderRadius = new float[Border.RADIUS_COUNT];

  private @Nullable DiffNode mDiffNode;
  protected @Nullable NodeInfo mNodeInfo;
  protected @Nullable EventHandler<VisibleEvent> mVisibleHandler;
  protected @Nullable EventHandler<FocusedVisibleEvent> mFocusedHandler;
  protected @Nullable EventHandler<UnfocusedVisibleEvent> mUnfocusedHandler;
  protected @Nullable EventHandler<FullImpressionVisibleEvent> mFullImpressionHandler;
  protected @Nullable EventHandler<InvisibleEvent> mInvisibleHandler;
  protected @Nullable EventHandler<VisibilityChangedEvent> mVisibilityChangedHandler;
  protected @Nullable Drawable mBackground;
  protected @Nullable Drawable mForeground;
  protected @Nullable PathEffect mBorderPathEffect;
  protected @Nullable StateListAnimator mStateListAnimator;
  private @Nullable Edges mTouchExpansion;
  protected @Nullable String mTransitionKey;
  protected @Nullable String mTransitionOwnerKey;
  protected @Nullable Transition.TransitionKeyType mTransitionKeyType;
  private @Nullable ArrayList<Transition> mTransitions;
  private @Nullable Map<String, Component> mComponentsNeedingPreviousRenderData;
  private @Nullable ArrayList<WorkingRangeContainer.Registration> mWorkingRangeRegistrations;
  private @Nullable ArrayList<Attachable> mAttachables;
  protected @Nullable String mTestKey;
  private @Nullable Set<DebugComponent> mDebugComponents;
  private @Nullable List<Component> mUnresolvedComponents;
  protected @Nullable Paint mLayerPaint;

  protected boolean mDuplicateParentState;
  protected boolean mDuplicateChildrenStates;
  protected boolean mForceViewWrapping;
  private boolean mCachedMeasuresValid;

  protected int mLayerType = LayerType.LAYER_TYPE_NOT_SET;
  protected int mImportantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  protected @DrawableRes int mStateListAnimatorRes;

  protected float mVisibleHeightRatio;
  protected float mVisibleWidthRatio;

  private int mLastWidthSpec = DiffNode.UNSPECIFIED;
  private int mLastHeightSpec = DiffNode.UNSPECIFIED;
  private float mLastMeasuredWidth = DiffNode.UNSPECIFIED;
  private float mLastMeasuredHeight = DiffNode.UNSPECIFIED;

  private boolean mIsClone = false;

  protected long mPrivateFlags;

  protected DefaultInternalNode(ComponentContext componentContext) {
    this(componentContext, NodeConfig.createYogaNode());
  }

  protected DefaultInternalNode(ComponentContext componentContext, YogaNode yogaNode) {
    mComponentContext = componentContext;

    mYogaNode = yogaNode;
    mYogaNode.setData(this);

    mDebugComponents = new HashSet<>();
  }

  @Override
  public void addChildAt(InternalNode child, int index) {
    if (child instanceof DefaultInternalNode) {
      mYogaNode.addChildAt(((DefaultInternalNode) child).getYogaNode(), index);
    }
  }

  @Override
  public void addComponentNeedingPreviousRenderData(String globalKey, Component component) {
    if (mComponentsNeedingPreviousRenderData == null) {
      mComponentsNeedingPreviousRenderData = new HashMap<>(1);
    }
    mComponentsNeedingPreviousRenderData.put(globalKey, component);
  }

  @Override
  public void addTransition(Transition transition) {
    if (mTransitions == null) {
      mTransitions = new ArrayList<>(1);
    }
    mTransitions.add(transition);
  }

  @Override
  public void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations) {
    if (mWorkingRangeRegistrations == null) {
      mWorkingRangeRegistrations = new ArrayList<>(registrations.size());
    }
    mWorkingRangeRegistrations.addAll(registrations);
  }

  @Override
  public void addAttachable(Attachable attachable) {
    if (mAttachables == null) {
      mAttachables = new ArrayList<>(4);
    }
    mAttachables.add(attachable);
  }

  @Override
  public @Nullable List<Attachable> getAttachables() {
    return mAttachables;
  }

  @Override
  public InternalNode alignContent(YogaAlign alignContent) {
    mYogaNode.setAlignContent(alignContent);
    return this;
  }

  @Override
  public InternalNode alignItems(YogaAlign alignItems) {
    mYogaNode.setAlignItems(alignItems);
    return this;
  }

  @Override
  public void alignSelf(YogaAlign alignSelf) {
    mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
    mYogaNode.setAlignSelf(alignSelf);
  }

  @Override
  public void appendComponent(Component component, String key) {
    mComponents.add(component);
    mComponentGlobalKeys.add(key);
  }

  @Override
  public void appendUnresolvedComponent(Component component) {
    if (mUnresolvedComponents == null) {
      mUnresolvedComponents = new ArrayList<>();
    }

    mUnresolvedComponents.add(component);
  }

  @Override
  public boolean areCachedMeasuresValid() {
    return mCachedMeasuresValid;
  }

  @Override
  public void setNestedPadding(@Nullable Edges padding, @Nullable boolean[] isPercentage) {
    for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
      float value = padding.getRaw(i);
      if (!YogaConstants.isUndefined(value)) {
        final YogaEdge edge = YogaEdge.fromInt(i);
        if (isPercentage != null && isPercentage[edge.intValue()]) {
          paddingPercent(edge, value);
        } else {
          paddingPx(edge, (int) value);
        }
      }
    }
  }

  @Override
  public LayoutProps getDebugLayoutEditor() {
    return this;
  }

  @Override
  public void aspectRatio(float aspectRatio) {
    mPrivateFlags |= PFLAG_ASPECT_RATIO_IS_SET;
    mYogaNode.setAspectRatio(aspectRatio);
  }

  @Override
  public InternalNode background(@Nullable Drawable background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
    setPaddingFromBackground(background);
    return this;
  }

  @Override
  public InternalNode backgroundColor(@ColorInt int backgroundColor) {
    return background(ComparableColorDrawable.create(backgroundColor));
  }

  @Override
  public InternalNode backgroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return background(null);
    }

    return background(ContextCompat.getDrawable(mComponentContext.getAndroidContext(), resId));
  }

  @Override
  public InternalNode border(Border border) {
    border(border.mEdgeWidths, border.mEdgeColors, border.mRadius, border.mPathEffect);
    return this;
  }

  @Override
  public void border(int[] widths, int[] colors, float[] radii, @Nullable PathEffect effect) {
    mPrivateFlags |= PFLAG_BORDER_IS_SET;
    setBorderWidth(LEFT, widths[Border.EDGE_LEFT]);
    setBorderWidth(TOP, widths[Border.EDGE_TOP]);
    setBorderWidth(RIGHT, widths[Border.EDGE_RIGHT]);
    setBorderWidth(BOTTOM, widths[Border.EDGE_BOTTOM]);
    System.arraycopy(colors, 0, mBorderColors, 0, colors.length);
    System.arraycopy(radii, 0, mBorderRadius, 0, radii.length);
    mBorderPathEffect = effect;
  }

  @Override
  public void freeze(final YogaNode node, final @Nullable YogaNode parent) {
    freeze(mComponentContext.getLayoutStateContext(), node, parent);
  }

  @Override
  public void freeze(
      final LayoutStateContext context,
      final YogaNode node,
      final @Nullable YogaNode parentYogaNode) {

    // If parents important for A11Y is YES_HIDE_DESCENDANTS then
    // child's important for A11Y needs to be NO_HIDE_DESCENDANTS
    final InternalNode parent =
        parentYogaNode != null ? (InternalNode) parentYogaNode.getData() : null;

    if (parent != null
        && parent.getImportantForAccessibility()
            == IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS) {
      importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    // If the parent of this node is disabled, this node has to be disabled too.
    final @NodeInfo.EnabledState int parentEnabledState;
    if (parent != null && parent.getNodeInfo() != null) {
      parentEnabledState = parent.getNodeInfo().getEnabledState();
    } else {
      parentEnabledState = ENABLED_UNSET;
    }

    // If the parent of this node is disabled, this node has to be disabled too.
    if (parentEnabledState == ENABLED_SET_FALSE) {
      getOrCreateNodeInfo().setEnabled(false);
    }
  }

  @Override
  public LithoLayoutResult calculateLayout(
      final RenderState.LayoutContext<LithoRenderContext> c,
      final int widthSpec,
      final int heightSpec) {

    if (mYogaNode.getStyleDirection() == YogaDirection.INHERIT
        && isLayoutDirectionRTL(c.getAndroidContext())) {
      mYogaNode.setDirection(YogaDirection.RTL);
    }

    if (YogaConstants.isUndefined(mYogaNode.getWidth().value)) {
      Layout.setStyleWidthFromSpec(mYogaNode, widthSpec);
    }
    if (YogaConstants.isUndefined(mYogaNode.getHeight().value)) {
      Layout.setStyleHeightFromSpec(mYogaNode, heightSpec);
    }

    float width =
        SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(widthSpec);
    float height =
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(heightSpec);

    applyOverridesRecursive(this);

    mYogaNode.calculateLayout(width, height);

    return this;
  }

  @Override
  public InternalNode child(ComponentContext c, Component child) {
    if (child != null) {
      return child(Layout.create(c, child));
    }

    return this;
  }

  @Override
  public InternalNode child(InternalNode child) {
    if (child != null && child != NULL_LAYOUT) {
      addChildAt(child, mYogaNode.getChildCount());
    }

    return this;
  }

  @Override
  public InternalNode duplicateParentState(boolean duplicateParentState) {
    mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
    mDuplicateParentState = duplicateParentState;
    return this;
  }

  @Override
  public InternalNode duplicateChildrenStates(boolean duplicateChildrenStates) {
    mPrivateFlags |= PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET;
    mDuplicateChildrenStates = duplicateChildrenStates;
    return this;
  }

  @Override
  public void flex(float flex) {
    mPrivateFlags |= PFLAG_FLEX_IS_SET;
    mYogaNode.setFlex(flex);
  }

  // Used by stetho to re-set auto value
  @Override
  public void flexBasisAuto() {
    mYogaNode.setFlexBasisAuto();
  }

  @Override
  public void flexBasisPercent(float percent) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    mYogaNode.setFlexBasisPercent(percent);
  }

  @Override
  public void flexBasisPx(@Px int flexBasis) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    mYogaNode.setFlexBasis(flexBasis);
  }

  @Override
  public InternalNode flexDirection(YogaFlexDirection direction) {
    mYogaNode.setFlexDirection(direction);
    return this;
  }

  @Override
  public void flexGrow(float flexGrow) {
    mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
    mYogaNode.setFlexGrow(flexGrow);
  }

  @Override
  public void flexShrink(float flexShrink) {
    mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
    mYogaNode.setFlexShrink(flexShrink);
  }

  @Override
  public InternalNode focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    mPrivateFlags |= PFLAG_FOCUSED_HANDLER_IS_SET;
    mFocusedHandler = addVisibilityHandler(mFocusedHandler, focusedHandler);
    return this;
  }

  @Override
  public InternalNode foreground(@Nullable Drawable foreground) {
    mPrivateFlags |= PFLAG_FOREGROUND_IS_SET;
    mForeground = foreground;
    return this;
  }

  @Override
  public InternalNode foregroundColor(@ColorInt int foregroundColor) {
    return foreground(ComparableColorDrawable.create(foregroundColor));
  }

  @Override
  public InternalNode foregroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return foreground(null);
    }

    return foreground(ContextCompat.getDrawable(mComponentContext.getAndroidContext(), resId));
  }

  @Override
  public InternalNode layerType(final @LayerType int type, @Nullable final Paint paint) {
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      mLayerType = type;
      mLayerPaint = paint;
    }
    return this;
  }

  @Override
  public int getLayerType() {
    return mLayerType;
  }

  @Override
  public @Nullable Paint getLayerPaint() {
    return mLayerPaint;
  }

  @Override
  public InternalNode fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    mPrivateFlags |= PFLAG_FULL_IMPRESSION_HANDLER_IS_SET;
    mFullImpressionHandler = addVisibilityHandler(mFullImpressionHandler, fullImpressionHandler);
    return this;
  }

  @Override
  public int[] getBorderColors() {
    return mBorderColors;
  }

  @Override
  public @Nullable PathEffect getBorderPathEffect() {
    return mBorderPathEffect;
  }

  @Override
  public float[] getBorderRadius() {
    return mBorderRadius;
  }

  @Override
  public @Nullable RenderUnit<?> getRenderUnit() {
    throw new UnsupportedOperationException("This API is not yet implemented");
  }

  @Override
  public @Nullable Object getLayoutData() {
    throw new UnsupportedOperationException("This API is not yet implemented");
  }

  @Override
  public int getChildrenCount() {
    return getChildCount();
  }

  @Override
  public @Nullable DefaultInternalNode getChildAt(int index) {
    return (DefaultInternalNode) mYogaNode.getChildAt(index).getData();
  }

  @Override
  public int getXForChildAtIndex(int index) {
    return getChildAt(index).getX();
  }

  @Override
  public int getYForChildAtIndex(int index) {
    return getChildAt(index).getY();
  }

  @Override
  public void addChild(LithoLayoutResult child) {
    throw new UnsupportedOperationException("This API is not supported by DefaultInternalNode");
  }

  @Override
  public int getChildCount() {
    return mYogaNode.getChildCount();
  }

  @Override
  public int getChildIndex(InternalNode child) {
    for (int i = 0, count = getChildCount(); i < count; i++) {
      if (getChildAt(i) == child) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the list of components contributing to this InternalNode. We have no need for this in
   * production but it is useful information to have while debugging. Therefor this list will only
   * contain the root component if running in production mode.
   */
  @Override
  public List<Component> getComponents() {
    return mComponents;
  }

  /**
   * Return the list of keys of components contributing to this InternalNode. We have no need for
   * this in production but it is useful information to have while debugging. Therefor this list
   * will only contain the root component if running in production mode.
   */
  @Override
  public List<String> getComponentKeys() {
    return mComponentGlobalKeys;
  }

  @Override
  public @Nullable List<Component> getUnresolvedComponents() {
    return mUnresolvedComponents;
  }

  @Override
  public @Nullable Map<String, Component> getComponentsNeedingPreviousRenderData() {
    return mComponentsNeedingPreviousRenderData;
  }

  @Override
  public Context getAndroidContext() {
    return mComponentContext.getAndroidContext();
  }

  @Override
  public ComponentContext getContext() {
    return mComponentContext;
  }

  @Override
  public InternalNode getInternalNode() {
    return this;
  }

  @Override
  public @Nullable DiffNode getDiffNode() {
    return mDiffNode;
  }

  @Override
  public void setDiffNode(@Nullable DiffNode diffNode) {
    mDiffNode = diffNode;
  }

  @Override
  public @Nullable EventHandler<FocusedVisibleEvent> getFocusedHandler() {
    return mFocusedHandler;
  }

  @Override
  public @Nullable Drawable getForeground() {
    return mForeground;
  }

  @Override
  public @Nullable EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler() {
    return mFullImpressionHandler;
  }

  @Override
  public @Nullable Component getHeadComponent() {
    return mComponents.isEmpty() ? null : mComponents.get(mComponents.size() - 1);
  }

  @Override
  public @Nullable String getHeadComponentKey() {
    return mComponentGlobalKeys.get(mComponentGlobalKeys.size() - 1);
  }

  @Override
  public int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  @Override
  public @Nullable EventHandler<InvisibleEvent> getInvisibleHandler() {
    return mInvisibleHandler;
  }

  @Override
  public int getLastHeightSpec() {
    return mLastHeightSpec;
  }

  @Override
  public void setLastHeightSpec(int heightSpec) {
    mLastHeightSpec = heightSpec;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * height. This is used together with {@link LithoLayoutResult#getLastHeightSpec()} to implement
   * measure caching.
   */
  @Override
  public float getLastMeasuredHeight() {
    return mLastMeasuredHeight;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  @Override
  public void setLastMeasuredHeight(float lastMeasuredHeight) {
    mLastMeasuredHeight = lastMeasuredHeight;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * width. This is used together with {@link LithoLayoutResult#getLastWidthSpec()} to implement
   * measure caching.
   */
  @Override
  public float getLastMeasuredWidth() {
    return mLastMeasuredWidth;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  @Override
  public void setLastMeasuredWidth(float lastMeasuredWidth) {
    mLastMeasuredWidth = lastMeasuredWidth;
  }

  @Override
  public int getLastWidthSpec() {
    return mLastWidthSpec;
  }

  @Override
  public void setLastWidthSpec(int widthSpec) {
    mLastWidthSpec = widthSpec;
  }

  @Override
  public int getLayoutBorder(YogaEdge edge) {
    return FastMath.round(mYogaNode.getLayoutBorder(edge));
  }

  @Override
  public @Nullable NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  @Override
  public void setNodeInfo(NodeInfo nodeInfo) {
    mNodeInfo = nodeInfo;
  }

  @Override
  public NodeInfo getOrCreateNodeInfo() {
    if (mNodeInfo == null) {
      mNodeInfo = new DefaultNodeInfo();
    }

    return mNodeInfo;
  }

  @Override
  public @Nullable DefaultInternalNode getParent() {
    if (mYogaNode.getOwner() != null) {
      return (DefaultInternalNode) mYogaNode.getOwner().getData();
    } else {
      return mParent != null ? (DefaultInternalNode) mParent : null;
    }
  }

  @Override
  public void setParent(@Nullable LithoLayoutResult parent) {
    mParent = parent;
  }

  @Override
  public @Nullable Component getTailComponent() {
    return mComponents.isEmpty() ? null : mComponents.get(0);
  }

  @Override
  public @Nullable String getTailComponentKey() {
    return mComponentGlobalKeys.get(0);
  }

  @Override
  public @Nullable StateListAnimator getStateListAnimator() {
    return mStateListAnimator;
  }

  @Override
  public @DrawableRes int getStateListAnimatorRes() {
    return mStateListAnimatorRes;
  }

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  @Override
  public @Nullable String getTestKey() {
    return mTestKey;
  }

  @Override
  public @Nullable Edges getTouchExpansion() {
    return mTouchExpansion;
  }

  @Override
  public int getTouchExpansionBottom() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mTouchExpansion.get(YogaEdge.BOTTOM));
  }

  @Override
  public int getTouchExpansionLeft() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(resolveHorizontalEdges(mTouchExpansion, YogaEdge.LEFT));
  }

  @Override
  public int getTouchExpansionRight() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(resolveHorizontalEdges(mTouchExpansion, YogaEdge.RIGHT));
  }

  @Override
  public int getTouchExpansionTop() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mTouchExpansion.get(YogaEdge.TOP));
  }

  @Override
  public @Nullable String getTransitionKey() {
    return mTransitionKey;
  }

  @Override
  public @Nullable String getTransitionOwnerKey() {
    return mTransitionOwnerKey;
  }

  @Override
  public @Nullable Transition.TransitionKeyType getTransitionKeyType() {
    return mTransitionKeyType;
  }

  @Override
  public @Nullable ArrayList<Transition> getTransitions() {
    return mTransitions;
  }

  @Override
  public @Nullable String getTransitionGlobalKey() {
    final Component component = getTailComponent();
    return component != null ? getTailComponentKey() : null;
  }

  @Override
  public @Nullable EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler() {
    return mUnfocusedHandler;
  }

  @Override
  public @Nullable EventHandler<VisibilityChangedEvent> getVisibilityChangedHandler() {
    return mVisibilityChangedHandler;
  }

  @Override
  public @Nullable EventHandler<VisibleEvent> getVisibleHandler() {
    return mVisibleHandler;
  }

  @Override
  public float getVisibleHeightRatio() {
    return mVisibleHeightRatio;
  }

  @Override
  public float getVisibleWidthRatio() {
    return mVisibleWidthRatio;
  }

  @Override
  public @Nullable ArrayList<WorkingRangeContainer.Registration> getWorkingRangeRegistrations() {
    return mWorkingRangeRegistrations;
  }

  @Override
  public YogaNode getYogaNode() {
    return mYogaNode;
  }

  @Override
  public boolean hasBorderColor() {
    for (int color : mBorderColors) {
      if (color != Color.TRANSPARENT) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean hasStateListAnimatorResSet() {
    return (mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_RES_SET) != 0;
  }

  @Override
  public boolean hasTouchExpansion() {
    return ((mPrivateFlags & PFLAG_TOUCH_EXPANSION_IS_SET) != 0L);
  }

  @Override
  public boolean hasTransitionKey() {
    return !TextUtils.isEmpty(mTransitionKey);
  }

  @Override
  public boolean hasVisibilityHandlers() {
    return mVisibleHandler != null
        || mFocusedHandler != null
        || mUnfocusedHandler != null
        || mFullImpressionHandler != null
        || mInvisibleHandler != null
        || mVisibilityChangedHandler != null;
  }

  // Used by stetho to re-set auto value
  @Override
  public void heightAuto() {
    mYogaNode.setHeightAuto();
  }

  @Override
  public void heightPercent(float percent) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mYogaNode.setHeightPercent(percent);
  }

  @Override
  public void heightPx(@Px int height) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mYogaNode.setHeight(height);
  }

  @Override
  public InternalNode importantForAccessibility(int importantForAccessibility) {
    mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
    mImportantForAccessibility = importantForAccessibility;
    return this;
  }

  @Override
  public InternalNode invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    mPrivateFlags |= PFLAG_INVISIBLE_HANDLER_IS_SET;
    mInvisibleHandler = addVisibilityHandler(mInvisibleHandler, invisibleHandler);
    return this;
  }

  @Override
  public boolean isDuplicateParentStateEnabled() {
    return mDuplicateParentState;
  }

  @Override
  public boolean isDuplicateChildrenStatesEnabled() {
    return mDuplicateChildrenStates;
  }

  @Override
  public boolean isForceViewWrapping() {
    return mForceViewWrapping;
  }

  @Override
  public boolean isImportantForAccessibilityIsSet() {
    return (mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) == 0L
        || mImportantForAccessibility == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  }

  @Override
  public boolean isLayoutDirectionInherit() {
    return (mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) == 0L
        || mYogaNode.getStyleDirection() == YogaDirection.INHERIT;
  }

  @Override
  public void isReferenceBaseline(boolean isReferenceBaseline) {
    mYogaNode.setIsReferenceBaseline(isReferenceBaseline);
  }

  @Override
  public InternalNode justifyContent(YogaJustify justifyContent) {
    mYogaNode.setJustifyContent(justifyContent);
    return this;
  }

  @Override
  public void layoutDirection(YogaDirection direction) {
    mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
    mYogaNode.setDirection(direction);
  }

  @Override
  public void marginAuto(YogaEdge edge) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMarginAuto(edge);
  }

  @Override
  public void marginPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMarginPercent(edge, percent);
  }

  @Override
  public void marginPx(YogaEdge edge, @Px int margin) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMargin(edge, margin);
  }

  @Override
  public void maxHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    mYogaNode.setMaxHeightPercent(percent);
  }

  @Override
  public void maxHeightPx(@Px int maxHeight) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    mYogaNode.setMaxHeight(maxHeight);
  }

  @Override
  public void maxWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    mYogaNode.setMaxWidthPercent(percent);
  }

  @Override
  public void maxWidthPx(@Px int maxWidth) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    mYogaNode.setMaxWidth(maxWidth);
  }

  @Override
  public void minHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    mYogaNode.setMinHeightPercent(percent);
  }

  @Override
  public void minHeightPx(@Px int minHeight) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    mYogaNode.setMinHeight(minHeight);
  }

  @Override
  public void minWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    mYogaNode.setMinWidthPercent(percent);
  }

  @Override
  public void minWidthPx(@Px int minWidth) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    mYogaNode.setMinWidth(minWidth);
  }

  @Override
  public void paddingPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;
    mYogaNode.setPaddingPercent(edge, percent);
  }

  @Override
  public void paddingPx(YogaEdge edge, @Px int padding) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;
    mYogaNode.setPadding(edge, padding);
  }

  @Override
  public void positionPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    mYogaNode.setPositionPercent(edge, percent);
  }

  @Override
  public void positionPx(YogaEdge edge, @Px int position) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    mYogaNode.setPosition(edge, position);
  }

  @Override
  public void positionType(@Nullable YogaPositionType positionType) {
    mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
    mYogaNode.setPositionType(positionType);
  }

  /** Continually walks the node hierarchy until a node returns a non inherited layout direction */
  @Override
  public YogaDirection recursivelyResolveLayoutDirection() {
    YogaNode yogaNode = mYogaNode;
    while (yogaNode != null && yogaNode.getLayoutDirection() == YogaDirection.INHERIT) {
      yogaNode = yogaNode.getOwner();
    }
    return yogaNode == null ? YogaDirection.INHERIT : yogaNode.getLayoutDirection();
  }

  @Override
  public void registerDebugComponent(DebugComponent debugComponent) {
    if (mDebugComponents == null) {
      mDebugComponents = new HashSet<>();
    }
    mDebugComponents.add(debugComponent);
  }

  @Deprecated
  @Override
  public boolean implementsLayoutDiffing() {
    return true;
  }

  @Override
  public InternalNode removeChildAt(int index) {
    return (InternalNode) mYogaNode.removeChildAt(index).getData();
  }

  @Override
  public void setBorderWidth(YogaEdge edge, float borderWidth) {
    mYogaNode.setBorder(edge, borderWidth);
  }

  @Override
  public void setCachedMeasuresValid(boolean valid) {
    mCachedMeasuresValid = valid;
  }

  @Override
  public void setMeasureFunction(YogaMeasureFunction measureFunction) {
    mYogaNode.setMeasureFunction(measureFunction);
  }

  @Override
  public boolean shouldDrawBorders() {
    return hasBorderColor()
        && (mYogaNode.getLayoutBorder(LEFT) != 0
            || mYogaNode.getLayoutBorder(TOP) != 0
            || mYogaNode.getLayoutBorder(RIGHT) != 0
            || mYogaNode.getLayoutBorder(BOTTOM) != 0);
  }

  @Override
  public InternalNode stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_SET;
    mStateListAnimator = stateListAnimator;
    wrapInView();
    return this;
  }

  @Override
  public InternalNode stateListAnimatorRes(@DrawableRes int resId) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_RES_SET;
    mStateListAnimatorRes = resId;
    wrapInView();
    return this;
  }

  @Override
  public InternalNode testKey(@Nullable String testKey) {
    mTestKey = testKey;
    return this;
  }

  @Override
  public InternalNode touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    if (mTouchExpansion == null) {
      mTouchExpansion = new Edges();
    }

    mPrivateFlags |= PFLAG_TOUCH_EXPANSION_IS_SET;
    mTouchExpansion.set(edge, touchExpansion);

    return this;
  }

  @Override
  public InternalNode transitionKey(@Nullable String key, @Nullable String ownerKey) {
    if (SDK_INT >= ICE_CREAM_SANDWICH && !TextUtils.isEmpty(key)) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
      mTransitionKey = key;
      mTransitionOwnerKey = ownerKey;
    }

    return this;
  }

  @Override
  public InternalNode transitionKeyType(@Nullable Transition.TransitionKeyType type) {
    mPrivateFlags |= PFLAG_TRANSITION_KEY_TYPE_IS_SET;
    mTransitionKeyType = type;
    return this;
  }

  @Override
  public InternalNode unfocusedHandler(
      @Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    mPrivateFlags |= PFLAG_UNFOCUSED_HANDLER_IS_SET;
    mUnfocusedHandler = addVisibilityHandler(mUnfocusedHandler, unfocusedHandler);
    return this;
  }

  @Override
  public void useHeightAsBaseline(boolean useHeightAsBaselineFunction) {
    if (useHeightAsBaselineFunction) {
      mYogaNode.setBaselineFunction(
          new YogaBaselineFunction() {
            @Override
            public float baseline(YogaNode yogaNode, float width, float height) {
              return height;
            }
          });
    }
  }

  @Override
  public InternalNode visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET;
    mVisibilityChangedHandler =
        addVisibilityHandler(mVisibilityChangedHandler, visibilityChangedHandler);
    return this;
  }

  @Override
  public InternalNode visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_HANDLER_IS_SET;
    mVisibleHandler = addVisibilityHandler(mVisibleHandler, visibleHandler);
    return this;
  }

  @Override
  public InternalNode visibleHeightRatio(float visibleHeightRatio) {
    mVisibleHeightRatio = visibleHeightRatio;
    return this;
  }

  @Override
  public InternalNode visibleWidthRatio(float visibleWidthRatio) {
    mVisibleWidthRatio = visibleWidthRatio;
    return this;
  }

  @Override
  public void widthAuto() {
    mYogaNode.setWidthAuto();
  }

  @Override
  public void widthPercent(float percent) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mYogaNode.setWidthPercent(percent);
  }

  @Override
  public void widthPx(@Px int width) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mYogaNode.setWidth(width);
  }

  @Override
  public InternalNode wrap(YogaWrap wrap) {
    mYogaNode.setWrap(wrap);
    return this;
  }

  @Override
  public InternalNode wrapInView() {
    mForceViewWrapping = true;
    return this;
  }

  @Px
  @Override
  public int getX() {
    return (int) mYogaNode.getLayoutX();
  }

  @Px
  @Override
  public int getY() {
    return (int) mYogaNode.getLayoutY();
  }

  @Px
  @Override
  public int getWidth() {
    return (int) mYogaNode.getLayoutWidth();
  }

  @Px
  @Override
  public int getHeight() {
    return (int) mYogaNode.getLayoutHeight();
  }

  @Px
  @Override
  public int getPaddingTop() {
    return FastMath.round(mYogaNode.getLayoutPadding(TOP));
  }

  @Px
  @Override
  public int getPaddingRight() {
    return FastMath.round(mYogaNode.getLayoutPadding(RIGHT));
  }

  @Px
  @Override
  public int getPaddingBottom() {
    return FastMath.round(mYogaNode.getLayoutPadding(BOTTOM));
  }

  @Px
  @Override
  public int getPaddingLeft() {
    return FastMath.round(mYogaNode.getLayoutPadding(LEFT));
  }

  @Override
  public int getWidthSpec() {
    return mLastWidthSpec;
  }

  @Override
  public int getHeightSpec() {
    return mLastHeightSpec;
  }

  @Override
  public boolean isPaddingSet() {
    return (mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L;
  }

  @Override
  public @Nullable Drawable getBackground() {
    return mBackground;
  }

  @Override
  public YogaDirection getResolvedLayoutDirection() {
    return mYogaNode.getLayoutDirection();
  }

  @Override
  public void applyAttributes(Context c, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {

    TypedArray a =
        c.obtainStyledAttributes(null, R.styleable.ComponentLayout, defStyleAttr, defStyleRes);

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == R.styleable.ComponentLayout_android_layout_width) {
        int width = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (width >= 0) {
          widthPx(width);
        }
      } else if (attr == R.styleable.ComponentLayout_android_layout_height) {
        int height = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (height >= 0) {
          heightPx(height);
        }
      } else if (attr == R.styleable.ComponentLayout_android_minHeight) {
        minHeightPx(a.getDimensionPixelSize(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_minWidth) {
        minWidthPx(a.getDimensionPixelSize(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingLeft) {
        paddingPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingTop) {
        paddingPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingRight) {
        paddingPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingBottom) {
        paddingPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingStart && SUPPORTS_RTL) {
        paddingPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingEnd && SUPPORTS_RTL) {
        paddingPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_padding) {
        paddingPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginLeft) {
        marginPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginTop) {
        marginPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginRight) {
        marginPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginBottom) {
        marginPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginStart && SUPPORTS_RTL) {
        marginPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginEnd && SUPPORTS_RTL) {
        marginPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_margin) {
        marginPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_importantForAccessibility
          && SDK_INT >= JELLY_BEAN) {
        importantForAccessibility(a.getInt(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_duplicateParentState) {
        duplicateParentState(a.getBoolean(attr, false));
      } else if (attr == R.styleable.ComponentLayout_android_background) {
        if (TypedArrayUtils.isColorAttribute(a, R.styleable.ComponentLayout_android_background)) {
          backgroundColor(a.getColor(attr, 0));
        } else {
          backgroundRes(a.getResourceId(attr, -1));
        }
      } else if (attr == R.styleable.ComponentLayout_android_foreground) {
        if (TypedArrayUtils.isColorAttribute(a, R.styleable.ComponentLayout_android_foreground)) {
          foregroundColor(a.getColor(attr, 0));
        } else {
          foregroundRes(a.getResourceId(attr, -1));
        }
      } else if (attr == R.styleable.ComponentLayout_android_contentDescription) {
        getOrCreateNodeInfo().setContentDescription(a.getString(attr));
      } else if (attr == R.styleable.ComponentLayout_flex_direction) {
        flexDirection(YogaFlexDirection.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_wrap) {
        wrap(YogaWrap.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_justifyContent) {
        justifyContent(YogaJustify.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_alignItems) {
        alignItems(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_alignSelf) {
        alignSelf(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_positionType) {
        positionType(YogaPositionType.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex) {
        final float flex = a.getFloat(attr, -1);
        if (flex >= 0f) {
          flex(flex);
        }
      } else if (attr == R.styleable.ComponentLayout_flex_left) {
        positionPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_top) {
        positionPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_right) {
        positionPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_bottom) {
        positionPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_layoutDirection) {
        final int layoutDirection = a.getInteger(attr, -1);
        layoutDirection(YogaDirection.fromInt(layoutDirection));
      }
    }

    a.recycle();
  }

  /** Crash if the given node has context specific style set. */
  @Override
  public void assertContextSpecificStyleNotSet() {
    List<CharSequence> errorTypes = null;
    if ((mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "alignSelf");
    }
    if ((mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "positionType");
    }
    if ((mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "flex");
    }
    if ((mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "flexGrow");
    }
    if ((mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "margin");
    }

    if (errorTypes != null) {
      final CharSequence errorStr = TextUtils.join(", ", errorTypes);
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          CONTEXT_SPECIFIC_STYLE_SET,
          "You should not set "
              + errorStr
              + " to a root layout in "
              + getTailComponent().getClass().getSimpleName());
    }
  }

  @Override
  public DefaultInternalNode deepClone() {

    // 1. Clone this layout.
    final DefaultInternalNode copy = clone();

    // 2. Clone the YogaNode of this layout and set it on the cloned layout.
    YogaNode node = mYogaNode.cloneWithoutChildren();
    copy.mYogaNode = node;
    node.setData(copy);

    // 3. Deep clone all children and add it to the cloned YogaNode
    final int count = getChildCount();
    for (int i = 0; i < count; i++) {
      copy.child(getChildAt(i).deepClone());
    }

    return copy;
  }

  @Override
  public String getSimpleName() {
    return mComponents.isEmpty() ? "<null>" : mComponents.get(0).getSimpleName();
  }

  @Override
  public InternalNode reconcile(ComponentContext c, Component next, @Nullable String nextKey) {
    final StateHandler stateHandler = c.getStateHandler();
    final Set<String> keys;
    if (stateHandler == null) {
      keys = Collections.emptySet();
    } else {
      keys = stateHandler.getKeysForPendingUpdates();
    }

    return reconcile(c, this, next, nextKey, keys);
  }

  void setComponentContext(ComponentContext c) {
    mComponentContext = c;
  }

  @Override
  public boolean isClone() {
    return mIsClone;
  }

  protected DefaultInternalNode clone() {
    final DefaultInternalNode node;
    try {
      node = (DefaultInternalNode) super.clone();
      node.mIsClone = true;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }

    return node;
  }

  private void applyOverridesRecursive(@Nullable InternalNode node) {
    if (ComponentsConfiguration.isDebugModeEnabled && node != null) {
      DebugComponent.applyOverrides(mComponentContext, node);

      for (int i = 0, count = node.getChildCount(); i < count; i++) {
        applyOverridesRecursive(node.getChildAt(i));
      }

      if (node instanceof NestedTreeHolderResult) {
        LithoLayoutResult result = ((NestedTreeHolderResult) node).getNestedResult();
        if (result != null) {
          applyOverridesRecursive((InternalNode) result);
        }
      }
    }
  }

  private float resolveHorizontalEdges(Edges spacing, YogaEdge edge) {
    final boolean isRtl = (mYogaNode.getLayoutDirection() == YogaDirection.RTL);

    final YogaEdge resolvedEdge;
    switch (edge) {
      case LEFT:
        resolvedEdge = (isRtl ? YogaEdge.END : YogaEdge.START);
        break;

      case RIGHT:
        resolvedEdge = (isRtl ? YogaEdge.START : YogaEdge.END);
        break;

      default:
        throw new IllegalArgumentException("Not an horizontal padding edge: " + edge);
    }

    float result = spacing.getRaw(resolvedEdge);
    if (YogaConstants.isUndefined(result)) {
      result = spacing.get(edge);
    }

    return result;
  }

  private void setPaddingFromBackground(@Nullable Drawable drawable) {
    if (drawable != null) {
      final Rect backgroundPadding = new Rect();
      if (getDrawablePadding(drawable, backgroundPadding)) {
        paddingPx(LEFT, backgroundPadding.left);
        paddingPx(TOP, backgroundPadding.top);
        paddingPx(RIGHT, backgroundPadding.right);
        paddingPx(BOTTOM, backgroundPadding.bottom);
      }
    }
  }

  private boolean shouldApplyTouchExpansion() {
    return mTouchExpansion != null && mNodeInfo != null && mNodeInfo.hasTouchEventHandlers();
  }

  /**
   * Release properties which are not longer required for the current layout pass or release
   * properties which should be reset during reconciliation.
   */
  protected void clean() {
    // 1. Release or clone props.
    mComponents = new ArrayList<>();
    mComponentGlobalKeys = new ArrayList<>();
    mDiffNode = null;
    mDebugComponents = null;
  }

  void updateWith(
      final ComponentContext c,
      final YogaNode node,
      final List<Component> components,
      final List<String> componentKeys,
      final @Nullable DiffNode diffNode) {
    // 1. Set new ComponentContext, YogaNode, and components.
    mComponentContext = c;
    mYogaNode = node;
    mYogaNode.setData(this);
    mComponents = components;
    mComponentGlobalKeys = componentKeys;

    mDiffNode = diffNode;

    // 2. Update props.

    mComponentsNeedingPreviousRenderData = null;
    for (int i = 0, size = components.size(); i < size; i++) {
      final Component component = components.get(i);
      final String key = componentKeys.get(i);
      if (component.needsPreviousRenderData()) {
        addComponentNeedingPreviousRenderData(key, component);
      }
    }

    ArrayList<WorkingRangeContainer.Registration> ranges = mWorkingRangeRegistrations;
    mWorkingRangeRegistrations = null;
    if (ranges != null && !ranges.isEmpty()) {
      mWorkingRangeRegistrations = new ArrayList<>(ranges.size());
      for (WorkingRangeContainer.Registration old : ranges) {
        final String key = old.mKey;
        final Component component = old.mComponent.makeUpdatedShallowCopy(c, key);
        mWorkingRangeRegistrations.add(
            new WorkingRangeContainer.Registration(old.mName, old.mWorkingRange, component, key));
      }
    }
  }

  /**
   * Convenience method to get an updated shallow copy of all the components of this InternalNode.
   * Optionally replace the head component with a new component. The head component is the root
   * component in the Component hierarchy representing this InternalNode.
   *
   * @param head The root component of this InternalNode's Component hierarchy.
   * @return List of updated shallow copied components of this InternalNode.
   */
  private Pair<List<Component>, List<String>> getUpdatedComponents(
      final LayoutStateContext layoutStateContext, Component head, @Nullable String headKey) {
    int size = mComponents.size();
    List<Component> updated = new ArrayList<>(size);
    List<String> updatedKeys = new ArrayList<>(size);

    // 1. Add the updated head component to the list.
    updated.add(head);
    updatedKeys.add(headKey);

    // 2. Set parent context for descendants.
    ComponentContext parentContext = head.getScopedContext(layoutStateContext, headKey);

    // 3. Shallow copy and update all components, except the head component.
    for (int i = size - 2; i >= 0; i--) {
      final String key = mComponentGlobalKeys.get(i);
      final Component component = mComponents.get(i).makeUpdatedShallowCopy(parentContext, key);
      updated.add(component);
      updatedKeys.add(key);

      parentContext =
          component.getScopedContext(layoutStateContext, key); // set parent context for descendant
    }

    // 4. Reverse the list so that the root component is at index 0.
    Collections.reverse(updated);
    Collections.reverse(updatedKeys);

    return new Pair<>(updated, updatedKeys);
  }

  private @Nullable static <T> EventHandler<T> addVisibilityHandler(
      @Nullable EventHandler<T> currentHandler, @Nullable EventHandler<T> newHandler) {
    if (currentHandler == null) {
      return newHandler;
    }
    if (newHandler == null) {
      return currentHandler;
    }
    return new DelegatingEventHandler<>(currentHandler, newHandler);
  }

  /**
   * This is a wrapper on top of built in {@link Drawable#getPadding(Rect)} which overrides default
   * return value. The reason why we need this - is because on pre-L devices LayerDrawable always
   * returns "true" even if drawable doesn't have padding (see https://goo.gl/gExcMQ). Since we
   * heavily rely on correctness of this information, we need to check padding manually
   */
  private static boolean getDrawablePadding(Drawable drawable, Rect outRect) {
    drawable.getPadding(outRect);
    return outRect.bottom != 0 || outRect.top != 0 || outRect.left != 0 || outRect.right != 0;
  }

  /**
   * Internal method to <b>try</b> and reconcile the {@param current} InternalNode with a new {@link
   * ComponentContext} and an updated head {@link Component}.
   *
   * @param parentContext The ComponentContext.
   * @param current The current InternalNode which should be updated.
   * @param next The updated component to be used to reconcile this InternalNode.
   * @param keys The keys of mutated components.
   * @return A new updated InternalNode.
   */
  private static InternalNode reconcile(
      final ComponentContext parentContext,
      final DefaultInternalNode current,
      final Component next,
      @Nullable final String nextKey,
      final Set<String> keys) {
    int mode =
        getReconciliationMode(
            next.getScopedContext(parentContext.getLayoutStateContext(), nextKey), current, keys);
    final InternalNode layout;

    switch (mode) {
      case ReconciliationMode.COPY:
        layout =
            reconcile(
                parentContext.getLayoutStateContext(),
                current,
                next,
                nextKey,
                keys,
                ReconciliationMode.COPY);
        break;
      case ReconciliationMode.RECONCILE:
        layout =
            reconcile(
                parentContext.getLayoutStateContext(),
                current,
                next,
                nextKey,
                keys,
                ReconciliationMode.RECONCILE);
        break;
      case ReconciliationMode.RECREATE:
        layout = Layout.create(parentContext, next, false, true, nextKey);
        break;
      default:
        throw new IllegalArgumentException(mode + " is not a valid ReconciliationMode");
    }

    return layout;
  }

  /**
   * Internal method to reconcile the {@param current} InternalNode with a new {@link
   * ComponentContext} and an updated head {@link Component} and a {@link ReconciliationMode}.
   *
   * @param current The current InternalNode which should be updated.
   * @param next The updated component to be used to reconcile this InternalNode.
   * @param keys The keys of mutated components.
   * @param mode {@link ReconciliationMode#RECONCILE} or {@link ReconciliationMode#COPY}.
   * @return A new updated InternalNode.
   */
  private static InternalNode reconcile(
      final LayoutStateContext layoutStateContext,
      final DefaultInternalNode current,
      final Component next,
      final @Nullable String nextKey,
      final Set<String> keys,
      final @ReconciliationMode int mode) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection(
          (mode == ReconciliationMode.COPY ? "copy:" : "reconcile:") + next.getSimpleName());
    }

    // 1. Shallow copy this layouts's YogaNode.
    final YogaNode currentNode = current.getYogaNode();

    if (isTracing) {
      ComponentsSystrace.beginSection("cloneYogaNode:" + next.getSimpleName());
    }

    final YogaNode copiedNode = currentNode.cloneWithoutChildren();

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    // 2. Shallow copy this layout.
    final DefaultInternalNode layout =
        getCleanUpdatedShallowCopy(layoutStateContext, current, next, nextKey, copiedNode);
    ComponentContext parentContext =
        layout
            .getTailComponent()
            .getScopedContext(layoutStateContext, layout.getTailComponentKey());

    // 3. Iterate over children.
    int count = currentNode.getChildCount();
    for (int i = 0; i < count; i++) {
      final DefaultInternalNode child = (DefaultInternalNode) currentNode.getChildAt(i).getData();

      // 3.1 Get the head component of the child layout.
      List<Component> components = child.getComponents();
      List<String> componentKeys = child.getComponentKeys();
      int index = Math.max(0, components.size() - 1);
      final Component component = components.get(index);
      final String key = componentKeys == null ? null : componentKeys.get(index);

      // 3.2 Update the head component of the child layout.
      final Component updated = component.makeUpdatedShallowCopy(parentContext, key);

      // 3.3 Reconcile child layout.
      final InternalNode copy;
      if (mode == ReconciliationMode.COPY) {
        copy = reconcile(layoutStateContext, child, updated, key, keys, ReconciliationMode.COPY);
      } else {
        copy = reconcile(parentContext, child, updated, key, keys);
      }

      // 3.3 Add the child to the cloned yoga node
      layout.child(copy);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return layout;
  }

  /**
   * Convenience method to create a shallow copy of the InternalNode, set a new YogaNode, update all
   * components and ComponentContext, release all the unnecessary properties from the new
   * InternalNode.
   */
  private static DefaultInternalNode getCleanUpdatedShallowCopy(
      final LayoutStateContext layoutStateContext,
      final DefaultInternalNode current,
      final Component head,
      final @Nullable String headKey,
      final YogaNode node) {

    final boolean isTracing = ComponentsSystrace.isTracing();

    if (isTracing) {
      ComponentsSystrace.beginSection("clone:" + head.getSimpleName());
    }

    // 1. Shallow copy this layout.
    final DefaultInternalNode layout = current.clone();

    if (isTracing) {
      ComponentsSystrace.endSection();
      ComponentsSystrace.beginSection("clean:" + head.getSimpleName());
    }

    // 2. Reset and release properties
    layout.clean();

    if (isTracing) {
      ComponentsSystrace.endSection();
      ComponentsSystrace.beginSection("update:" + head.getSimpleName());
    }

    // 3. Get updated components
    Pair<List<Component>, List<String>> updated =
        current.getUpdatedComponents(layoutStateContext, head, headKey);

    // 4. Update the layout with the updated context, components, and YogaNode.
    layout.updateWith(
        head.getScopedContext(layoutStateContext, headKey),
        node,
        updated.first,
        updated.second,
        null);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return layout;
  }

  /**
   * Returns the a {@link ReconciliationMode} mode which directs the reconciling process to branch
   * to either recreate the entire subtree, copy the entire subtree or continue to recursively
   * reconcile the subtree.
   */
  @VisibleForTesting
  static @ReconciliationMode int getReconciliationMode(
      final ComponentContext c, final InternalNode current, final Set<String> keys) {
    final List<Component> components = current.getComponents();
    final List<String> componentKeys = current.getComponentKeys();
    final Component root = current.getHeadComponent();

    // 1.0 check early exit conditions
    if (c == null || root == null || current instanceof NestedTreeHolder) {
      return ReconciliationMode.RECREATE;
    }

    // 1.1 Check if any component has mutations
    for (int i = 0, size = components.size(); i < size; i++) {
      final String key = componentKeys.get(i);
      if (keys.contains(key)) {
        return ReconciliationMode.RECREATE;
      }
    }

    // 2.0 Check if any descendants have mutations
    final String rootKey = current.getHeadComponentKey();
    for (String key : keys) {
      if (key.startsWith(rootKey)) {
        return ReconciliationMode.RECONCILE;
      }
    }

    return ReconciliationMode.COPY;
  }

  @Override
  public Copyable getLayoutParams() {
    throw new UnsupportedOperationException("This API is not yet implemented");
  }

  @Override
  public Copyable makeCopy() {
    return null;
  }
}
