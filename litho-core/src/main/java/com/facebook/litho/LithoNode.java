/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Layout.isLayoutDirectionRTL;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.ENABLED_UNSET;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
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
import android.util.Pair;
import android.view.View;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;
import androidx.core.view.ViewCompat;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.rendercore.LayoutContext;
import com.facebook.rendercore.Mountable;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.primitives.Primitive;
import com.facebook.rendercore.primitives.utils.EquivalenceUtils;
import com.facebook.yoga.YogaAlign;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** {@link LithoNode} is the {@link Node} implementation of Litho. */
@OkToExtend
@ThreadConfined(ThreadConfined.ANY)
public class LithoNode implements Node<LithoRenderContext>, Cloneable {

  // Used to check whether or not the framework can use style IDs for
  // paddingStart/paddingEnd due to a bug in some Android devices.
  private static final boolean SUPPORTS_RTL = (SDK_INT >= JELLY_BEAN_MR1);

  // Flags used to indicate that a certain attribute was explicitly set on the node.
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L;
  private static final long PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1L << 7;
  protected static final long PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1L << 8;
  protected static final long PFLAG_BACKGROUND_IS_SET = 1L << 18;
  protected static final long PFLAG_FOREGROUND_IS_SET = 1L << 19;
  protected static final long PFLAG_VISIBLE_HANDLER_IS_SET = 1L << 20;
  protected static final long PFLAG_FOCUSED_HANDLER_IS_SET = 1L << 21;
  protected static final long PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1L << 22;
  protected static final long PFLAG_INVISIBLE_HANDLER_IS_SET = 1L << 23;
  protected static final long PFLAG_UNFOCUSED_HANDLER_IS_SET = 1L << 24;
  private static final long PFLAG_TOUCH_EXPANSION_IS_SET = 1L << 25;
  protected static final long PFLAG_TRANSITION_KEY_IS_SET = 1L << 27;
  protected static final long PFLAG_BORDER_IS_SET = 1L << 28;
  protected static final long PFLAG_STATE_LIST_ANIMATOR_SET = 1L << 29;
  protected static final long PFLAG_STATE_LIST_ANIMATOR_RES_SET = 1L << 30;
  protected static final long PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET = 1L << 31;
  protected static final long PFLAG_TRANSITION_KEY_TYPE_IS_SET = 1L << 32;
  protected static final long PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET = 1L << 33;

  private List<LithoNode> mChildren = new ArrayList<>(4);

  @ThreadConfined(ThreadConfined.ANY)
  private final List<ScopedComponentInfo> mScopedComponentInfos = new ArrayList<>(2);

  protected final int[] mBorderEdgeWidths = new int[Border.EDGE_COUNT];
  protected final int[] mBorderColors = new int[Border.EDGE_COUNT];
  protected final float[] mBorderRadius = new float[Border.RADIUS_COUNT];

  protected @Nullable NodeInfo mNodeInfo;
  protected @Nullable EventHandler<VisibleEvent> mVisibleHandler;
  protected @Nullable EventHandler<FocusedVisibleEvent> mFocusedHandler;
  protected @Nullable EventHandler<UnfocusedVisibleEvent> mUnfocusedHandler;
  protected @Nullable EventHandler<FullImpressionVisibleEvent> mFullImpressionHandler;
  protected @Nullable EventHandler<InvisibleEvent> mInvisibleHandler;
  protected @Nullable EventHandler<VisibilityChangedEvent> mVisibilityChangedHandler;
  protected @Nullable Drawable mBackground;
  protected @Nullable Rect mPaddingFromBackground;
  protected @Nullable Drawable mForeground;
  protected @Nullable PathEffect mBorderPathEffect;
  protected @Nullable StateListAnimator mStateListAnimator;
  private @Nullable Edges mTouchExpansion;
  protected @Nullable String mTransitionKey;
  protected @Nullable String mTransitionOwnerKey;
  protected @Nullable Transition.TransitionKeyType mTransitionKeyType;
  private @Nullable ArrayList<Transition> mTransitions;
  private @Nullable Map<String, ScopedComponentInfo> mScopedComponentInfosNeedingPreviousRenderData;
  private @Nullable ArrayList<WorkingRangeContainer.Registration> mWorkingRangeRegistrations;
  private @Nullable ArrayList<Attachable> mAttachables;
  protected @Nullable String mTestKey;
  private @Nullable Set<DebugComponent> mDebugComponents;
  private @Nullable List<Component> mUnresolvedComponents;
  protected @Nullable Paint mLayerPaint;

  protected boolean mIsPaddingSet;
  protected boolean mDuplicateParentState;
  protected boolean mDuplicateChildrenStates;
  protected boolean mForceViewWrapping;

  protected int mLayerType = LayerType.LAYER_TYPE_NOT_SET;
  protected int mImportantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  protected @DrawableRes int mStateListAnimatorRes;

  protected float mVisibleHeightRatio;
  protected float mVisibleWidthRatio;

  protected @Nullable YogaDirection mLayoutDirection;
  protected @Nullable YogaFlexDirection mFlexDirection;
  protected @Nullable YogaJustify mJustifyContent;
  protected @Nullable YogaAlign mAlignContent;
  protected @Nullable YogaAlign mAlignItems;
  protected @Nullable YogaWrap mYogaWrap;

  private @Nullable NestedTreeHolder mNestedTreeHolder;
  private @Nullable Edges mNestedPaddingEdges;
  private @Nullable boolean[] mNestedIsPaddingPercent;

  protected @Nullable YogaMeasureFunction mYogaMeasureFunction;

  private @Nullable CommonProps.DefaultLayoutProps mDebugLayoutProps;

  private boolean mIsClone = false;
  private boolean mFrozen;
  private boolean mNodeInfoWasWritten;

  protected long mPrivateFlags;

  private @Nullable Mountable<?> mMountable;

  private @Nullable Primitive mPrimitive;

  protected LithoNode() {
    mDebugComponents = new HashSet<>();
  }

  public @Nullable Mountable<?> getMountable() {
    return mMountable;
  }

  public void setMountable(Mountable<?> mountable) {
    mMountable = mountable;
  }

  public @Nullable Primitive getPrimitive() {
    return mPrimitive;
  }

  public void setPrimitive(Primitive primitive) {
    if (primitive == null) {
      throw new RuntimeException("Primitive is null in LithoNode.setPrimitive()");
    }
    mPrimitive = primitive;
  }

  public void addChildAt(LithoNode child, int index) {
    mChildren.add(index, child);
  }

  public void addComponentNeedingPreviousRenderData(
      final String globalKey, final ScopedComponentInfo scopedComponentInfo) {
    if (mScopedComponentInfosNeedingPreviousRenderData == null) {
      mScopedComponentInfosNeedingPreviousRenderData = new HashMap<>(1);
    }
    mScopedComponentInfosNeedingPreviousRenderData.put(globalKey, scopedComponentInfo);
  }

  public void addTransition(Transition transition) {
    if (mTransitions == null) {
      mTransitions = new ArrayList<>(1);
    }
    mTransitions.add(transition);
  }

  public void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations) {
    if (mWorkingRangeRegistrations == null) {
      mWorkingRangeRegistrations = new ArrayList<>(registrations.size());
    }
    mWorkingRangeRegistrations.addAll(registrations);
  }

  public void addAttachable(Attachable attachable) {
    if (mAttachables == null) {
      mAttachables = new ArrayList<>(4);
    }
    mAttachables.add(attachable);
  }

  public void alignContent(YogaAlign alignContent) {
    mAlignContent = alignContent;
  }

  public void alignItems(YogaAlign alignItems) {
    mAlignItems = alignItems;
  }

  public void appendComponent(ScopedComponentInfo scopedComponentInfo) {
    mScopedComponentInfos.add(scopedComponentInfo);
  }

  public void appendUnresolvedComponent(Component component) {
    if (mUnresolvedComponents == null) {
      mUnresolvedComponents = new ArrayList<>();
    }

    mUnresolvedComponents.add(component);
  }

  public void background(@Nullable Drawable background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
  }

  public void backgroundColor(@ColorInt int backgroundColor) {
    background(ComparableColorDrawable.create(backgroundColor));
  }

  public void backgroundRes(final Context context, @DrawableRes int resId) {
    if (resId == 0) {
      background(null);
    } else {
      background(ContextCompat.getDrawable(context, resId));
    }
  }

  public void setPaddingFromBackground(@Nullable Rect padding) {
    mPaddingFromBackground = padding;
  }

  public void border(Border border) {
    border(border.mEdgeWidths, border.mEdgeColors, border.mRadius, border.mPathEffect);
  }

  public void border(int[] widths, int[] colors, float[] radii, @Nullable PathEffect effect) {
    mPrivateFlags |= PFLAG_BORDER_IS_SET;
    System.arraycopy(widths, 0, mBorderEdgeWidths, 0, mBorderEdgeWidths.length);
    System.arraycopy(colors, 0, mBorderColors, 0, mBorderColors.length);
    System.arraycopy(radii, 0, mBorderRadius, 0, mBorderRadius.length);
    mBorderPathEffect = effect;
  }

  protected void applyDiffNode(
      final LayoutStateContext current,
      final LithoLayoutResult result,
      final @Nullable LithoLayoutResult parent) {
    final @Nullable DiffNode diff;
    if (current.isReleased()) {
      return; // Cannot apply diff nodes with a released LayoutStateContext
    }

    if (parent == null) { // If root, then get diff node root from the current layout state
      if (isLayoutSpecWithSizeSpec(getHeadComponent()) && current.hasNestedTreeDiffNodeSet()) {
        diff = current.consumeNestedTreeDiffNode();
      } else {
        diff = current.getCurrentDiffTree();
      }
    } else if (parent.getDiffNode() != null) { // Otherwise get it from the parent
      final int index = parent.getNode().getChildIndex(this);
      if (index != -1 && index < parent.getDiffNode().getChildCount()) {
        diff = parent.getDiffNode().getChildAt(index);
      } else {
        diff = null;
      }
    } else {
      diff = null;
    }

    if (diff == null) { // Return if no diff node to apply.
      return;
    }

    final Component component = getTailComponent();

    if (!ComponentUtils.isSameComponentType(component, diff.getComponent())
        && !(parent != null && isLayoutSpecWithSizeSpec(component))) {
      return;
    }

    result.setDiffNode(diff);

    if (mMountable != null
        && diff.getMountable() != null
        && EquivalenceUtils.hasEquivalentFields(mMountable, diff.getMountable())) {

      result.setLayoutData(diff.getLayoutData());
      result.setCachedMeasuresValid(true);

    } else if (mPrimitive != null
        && diff.getPrimitive() != null
        && mPrimitive.getLayoutBehavior().isEquivalentTo(diff.getPrimitive().getLayoutBehavior())) {

      result.setLayoutData(diff.getLayoutData());
      result.setCachedMeasuresValid(true);

    } else if (!Layout.shouldComponentUpdate(this, diff)) {
      final ScopedComponentInfo scopedComponentInfo = getTailScopedComponentInfo();
      final ScopedComponentInfo diffNodeScopedComponentInfo =
          Preconditions.checkNotNull(diff.getScopedComponentInfo());

      if (component instanceof SpecGeneratedComponent) {
        ((SpecGeneratedComponent) component)
            .copyInterStageImpl(
                (InterStagePropsContainer) result.getLayoutData(),
                (InterStagePropsContainer) diff.getLayoutData());

        ((SpecGeneratedComponent) component)
            .copyPrepareInterStageImpl(
                scopedComponentInfo.getPrepareInterStagePropsContainer(),
                diffNodeScopedComponentInfo.getPrepareInterStagePropsContainer());
      }

      result.setCachedMeasuresValid(true);
    }
  }

  final void freeze(final CalculationStateContext context) {
    freeze(context, IMPORTANT_FOR_ACCESSIBILITY_AUTO, ENABLED_UNSET);
  }

  private void freeze(
      final CalculationStateContext context,
      final int parentImportantForAccessibility,
      final int parentEnabledState) {
    if (mFrozen) {
      return;
    }

    final boolean isRoot = context.getRootComponentId() == getHeadComponent().getId();

    if (!isRoot) { // if not root component

      // If parents important for A11Y is YES_HIDE_DESCENDANTS then
      // child's important for A11Y needs to be NO_HIDE_DESCENDANTS
      if (parentImportantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS) {
        importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
      }

      // If the parent of this node is disabled, this node has to be disabled too.
      if (parentEnabledState == ENABLED_SET_FALSE) {
        mutableNodeInfo().setEnabled(false);
      }
    }

    for (int i = 0; i < getChildCount(); i++) {
      getChildAt(i)
          .freeze(
              context,
              getImportantForAccessibility(),
              getNodeInfo() != null ? getNodeInfo().getEnabledState() : ENABLED_UNSET);
    }

    // Sets mFrozen as true to avoid anymore mutation.
    mFrozen = true;
  }

  /**
   * Builds the YogaNode tree from this tree of LithoNodes. At the same time, builds the
   * LayoutResult tree and sets it in the data of the corresponding YogaNodes.
   */
  private static @Nullable LithoLayoutResult buildYogaTree(
      LayoutContext<LithoRenderContext> context,
      LithoNode currentNode,
      @Nullable YogaNode parentNode) {
    final LithoRenderContext renderContext = context.getRenderContext();
    final @Nullable YogaLayoutProps writer = currentNode.createYogaNodeWriter();

    if (writer == null) {
      return null;
    }

    // Transfer the layout props to YogaNode
    currentNode.writeToYogaNode(writer);

    final YogaNode yogaNode = writer.getNode();

    // Only add the YogaNode and LayoutResult if the node renders something. If it does
    // not render anything then it should not participate in grow/shrink behaviours.
    final @Nullable LithoLayoutResult parentLayoutResult =
        parentNode != null ? LithoLayoutResult.getLayoutResultFromYogaNode(parentNode) : null;

    final LithoLayoutResult layoutResult = currentNode.createLayoutResult(yogaNode);
    currentNode.applyDiffNode(renderContext.layoutStateContext, layoutResult, parentLayoutResult);
    yogaNode.setData(new Pair(context, layoutResult));

    for (int i = 0; i < currentNode.getChildCount(); i++) {
      final @Nullable LithoLayoutResult childLayoutResult =
          buildYogaTree(context, currentNode.getChildAt(i), yogaNode);
      if (childLayoutResult != null) {
        yogaNode.addChildAt(childLayoutResult.getYogaNode(), yogaNode.getChildCount());
        layoutResult.addChild(childLayoutResult);
      }
    }

    return layoutResult;
  }

  public @Nullable LithoLayoutResult calculateLayout(
      final LayoutContext<LithoRenderContext> c, final int widthSpec, final int heightSpec) {

    if (c.getRenderContext().layoutStateContext.isReleased()) {
      throw new IllegalStateException(
          "Cannot calculate a layout with a released LayoutStateContext.");
    }

    final boolean isTracing = ComponentsSystrace.isTracing();

    applyOverridesRecursive(c.getRenderContext().layoutStateContext, this);

    if (isTracing) {
      ComponentsSystrace.beginSection("freeze:" + getHeadComponent().getSimpleName());
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("buildYogaTree:" + getHeadComponent().getSimpleName());
    }

    final @Nullable LithoLayoutResult layoutResult = buildYogaTree(c, this, null);
    final @Nullable YogaNode yogaRoot = layoutResult != null ? layoutResult.getYogaNode() : null;

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (yogaRoot == null) {
      return null;
    }

    if (isLayoutDirectionInherit() && isLayoutDirectionRTL(c.getAndroidContext())) {
      yogaRoot.setDirection(YogaDirection.RTL);
    }
    if (YogaConstants.isUndefined(yogaRoot.getWidth().value)) {
      Layout.setStyleWidthFromSpec(yogaRoot, widthSpec);
    }
    if (YogaConstants.isUndefined(yogaRoot.getHeight().value)) {
      Layout.setStyleHeightFromSpec(yogaRoot, heightSpec);
    }

    final float width =
        SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(widthSpec);
    final float height =
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(heightSpec);

    if (isTracing) {
      ComponentsSystrace.beginSection("yogaCalculateLayout:" + getHeadComponent().getSimpleName());
    }

    yogaRoot.calculateLayout(width, height);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return layoutResult;
  }

  public void child(
      ResolveStateContext resolveStateContext, ComponentContext c, @Nullable Component child) {
    if (child != null) {
      child(Resolver.resolve(resolveStateContext, c, child));
    }
  }

  public void child(@Nullable LithoNode child) {
    if (child != null) {
      addChildAt(child, mChildren.size());
    }
  }

  public void duplicateParentState(boolean duplicateParentState) {
    mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
    mDuplicateParentState = duplicateParentState;
  }

  public void duplicateChildrenStates(boolean duplicateChildrenStates) {
    mPrivateFlags |= PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET;
    mDuplicateChildrenStates = duplicateChildrenStates;
  }

  public void flexDirection(YogaFlexDirection direction) {
    mFlexDirection = direction;
  }

  public void focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    mPrivateFlags |= PFLAG_FOCUSED_HANDLER_IS_SET;
    mFocusedHandler = addVisibilityHandler(mFocusedHandler, focusedHandler);
  }

  public void foreground(@Nullable Drawable foreground) {
    mPrivateFlags |= PFLAG_FOREGROUND_IS_SET;
    mForeground = foreground;
  }

  public void foregroundColor(@ColorInt int foregroundColor) {
    foreground(ComparableColorDrawable.create(foregroundColor));
  }

  public void foregroundRes(final Context context, @DrawableRes int resId) {
    if (resId == 0) {
      foreground(null);
    } else {
      foreground(ContextCompat.getDrawable(context, resId));
    }
  }

  public void layerType(final @LayerType int type, @Nullable final Paint paint) {
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      mLayerType = type;
      mLayerPaint = paint;
    }
  }

  public int getLayerType() {
    return mLayerType;
  }

  public @Nullable Paint getLayerPaint() {
    return mLayerPaint;
  }

  public void fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    mPrivateFlags |= PFLAG_FULL_IMPRESSION_HANDLER_IS_SET;
    mFullImpressionHandler = addVisibilityHandler(mFullImpressionHandler, fullImpressionHandler);
  }

  public int[] getBorderColors() {
    return mBorderColors;
  }

  public @Nullable PathEffect getBorderPathEffect() {
    return mBorderPathEffect;
  }

  public float[] getBorderRadius() {
    return mBorderRadius;
  }

  public LithoNode getChildAt(int index) {
    return mChildren.get(index);
  }

  public int getChildCount() {
    return mChildren.size();
  }

  public int getChildIndex(LithoNode child) {
    for (int i = 0, count = mChildren.size(); i < count; i++) {
      if (mChildren.get(i) == child) {
        return i;
      }
    }
    return -1;
  }

  public List<ScopedComponentInfo> getScopedComponentInfos() {
    return mScopedComponentInfos;
  }

  public @Nullable List<Component> getUnresolvedComponents() {
    return mUnresolvedComponents;
  }

  public @Nullable Map<String, ScopedComponentInfo>
      getScopedComponentInfosNeedingPreviousRenderData() {
    return mScopedComponentInfosNeedingPreviousRenderData;
  }

  public @Nullable EventHandler<FocusedVisibleEvent> getFocusedHandler() {
    return mFocusedHandler;
  }

  public @Nullable Drawable getForeground() {
    return mForeground;
  }

  public @Nullable EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler() {
    return mFullImpressionHandler;
  }

  public Component getHeadComponent() {
    return mScopedComponentInfos.get(mScopedComponentInfos.size() - 1).getComponent();
  }

  public String getHeadComponentKey() {
    return mScopedComponentInfos.get(mScopedComponentInfos.size() - 1).getContext().getGlobalKey();
  }

  public ComponentContext getHeadComponentContext() {
    return mScopedComponentInfos.get(mScopedComponentInfos.size() - 1).getContext();
  }

  public int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  public @Nullable EventHandler<InvisibleEvent> getInvisibleHandler() {
    return mInvisibleHandler;
  }

  public @Nullable NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  public NodeInfo mutableNodeInfo() {
    if (!mNodeInfoWasWritten) {
      mNodeInfoWasWritten = true;
      NodeInfo nodeInfo = new NodeInfo();
      if (mNodeInfo != null) {
        mNodeInfo.copyInto(nodeInfo);
      }
      mNodeInfo = nodeInfo;
    } else {
      if (mNodeInfo == null) {
        // In theory this will not happen, just to avoid any lint warnings from static analysis
        mNodeInfo = new NodeInfo();
      }
    }
    return mNodeInfo;
  }

  public void applyNodeInfo(@Nullable NodeInfo nodeInfo) {
    if (nodeInfo != null) {
      if (mNodeInfoWasWritten || mNodeInfo != null) {
        nodeInfo.copyInto(mutableNodeInfo());
      } else {
        mNodeInfo = nodeInfo;
      }
    }
  }

  public Component getTailComponent() {
    return mScopedComponentInfos.get(0).getComponent();
  }

  public String getTailComponentKey() {
    return mScopedComponentInfos.get(0).getContext().getGlobalKey();
  }

  public ComponentContext getTailComponentContext() {
    return mScopedComponentInfos.get(0).getContext();
  }

  public ScopedComponentInfo getTailScopedComponentInfo() {
    return mScopedComponentInfos.get(0);
  }

  public ScopedComponentInfo getComponentInfoAt(int index) {
    return mScopedComponentInfos.get(index);
  }

  public ComponentContext getComponentContextAt(int index) {
    return getComponentInfoAt(index).getContext();
  }

  public Component getComponentAt(int index) {
    return getComponentInfoAt(index).getComponent();
  }

  public String getGlobalKeyAt(int index) {
    return getComponentContextAt(index).getGlobalKey();
  }

  public int getComponentCount() {
    return mScopedComponentInfos.size();
  }

  public @Nullable List<Attachable> getAttachables() {
    return mAttachables;
  }

  public @Nullable StateListAnimator getStateListAnimator() {
    return mStateListAnimator;
  }

  public @DrawableRes int getStateListAnimatorRes() {
    return mStateListAnimatorRes;
  }

  public void setNestedPadding(@Nullable Edges padding, @Nullable boolean[] isPercentage) {
    mNestedPaddingEdges = padding;
    mNestedIsPaddingPercent = isPercentage;
  }

  public LayoutProps getDebugLayoutEditor() {
    if (ComponentsConfiguration.isDebugModeEnabled) {
      mDebugLayoutProps = new CommonProps.DefaultLayoutProps();
      return mDebugLayoutProps;
    }
    return null;
  }

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  public @Nullable String getTestKey() {
    return mTestKey;
  }

  public @Nullable Edges getTouchExpansion() {
    return mTouchExpansion;
  }

  public @Nullable String getTransitionKey() {
    return mTransitionKey;
  }

  public @Nullable String getTransitionOwnerKey() {
    return mTransitionOwnerKey;
  }

  public @Nullable Transition.TransitionKeyType getTransitionKeyType() {
    return mTransitionKeyType;
  }

  public @Nullable ArrayList<Transition> getTransitions() {
    return mTransitions;
  }

  public String getTransitionGlobalKey() {
    return getTailComponentKey();
  }

  public @Nullable EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler() {
    return mUnfocusedHandler;
  }

  public @Nullable EventHandler<VisibilityChangedEvent> getVisibilityChangedHandler() {
    return mVisibilityChangedHandler;
  }

  public @Nullable EventHandler<VisibleEvent> getVisibleHandler() {
    return mVisibleHandler;
  }

  public float getVisibleHeightRatio() {
    return mVisibleHeightRatio;
  }

  public float getVisibleWidthRatio() {
    return mVisibleWidthRatio;
  }

  public @Nullable ArrayList<WorkingRangeContainer.Registration> getWorkingRangeRegistrations() {
    return mWorkingRangeRegistrations;
  }

  public boolean hasBorderColor() {
    for (int color : mBorderColors) {
      if (color != Color.TRANSPARENT) {
        return true;
      }
    }

    return false;
  }

  public boolean hasStateListAnimatorResSet() {
    return (mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_RES_SET) != 0;
  }

  public boolean hasTouchExpansion() {
    return ((mPrivateFlags & PFLAG_TOUCH_EXPANSION_IS_SET) != 0L);
  }

  public boolean hasTransitionKey() {
    return !TextUtils.isEmpty(mTransitionKey);
  }

  public boolean hasVisibilityHandlers() {
    return mVisibleHandler != null
        || mFocusedHandler != null
        || mUnfocusedHandler != null
        || mFullImpressionHandler != null
        || mInvisibleHandler != null
        || mVisibilityChangedHandler != null;
  }

  public LithoNode importantForAccessibility(int importantForAccessibility) {
    mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
    mImportantForAccessibility = importantForAccessibility;
    return this;
  }

  public LithoNode invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    mPrivateFlags |= PFLAG_INVISIBLE_HANDLER_IS_SET;
    mInvisibleHandler = addVisibilityHandler(mInvisibleHandler, invisibleHandler);
    return this;
  }

  public boolean isDuplicateParentStateEnabled() {
    return mDuplicateParentState;
  }

  public boolean isDuplicateChildrenStatesEnabled() {
    return mDuplicateChildrenStates;
  }

  public boolean isForceViewWrapping() {
    return mForceViewWrapping;
  }

  public boolean isImportantForAccessibilityIsSet() {
    return (mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) == 0L
        || mImportantForAccessibility == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  }

  public boolean isLayoutDirectionInherit() {
    return mLayoutDirection == null || mLayoutDirection == YogaDirection.INHERIT;
  }

  public void justifyContent(YogaJustify justifyContent) {
    mJustifyContent = justifyContent;
  }

  public void layoutDirection(YogaDirection direction) {
    mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
    mLayoutDirection = direction;
  }

  public void registerDebugComponent(DebugComponent debugComponent) {
    if (mDebugComponents == null) {
      mDebugComponents = new HashSet<>();
    }
    mDebugComponents.add(debugComponent);
  }

  public void setMeasureFunction(YogaMeasureFunction measureFunction) {
    mYogaMeasureFunction = measureFunction;
  }

  public void stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_SET;
    mStateListAnimator = stateListAnimator;
    wrapInView();
  }

  public void stateListAnimatorRes(@DrawableRes int resId) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_RES_SET;
    mStateListAnimatorRes = resId;
    wrapInView();
  }

  public void testKey(@Nullable String testKey) {
    mTestKey = testKey;
  }

  public void touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    if (mTouchExpansion == null) {
      mTouchExpansion = new Edges();
    }

    mPrivateFlags |= PFLAG_TOUCH_EXPANSION_IS_SET;
    mTouchExpansion.set(edge, touchExpansion);
  }

  public void transitionKey(@Nullable String key, @Nullable String ownerKey) {
    if (SDK_INT >= ICE_CREAM_SANDWICH && !TextUtils.isEmpty(key)) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
      mTransitionKey = key;
      mTransitionOwnerKey = ownerKey;
    }
  }

  public void transitionKeyType(@Nullable Transition.TransitionKeyType type) {
    mPrivateFlags |= PFLAG_TRANSITION_KEY_TYPE_IS_SET;
    mTransitionKeyType = type;
  }

  public void unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    mPrivateFlags |= PFLAG_UNFOCUSED_HANDLER_IS_SET;
    mUnfocusedHandler = addVisibilityHandler(mUnfocusedHandler, unfocusedHandler);
  }

  public void visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET;
    mVisibilityChangedHandler =
        addVisibilityHandler(mVisibilityChangedHandler, visibilityChangedHandler);
  }

  public void visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_HANDLER_IS_SET;
    mVisibleHandler = addVisibilityHandler(mVisibleHandler, visibleHandler);
  }

  public void visibleHeightRatio(float visibleHeightRatio) {
    mVisibleHeightRatio = visibleHeightRatio;
  }

  public void visibleWidthRatio(float visibleWidthRatio) {
    mVisibleWidthRatio = visibleWidthRatio;
  }

  public void wrap(YogaWrap wrap) {
    mYogaWrap = wrap;
  }

  public void wrapInView() {
    mForceViewWrapping = true;
  }

  public @Nullable Drawable getBackground() {
    return mBackground;
  }

  public void applyAttributes(Context c, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    final TypedArray a =
        c.obtainStyledAttributes(
            null, com.facebook.litho.R.styleable.ComponentLayout, defStyleAttr, defStyleRes);

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_importantForAccessibility
          && SDK_INT >= JELLY_BEAN) {
        importantForAccessibility(a.getInt(attr, 0));
      } else if (attr
          == com.facebook.litho.R.styleable.ComponentLayout_android_duplicateParentState) {
        duplicateParentState(a.getBoolean(attr, false));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_background) {
        if (TypedArrayUtils.isColorAttribute(
            a, com.facebook.litho.R.styleable.ComponentLayout_android_background)) {
          backgroundColor(a.getColor(attr, 0));
        } else {
          backgroundRes(c, a.getResourceId(attr, -1));
        }
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_foreground) {
        if (TypedArrayUtils.isColorAttribute(
            a, com.facebook.litho.R.styleable.ComponentLayout_android_foreground)) {
          foregroundColor(a.getColor(attr, 0));
        } else {
          foregroundRes(c, a.getResourceId(attr, -1));
        }
      } else if (attr
          == com.facebook.litho.R.styleable.ComponentLayout_android_contentDescription) {
        mutableNodeInfo().setContentDescription(a.getString(attr));
      }
    }

    a.recycle();
  }

  protected static void applyLayoutStyleAttributes(YogaLayoutProps props, TypedArray a) {
    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_width) {
        int width = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (width >= 0) {
          props.widthPx(width);
        }
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_height) {
        int height = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (height >= 0) {
          props.heightPx(height);
        }
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_minHeight) {
        props.minHeightPx(a.getDimensionPixelSize(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_minWidth) {
        props.minWidthPx(a.getDimensionPixelSize(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingLeft) {
        props.paddingPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingTop) {
        props.paddingPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingRight) {
        props.paddingPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingBottom) {
        props.paddingPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingStart
          && SUPPORTS_RTL) {
        props.paddingPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingEnd
          && SUPPORTS_RTL) {
        props.paddingPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_padding) {
        props.paddingPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginLeft) {
        props.marginPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginTop) {
        props.marginPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr
          == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginRight) {
        props.marginPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr
          == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginBottom) {
        props.marginPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginStart
          && SUPPORTS_RTL) {
        props.marginPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginEnd
          && SUPPORTS_RTL) {
        props.marginPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_margin) {
        props.marginPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_direction) {
        props.flexDirection(YogaFlexDirection.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_wrap) {
        props.wrap(YogaWrap.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_justifyContent) {
        props.justifyContent(YogaJustify.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_alignItems) {
        props.alignItems(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_alignSelf) {
        props.alignSelf(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_positionType) {
        props.positionType(YogaPositionType.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_layoutDirection) {
        final int layoutDirection = a.getInteger(attr, -1);
        props.layoutDirection(YogaDirection.fromInt(layoutDirection));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex) {
        final float flex = a.getFloat(attr, -1);
        if (flex >= 0f) {
          props.flex(flex);
        }
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_left) {
        props.positionPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_top) {
        props.positionPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_right) {
        props.positionPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_bottom) {
        props.positionPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      }
    }
  }

  public String getSimpleName() {
    return mScopedComponentInfos.isEmpty() ? "<null>" : getComponentAt(0).getSimpleName();
  }

  public boolean isClone() {
    return mIsClone;
  }

  protected LithoNode clone() {
    final LithoNode node;
    try {
      node = (LithoNode) super.clone();
      node.mIsClone = true;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }

    return node;
  }

  private @Nullable static <T> EventHandler<T> addVisibilityHandler(
      @Nullable EventHandler<T> currentHandler, @Nullable EventHandler<T> newHandler) {
    if (currentHandler == null) {
      return newHandler;
    }
    if (newHandler == null) {
      return currentHandler;
    }
    if (currentHandler instanceof DelegatingEventHandler) {
      DelegatingEventHandler<T> delegatingEventHandler = (DelegatingEventHandler<T>) currentHandler;
      return delegatingEventHandler.addEventHandler(newHandler);
    } else {
      return new DelegatingEventHandler<>(currentHandler, newHandler);
    }
  }

  public void setNestedTreeHolder(@Nullable NestedTreeHolder holder) {
    mNestedTreeHolder = holder;
  }

  protected @Nullable YogaLayoutProps createYogaNodeWriter() {
    return new YogaLayoutProps(NodeConfig.createYogaNode());
  }

  void writeToYogaNode(YogaLayoutProps writer) {
    YogaNode node = writer.getNode();

    // Apply the extra layout props
    if (mLayoutDirection != null) {
      node.setDirection(mLayoutDirection);
    }

    if (mFlexDirection != null) {
      node.setFlexDirection(mFlexDirection);
    }
    if (mJustifyContent != null) {
      node.setJustifyContent(mJustifyContent);
    }
    if (mAlignContent != null) {
      node.setAlignContent(mAlignContent);
    }
    if (mAlignItems != null) {
      node.setAlignItems(mAlignItems);
    }
    if (mYogaWrap != null) {
      node.setWrap(mYogaWrap);
    }
    if (mYogaMeasureFunction != null) {
      node.setMeasureFunction(mYogaMeasureFunction);
    }

    // Apply the layout props from the components to the YogaNode
    for (ScopedComponentInfo info : mScopedComponentInfos) {
      final Component component = info.getComponent();
      // If a NestedTreeHolder is set then transfer its resolved props into this LithoNode.
      if (mNestedTreeHolder != null && isLayoutSpecWithSizeSpec(component)) {
        mNestedTreeHolder.transferInto(this);
        if (mPaddingFromBackground != null) {
          setPaddingFromDrawable(writer, mPaddingFromBackground);
        }
      } else {
        final CommonProps props = component.getCommonProps();
        if (props != null) {
          final int styleAttr = props.getDefStyleAttr();
          final int styleRes = props.getDefStyleRes();
          if (styleAttr != 0 || styleRes != 0) {
            final Context context = getTailComponentContext().getAndroidContext();
            final TypedArray a =
                context.obtainStyledAttributes(
                    null, com.facebook.litho.R.styleable.ComponentLayout, styleAttr, styleRes);
            applyLayoutStyleAttributes(writer, a);
            a.recycle();
          }

          // Set the padding from the background
          final @Nullable Rect padding = props.getPaddingFromBackground();
          if (padding != null) {
            setPaddingFromDrawable(writer, padding);
          }

          // Copy the layout props into this LithoNode.
          props.copyLayoutProps(writer);
        }
      }
    }

    // Apply the border widths
    if ((mPrivateFlags & PFLAG_BORDER_IS_SET) != 0L) {
      for (int i = 0, length = mBorderEdgeWidths.length; i < length; ++i) {
        writer.setBorderWidth(Border.edgeFromIndex(i), mBorderEdgeWidths[i]);
      }
    }

    // Maybe apply the padding if parent is a Nested Tree Holder
    if (mNestedPaddingEdges != null) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        float value = mNestedPaddingEdges.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          final YogaEdge edge = YogaEdge.fromInt(i);
          if (mNestedIsPaddingPercent != null && mNestedIsPaddingPercent[edge.intValue()]) {
            writer.paddingPercent(edge, value);
          } else {
            writer.paddingPx(edge, (int) value);
          }
        }
      }
    }

    if (mDebugLayoutProps != null) {
      mDebugLayoutProps.copyInto(writer);
    }

    mIsPaddingSet = writer.isPaddingSet;
  }

  LithoLayoutResult createLayoutResult(final YogaNode node) {
    return new LithoLayoutResult(getTailComponentContext(), this, node);
  }

  protected static void setPaddingFromDrawable(YogaLayoutProps target, Rect padding) {
    target.paddingPx(LEFT, padding.left);
    target.paddingPx(TOP, padding.top);
    target.paddingPx(RIGHT, padding.right);
    target.paddingPx(BOTTOM, padding.bottom);
  }

  boolean isPaddingSet() {
    return mIsPaddingSet;
  }

  void setChildren(List<LithoNode> children) {
    mChildren = children;
  }

  void resetDebugInfo() {
    mDebugComponents = null;
  }

  private static void applyOverridesRecursive(LayoutStateContext c, LithoNode node) {
    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(node.getTailComponentContext(), node);
      for (int i = 0, count = node.getChildCount(); i < count; i++) {
        applyOverridesRecursive(c, node.getChildAt(i));
      }
    }
  }

  /**
   * This utility method checks if the {@param result} will mount a {@link View}. It returns true if
   * and only if the {@param result} will mount a {@link View}. If it returns {@code false} then the
   * result will either mount a {@link Drawable} or it is {@link NestedTreeHolderResult}, which will
   * not mount anything.
   *
   * @return {@code true} iff the result will mount a view.
   */
  public static boolean willMountView(LithoNode node) {
    if (node.getMountable() != null) {
      return node.getMountable().getRenderType() == RenderUnit.RenderType.VIEW;
    } else if (node.getPrimitive() != null) {
      return node.getPrimitive().getRenderUnit().getRenderType() == RenderUnit.RenderType.VIEW;
    } else {
      final Component component = node.getTailComponent();
      return (component != null && component.getMountType() == Component.MountType.VIEW);
    }
  }
}
