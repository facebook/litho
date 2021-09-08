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

import android.animation.StateListAnimator;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.drawable.DrawableUtils;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import java.util.ArrayList;
import java.util.List;

/** Internal class that holds props that are common to all {@link Component}s. */
@ThreadConfined(ThreadConfined.ANY)
class CommonPropsHolder implements CommonProps {

  // Flags used to indicate that a certain attribute was explicitly set on the node.
  private static final byte PFLAG_BACKGROUND_IS_SET = 1 << 0;
  private static final byte PFLAG_TEST_KEY_IS_SET = 1 << 1;

  private static final byte PFLAG_SCALE_KEY_IS_SET = 1 << 2;
  private static final byte PFLAG_ALPHA_KEY_IS_SET = 1 << 3;
  private static final byte PFLAG_ROTATION_KEY_IS_SET = 1 << 4;

  private byte mPrivateFlags;
  @Nullable private OtherProps mOtherProps;
  @Nullable private NodeInfo mNodeInfo;
  @Nullable private CopyableLayoutProps mLayoutProps;
  @Nullable private Drawable mBackground;
  @Nullable private String mTestKey;
  @Nullable private Object mComponentTag;
  private boolean mWrapInView;
  @AttrRes private int mDefStyleAttr;
  @StyleRes private int mDefStyleRes;

  private OtherProps getOrCreateOtherProps() {
    if (mOtherProps == null) {
      mOtherProps = new OtherProps();
    }

    return mOtherProps;
  }

  private LayoutProps getOrCreateLayoutProps() {
    if (mLayoutProps == null) {
      mLayoutProps = new DefaultLayoutProps();
    }

    return mLayoutProps;
  }

  @Override
  public void setStyle(@AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    mDefStyleAttr = defStyleAttr;
    mDefStyleRes = defStyleRes;
  }

  @Override
  public void positionType(@Nullable YogaPositionType positionType) {
    getOrCreateLayoutProps().positionType(positionType);
  }

  @Override
  public void positionPx(YogaEdge edge, @Px int position) {
    getOrCreateLayoutProps().positionPx(edge, position);
  }

  @Override
  public void widthPx(@Px int width) {
    getOrCreateLayoutProps().widthPx(width);
  }

  @Override
  public void heightPx(@Px int height) {
    getOrCreateLayoutProps().heightPx(height);
  }

  @Override
  public void background(@Nullable Drawable background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
  }

  @Override
  public void testKey(@Nullable String testKey) {
    mPrivateFlags |= PFLAG_TEST_KEY_IS_SET;
    mTestKey = testKey;
  }

  @Override
  @Nullable
  public String getTestKey() {
    return (mPrivateFlags & PFLAG_TEST_KEY_IS_SET) != 0 ? mTestKey : null;
  }

  @Override
  @Nullable
  public Object getComponentTag() {
    return mComponentTag;
  }

  @Override
  public void componentTag(@Nullable Object componentTag) {
    mComponentTag = componentTag;
  }

  @Override
  public void wrapInView() {
    mWrapInView = true;
  }

  private boolean shouldWrapInView() {
    return mWrapInView
        || (mPrivateFlags
                & (PFLAG_SCALE_KEY_IS_SET | PFLAG_ALPHA_KEY_IS_SET | PFLAG_ROTATION_KEY_IS_SET))
            != 0L;
  }

  @Override
  public void layoutDirection(YogaDirection direction) {
    getOrCreateLayoutProps().layoutDirection(direction);
  }

  @Override
  public void alignSelf(YogaAlign alignSelf) {
    getOrCreateLayoutProps().alignSelf(alignSelf);
  }

  @Override
  public void flex(float flex) {
    getOrCreateLayoutProps().flex(flex);
  }

  @Override
  public void flexGrow(float flexGrow) {
    getOrCreateLayoutProps().flexGrow(flexGrow);
  }

  @Override
  public void flexShrink(float flexShrink) {
    getOrCreateLayoutProps().flexShrink(flexShrink);
  }

  @Override
  public void flexBasisPx(@Px int flexBasis) {
    getOrCreateLayoutProps().flexBasisPx(flexBasis);
  }

  @Override
  public void flexBasisPercent(float percent) {
    getOrCreateLayoutProps().flexBasisPercent(percent);
  }

  @Override
  public void importantForAccessibility(int importantForAccessibility) {
    getOrCreateOtherProps().importantForAccessibility(importantForAccessibility);
  }

  @Override
  public void duplicateParentState(boolean duplicateParentState) {
    getOrCreateOtherProps().duplicateParentState(duplicateParentState);
  }

  @Override
  public void duplicateChildrenStates(boolean duplicateChildrenStates) {
    getOrCreateOtherProps().duplicateChildrenStates(duplicateChildrenStates);
  }

  @Override
  public void marginPx(YogaEdge edge, @Px int margin) {
    getOrCreateLayoutProps().marginPx(edge, margin);
  }

  @Override
  public void marginPercent(YogaEdge edge, float percent) {
    getOrCreateLayoutProps().marginPercent(edge, percent);
  }

  @Override
  public void marginAuto(YogaEdge edge) {
    getOrCreateLayoutProps().marginAuto(edge);
  }

  @Override
  public void paddingPx(YogaEdge edge, @Px int padding) {
    getOrCreateLayoutProps().paddingPx(edge, padding);
  }

  @Override
  public void paddingPercent(YogaEdge edge, float percent) {
    getOrCreateLayoutProps().paddingPercent(edge, percent);
  }

  @Override
  public void border(@Nullable Border border) {
    getOrCreateOtherProps().border(border);
  }

  @Override
  public void stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    getOrCreateOtherProps().stateListAnimator(stateListAnimator);
  }

  @Override
  public void stateListAnimatorRes(@DrawableRes int resId) {
    getOrCreateOtherProps().stateListAnimatorRes(resId);
  }

  @Override
  public void positionPercent(YogaEdge edge, float percent) {
    getOrCreateLayoutProps().positionPercent(edge, percent);
  }

  @Override
  public void widthPercent(float percent) {
    getOrCreateLayoutProps().widthPercent(percent);
  }

  @Override
  public void minWidthPx(@Px int minWidth) {
    getOrCreateLayoutProps().minWidthPx(minWidth);
  }

  @Override
  public void minWidthPercent(float percent) {
    getOrCreateLayoutProps().minWidthPercent(percent);
  }

  @Override
  public void maxWidthPx(@Px int maxWidth) {
    getOrCreateLayoutProps().maxWidthPx(maxWidth);
  }

  @Override
  public void maxWidthPercent(float percent) {
    getOrCreateLayoutProps().maxWidthPercent(percent);
  }

  @Override
  public void heightPercent(float percent) {
    getOrCreateLayoutProps().heightPercent(percent);
  }

  @Override
  public void minHeightPx(@Px int minHeight) {
    getOrCreateLayoutProps().minHeightPx(minHeight);
  }

  @Override
  public void minHeightPercent(float percent) {
    getOrCreateLayoutProps().minHeightPercent(percent);
  }

  @Override
  public void maxHeightPx(@Px int maxHeight) {
    getOrCreateLayoutProps().maxHeightPx(maxHeight);
  }

  @Override
  public void maxHeightPercent(float percent) {
    getOrCreateLayoutProps().maxHeightPercent(percent);
  }

  @Override
  public void aspectRatio(float aspectRatio) {
    getOrCreateLayoutProps().aspectRatio(aspectRatio);
  }

  @Override
  public void isReferenceBaseline(boolean isReferenceBaseline) {
    getOrCreateLayoutProps().isReferenceBaseline(isReferenceBaseline);
  }

  @Override
  public void useHeightAsBaseline(boolean useHeightAsBaseline) {
    getOrCreateLayoutProps().useHeightAsBaseline(useHeightAsBaseline);
  }

  @Override
  public void heightAuto() {
    getOrCreateLayoutProps().heightAuto();
  }

  @Override
  public void widthAuto() {
    getOrCreateLayoutProps().widthAuto();
  }

  @Override
  public void flexBasisAuto() {
    getOrCreateLayoutProps().flexBasisAuto();
  }

  /** Used by {@link DebugLayoutNodeEditor} */
  @Override
  public void setBorderWidth(YogaEdge edge, float borderWidth) {
    getOrCreateLayoutProps().setBorderWidth(edge, borderWidth);
  }

  @Override
  public void touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    getOrCreateOtherProps().touchExpansionPx(edge, touchExpansion);
  }

  @Override
  public void foreground(@Nullable Drawable foreground) {
    getOrCreateOtherProps().foreground(foreground);
  }

  @Override
  public void clickHandler(@Nullable EventHandler<ClickEvent> clickHandler) {
    getOrCreateNodeInfo().setClickHandler(clickHandler);
  }

  @Override
  @Nullable
  public Drawable getBackground() {
    return mBackground;
  }

  @Override
  @Nullable
  public EventHandler<ClickEvent> getClickHandler() {
    return getOrCreateNodeInfo().getClickHandler();
  }

  @Override
  public void longClickHandler(@Nullable EventHandler<LongClickEvent> longClickHandler) {
    getOrCreateNodeInfo().setLongClickHandler(longClickHandler);
  }

  @Override
  @Nullable
  public EventHandler<LongClickEvent> getLongClickHandler() {
    return getOrCreateNodeInfo().getLongClickHandler();
  }

  @Override
  public void focusChangeHandler(@Nullable EventHandler<FocusChangedEvent> focusChangeHandler) {
    getOrCreateNodeInfo().setFocusChangeHandler(focusChangeHandler);
  }

  @Override
  @Nullable
  public EventHandler<FocusChangedEvent> getFocusChangeHandler() {
    return getOrCreateNodeInfo().getFocusChangeHandler();
  }

  @Override
  public void touchHandler(@Nullable EventHandler<TouchEvent> touchHandler) {
    getOrCreateNodeInfo().setTouchHandler(touchHandler);
  }

  @Override
  @Nullable
  public EventHandler<TouchEvent> getTouchHandler() {
    return getOrCreateNodeInfo().getTouchHandler();
  }

  @Override
  public void interceptTouchHandler(
      @Nullable EventHandler<InterceptTouchEvent> interceptTouchHandler) {
    getOrCreateNodeInfo().setInterceptTouchHandler(interceptTouchHandler);
  }

  @Override
  @Nullable
  public EventHandler<InterceptTouchEvent> getInterceptTouchHandler() {
    return getOrCreateNodeInfo().getInterceptTouchHandler();
  }

  @Override
  public void focusable(boolean isFocusable) {
    getOrCreateNodeInfo().setFocusable(isFocusable);
  }

  @Override
  public boolean getFocusable() {
    return getOrCreateNodeInfo().getFocusState() == NodeInfo.FOCUS_SET_TRUE;
  }

  @Override
  public void clickable(boolean isClickable) {
    getOrCreateNodeInfo().setClickable(isClickable);
  }

  @Override
  public void enabled(boolean isEnabled) {
    getOrCreateNodeInfo().setEnabled(isEnabled);
  }

  @Override
  public void selected(boolean isSelected) {
    getOrCreateNodeInfo().setSelected(isSelected);
  }

  @Override
  public void accessibilityHeading(boolean isHeading) {
    getOrCreateNodeInfo().setAccessibilityHeading(isHeading);
  }

  @Override
  public void visibleHeightRatio(float visibleHeightRatio) {
    getOrCreateOtherProps().visibleHeightRatio(visibleHeightRatio);
  }

  @Override
  public void visibleWidthRatio(float visibleWidthRatio) {
    getOrCreateOtherProps().visibleWidthRatio(visibleWidthRatio);
  }

  @Override
  public void visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
    getOrCreateOtherProps().visibleHandler(visibleHandler);
  }

  @Override
  public void focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    getOrCreateOtherProps().focusedHandler(focusedHandler);
  }

  @Override
  public void unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    getOrCreateOtherProps().unfocusedHandler(unfocusedHandler);
  }

  @Override
  public void fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    getOrCreateOtherProps().fullImpressionHandler(fullImpressionHandler);
  }

  @Override
  public void invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    getOrCreateOtherProps().invisibleHandler(invisibleHandler);
  }

  @Override
  public void visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    getOrCreateOtherProps().visibilityChangedHandler(visibilityChangedHandler);
  }

  @Override
  public void contentDescription(@Nullable CharSequence contentDescription) {
    getOrCreateNodeInfo().setContentDescription(contentDescription);
  }

  @Override
  public void viewTag(@Nullable Object viewTag) {
    getOrCreateNodeInfo().setViewTag(viewTag);
  }

  @Override
  public void viewTags(@Nullable SparseArray<Object> viewTags) {
    getOrCreateNodeInfo().setViewTags(viewTags);
  }

  @Override
  public void transitionName(@Nullable String transitionName) {
    getOrCreateNodeInfo().setTransitionName(transitionName);
  }

  @Override
  public void shadowElevationPx(float shadowElevation) {
    getOrCreateNodeInfo().setShadowElevation(shadowElevation);
  }

  @Override
  public void outlineProvider(@Nullable ViewOutlineProvider outlineProvider) {
    getOrCreateNodeInfo().setOutlineProvider(outlineProvider);
  }

  @Override
  public void clipToOutline(boolean clipToOutline) {
    getOrCreateNodeInfo().setClipToOutline(clipToOutline);
  }

  @Override
  public void clipChildren(boolean clipChildren) {
    getOrCreateNodeInfo().setClipChildren(clipChildren);
  }

  @Override
  public void accessibilityRole(@Nullable @AccessibilityRole.AccessibilityRoleType String role) {
    getOrCreateNodeInfo().setAccessibilityRole(role);
  }

  @Override
  public void accessibilityRoleDescription(@Nullable CharSequence roleDescription) {
    getOrCreateNodeInfo().setAccessibilityRoleDescription(roleDescription);
  }

  @Override
  public void dispatchPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<DispatchPopulateAccessibilityEventEvent>
              dispatchPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setDispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
  }

  @Override
  public void onInitializeAccessibilityEventHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
  }

  @Override
  public void onInitializeAccessibilityNodeInfoHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityNodeInfoEvent>
              onInitializeAccessibilityNodeInfoHandler) {
    getOrCreateNodeInfo()
        .setOnInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
  }

  @Override
  public void onPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
  }

  @Override
  public void onRequestSendAccessibilityEventHandler(
      @Nullable
          EventHandler<OnRequestSendAccessibilityEventEvent>
              onRequestSendAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
  }

  @Override
  public void performAccessibilityActionHandler(
      @Nullable EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    getOrCreateNodeInfo().setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
  }

  @Override
  public void sendAccessibilityEventHandler(
      @Nullable EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    getOrCreateNodeInfo().setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
  }

  @Override
  public void sendAccessibilityEventUncheckedHandler(
      @Nullable
          EventHandler<SendAccessibilityEventUncheckedEvent>
              sendAccessibilityEventUncheckedHandler) {
    getOrCreateNodeInfo()
        .setSendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);
  }

  @Override
  public void scale(float scale) {
    getOrCreateNodeInfo().setScale(scale);
    if (scale == 1) {
      mPrivateFlags &= ~PFLAG_SCALE_KEY_IS_SET;
    } else {
      mPrivateFlags |= PFLAG_SCALE_KEY_IS_SET;
    }
  }

  @Override
  public void alpha(float alpha) {
    getOrCreateNodeInfo().setAlpha(alpha);
    if (alpha == 1f) {
      mPrivateFlags &= ~PFLAG_ALPHA_KEY_IS_SET;
    } else {
      mPrivateFlags |= PFLAG_ALPHA_KEY_IS_SET;
    }
  }

  @Override
  public void rotation(float rotation) {
    getOrCreateNodeInfo().setRotation(rotation);
    if (rotation == 0) {
      mPrivateFlags &= ~PFLAG_ROTATION_KEY_IS_SET;
    } else {
      mPrivateFlags |= PFLAG_ROTATION_KEY_IS_SET;
    }
  }

  @Override
  public void rotationX(float rotationX) {
    wrapInView();
    getOrCreateNodeInfo().setRotationX(rotationX);
  }

  @Override
  public void rotationY(float rotationY) {
    wrapInView();
    getOrCreateNodeInfo().setRotationY(rotationY);
  }

  @Override
  public void transitionKey(@Nullable String key, @Nullable String ownerKey) {
    getOrCreateOtherProps().transitionKey(key, ownerKey);
  }

  @Override
  @Nullable
  public String getTransitionKey() {
    return getOrCreateOtherProps().mTransitionKey;
  }

  @Override
  public void transitionKeyType(@Nullable Transition.TransitionKeyType type) {
    getOrCreateOtherProps().transitionKeyType(type);
  }

  @Override
  public void layerType(@LayerType int type, @Nullable Paint paint) {
    getOrCreateOtherProps().layerType(type, paint);
  }

  @Nullable
  @Override
  public Transition.TransitionKeyType getTransitionKeyType() {
    return getOrCreateOtherProps().mTransitionKeyType;
  }

  @Override
  @Nullable
  public NodeInfo getNullableNodeInfo() {
    return mNodeInfo;
  }

  @Override
  public NodeInfo getOrCreateNodeInfo() {
    if (mNodeInfo == null) {
      mNodeInfo = new DefaultNodeInfo();
    }

    return mNodeInfo;
  }

  @Override
  public @Nullable CopyableLayoutProps getLayoutProps() {
    return mLayoutProps;
  }

  @Override
  public int getDefStyleAttr() {
    return mDefStyleAttr;
  }

  @Override
  public int getDefStyleRes() {
    return mDefStyleRes;
  }

  @Override
  public void copyInto(ComponentContext c, InternalNode node) {
    if (c != null) {
      c.applyStyle(node, mDefStyleAttr, mDefStyleRes);
    }

    if (mNodeInfo != null) {
      mNodeInfo.copyInto(node.getOrCreateNodeInfo());
    }

    if ((mPrivateFlags & PFLAG_BACKGROUND_IS_SET) != 0L) {
      node.background(mBackground);
    }
    if ((mPrivateFlags & PFLAG_TEST_KEY_IS_SET) != 0L) {
      node.testKey(mTestKey);
    }

    if (shouldWrapInView()) {
      node.wrapInView();
    }

    // InternalNode which implement LayoutProps should greedily transfer layout props
    if (mLayoutProps != null && node instanceof LayoutProps) {
      mLayoutProps.copyInto((LayoutProps) node);
    }

    if (mOtherProps != null) {
      mOtherProps.copyInto(node);
    }
  }

  @Override
  public boolean isEquivalentTo(CommonProps o) {
    if (this == o) {
      return true;
    }

    if (o == null) {
      return false;
    }

    if (!(o instanceof CommonPropsHolder)) {
      return false;
    }

    CommonPropsHolder other = (CommonPropsHolder) o;

    return mPrivateFlags == other.mPrivateFlags
        && mWrapInView == other.mWrapInView
        && mDefStyleAttr == other.mDefStyleAttr
        && mDefStyleRes == other.mDefStyleRes
        && DrawableUtils.isEquivalentTo(mBackground, other.mBackground)
        && CommonUtils.isEquivalentTo(mOtherProps, other.mOtherProps)
        && CommonUtils.isEquivalentTo(mNodeInfo, other.mNodeInfo)
        && CommonUtils.isEquivalentTo(mLayoutProps, other.mLayoutProps)
        && CommonUtils.equals(mTestKey, other.mTestKey)
        && CommonUtils.equals(mComponentTag, other.mComponentTag);
  }

  private static class OtherProps implements Equivalence<OtherProps> {
    // Flags used to indicate that a certain attribute was explicitly set on the node.
    private static final int PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1 << 0;
    private static final int PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1 << 1;
    private static final int PFLAG_FOREGROUND_IS_SET = 1 << 2;
    private static final int PFLAG_VISIBLE_HANDLER_IS_SET = 1 << 3;
    private static final int PFLAG_FOCUSED_HANDLER_IS_SET = 1 << 4;
    private static final int PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1 << 5;
    private static final int PFLAG_INVISIBLE_HANDLER_IS_SET = 1 << 6;
    private static final int PFLAG_UNFOCUSED_HANDLER_IS_SET = 1 << 7;
    private static final int PFLAG_TOUCH_EXPANSION_IS_SET = 1 << 8;
    private static final int PFLAG_TRANSITION_KEY_IS_SET = 1 << 9;
    private static final int PFLAG_WRAP_IN_VIEW_IS_SET = 1 << 10;
    private static final int PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET = 1 << 11;
    private static final int PFLAG_VISIBLE_WIDTH_RATIO_IS_SET = 1 << 12;
    private static final int PFLAG_BORDER_IS_SET = 1 << 13;
    private static final int PFLAG_STATE_LIST_ANIMATOR_IS_SET = 1 << 14;
    private static final int PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET = 1 << 15;
    private static final int PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET = 1 << 16;
    private static final int PFLAG_TRANSITION_KEY_TYPE_IS_SET = 1 << 17;
    private static final int PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET = 1 << 18;

    private int mPrivateFlags;

    private float mVisibleHeightRatio;
    private float mVisibleWidthRatio;
    @Nullable private EventHandler<VisibleEvent> mVisibleHandler;
    @Nullable private EventHandler<FocusedVisibleEvent> mFocusedHandler;
    @Nullable private EventHandler<UnfocusedVisibleEvent> mUnfocusedHandler;
    @Nullable private EventHandler<FullImpressionVisibleEvent> mFullImpressionHandler;
    @Nullable private EventHandler<InvisibleEvent> mInvisibleHandler;
    @Nullable private EventHandler<VisibilityChangedEvent> mVisibilityChangedHandler;
    private int mImportantForAccessibility;
    private boolean mDuplicateParentState;
    private boolean mDuplicateChildrenStates;
    @Nullable private Edges mTouchExpansions;
    @Nullable private Drawable mForeground;
    @Nullable private String mTransitionOwnerKey;
    @Nullable private String mTransitionKey;
    @Nullable private Transition.TransitionKeyType mTransitionKeyType;
    @Nullable private Border mBorder;
    @Nullable private StateListAnimator mStateListAnimator;
    @DrawableRes private int mStateListAnimatorRes;
    private int mLayerType = LayerType.LAYER_TYPE_NOT_SET;
    private @Nullable Paint mLayerPaint;

    private void importantForAccessibility(int importantForAccessibility) {
      mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
      mImportantForAccessibility = importantForAccessibility;
    }

    private void duplicateParentState(boolean duplicateParentState) {
      mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
      mDuplicateParentState = duplicateParentState;
    }

    private void duplicateChildrenStates(boolean duplicateChildrenStates) {
      mPrivateFlags |= PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET;
      mDuplicateChildrenStates = duplicateChildrenStates;
    }

    private void border(@Nullable Border border) {
      if (border != null) {
        mPrivateFlags |= PFLAG_BORDER_IS_SET;
        mBorder = border;
      }
    }

    private void touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
      mPrivateFlags |= PFLAG_TOUCH_EXPANSION_IS_SET;
      if (mTouchExpansions == null) {
        mTouchExpansions = new Edges();
      }
      mTouchExpansions.set(edge, touchExpansion);
    }

    private void foreground(@Nullable Drawable foreground) {
      mPrivateFlags |= PFLAG_FOREGROUND_IS_SET;
      mForeground = foreground;
    }

    private void visibleHeightRatio(float visibleHeightRatio) {
      mPrivateFlags |= PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET;
      mVisibleHeightRatio = visibleHeightRatio;
    }

    private void visibleWidthRatio(float visibleWidthRatio) {
      mPrivateFlags |= PFLAG_VISIBLE_WIDTH_RATIO_IS_SET;
      mVisibleWidthRatio = visibleWidthRatio;
    }

    private void visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
      mPrivateFlags |= PFLAG_VISIBLE_HANDLER_IS_SET;
      mVisibleHandler = visibleHandler;
    }

    private void focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
      mPrivateFlags |= PFLAG_FOCUSED_HANDLER_IS_SET;
      mFocusedHandler = focusedHandler;
    }

    private void unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
      mPrivateFlags |= PFLAG_UNFOCUSED_HANDLER_IS_SET;
      mUnfocusedHandler = unfocusedHandler;
    }

    private void fullImpressionHandler(
        @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
      mPrivateFlags |= PFLAG_FULL_IMPRESSION_HANDLER_IS_SET;
      mFullImpressionHandler = fullImpressionHandler;
    }

    private void invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
      mPrivateFlags |= PFLAG_INVISIBLE_HANDLER_IS_SET;
      mInvisibleHandler = invisibleHandler;
    }

    private void visibilityChangedHandler(
        @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
      mPrivateFlags |= PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET;
      mVisibilityChangedHandler = visibilityChangedHandler;
    }

    private void transitionKey(@Nullable String key, @Nullable String ownerKey) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
      mTransitionKey = key;
      mTransitionOwnerKey = ownerKey;
    }

    private void transitionKeyType(@Nullable Transition.TransitionKeyType type) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_TYPE_IS_SET;
      mTransitionKeyType = type;
    }

    private void stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
      mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_IS_SET;
      mStateListAnimator = stateListAnimator;
    }

    private void stateListAnimatorRes(@DrawableRes int resId) {
      mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET;
      mStateListAnimatorRes = resId;
    }

    void layerType(@LayerType int type, @Nullable Paint paint) {
      mLayerType = type;
      mLayerPaint = paint;
    }

    void copyInto(InternalNode node) {
      if ((mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) != 0L) {
        node.importantForAccessibility(mImportantForAccessibility);
      }
      if ((mPrivateFlags & PFLAG_DUPLICATE_PARENT_STATE_IS_SET) != 0L) {
        node.duplicateParentState(mDuplicateParentState);
      }
      if ((mPrivateFlags & PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET) != 0L) {
        node.duplicateChildrenStates(mDuplicateChildrenStates);
      }
      if ((mPrivateFlags & PFLAG_FOREGROUND_IS_SET) != 0L) {
        node.foreground(mForeground);
      }
      if ((mPrivateFlags & PFLAG_WRAP_IN_VIEW_IS_SET) != 0L) {
        node.wrapInView();
      }
      if ((mPrivateFlags & PFLAG_VISIBLE_HANDLER_IS_SET) != 0L) {
        node.visibleHandler(mVisibleHandler);
      }
      if ((mPrivateFlags & PFLAG_FOCUSED_HANDLER_IS_SET) != 0L) {
        node.focusedHandler(mFocusedHandler);
      }
      if ((mPrivateFlags & PFLAG_FULL_IMPRESSION_HANDLER_IS_SET) != 0L) {
        node.fullImpressionHandler(mFullImpressionHandler);
      }
      if ((mPrivateFlags & PFLAG_INVISIBLE_HANDLER_IS_SET) != 0L) {
        node.invisibleHandler(mInvisibleHandler);
      }
      if ((mPrivateFlags & PFLAG_UNFOCUSED_HANDLER_IS_SET) != 0L) {
        node.unfocusedHandler(mUnfocusedHandler);
      }
      if ((mPrivateFlags & PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET) != 0) {
        node.visibilityChangedHandler(mVisibilityChangedHandler);
      }
      if ((mPrivateFlags & PFLAG_TRANSITION_KEY_IS_SET) != 0L) {
        node.transitionKey(mTransitionKey, mTransitionOwnerKey);
      }
      if ((mPrivateFlags & PFLAG_TRANSITION_KEY_TYPE_IS_SET) != 0L) {
        node.transitionKeyType(mTransitionKeyType);
      }
      if ((mPrivateFlags & PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET) != 0L) {
        node.visibleHeightRatio(mVisibleHeightRatio);
      }
      if ((mPrivateFlags & PFLAG_VISIBLE_WIDTH_RATIO_IS_SET) != 0L) {
        node.visibleWidthRatio(mVisibleWidthRatio);
      }
      if ((mPrivateFlags & PFLAG_TOUCH_EXPANSION_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mTouchExpansions.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            node.touchExpansionPx(YogaEdge.fromInt(i), (int) value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_BORDER_IS_SET) != 0L) {
        node.border(mBorder);
      }
      if ((mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_IS_SET) != 0L) {
        node.stateListAnimator(mStateListAnimator);
      }
      if ((mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET) != 0L) {
        node.stateListAnimatorRes(mStateListAnimatorRes);
      }
      node.layerType(mLayerType, mLayerPaint);
    }

    @Override
    public boolean isEquivalentTo(@Nullable OtherProps other) {
      if (this == other) {
        return true;
      }

      if (other == null) {
        return false;
      }

      return mPrivateFlags == other.mPrivateFlags
          && mImportantForAccessibility == other.mImportantForAccessibility
          && mDuplicateParentState == other.mDuplicateParentState
          && mDuplicateChildrenStates == other.mDuplicateChildrenStates
          && mStateListAnimatorRes == other.mStateListAnimatorRes
          && mLayerType == other.mLayerType
          && Float.compare(other.mVisibleHeightRatio, mVisibleHeightRatio) == 0
          && Float.compare(other.mVisibleWidthRatio, mVisibleWidthRatio) == 0
          && CommonUtils.equals(mTransitionKeyType, other.mTransitionKeyType)
          && CommonUtils.equals(mStateListAnimator, other.mStateListAnimator)
          && CommonUtils.equals(mLayerPaint, other.mLayerPaint)
          && CommonUtils.isEquivalentTo(mVisibleHandler, other.mVisibleHandler)
          && CommonUtils.isEquivalentTo(mFocusedHandler, other.mFocusedHandler)
          && CommonUtils.isEquivalentTo(mUnfocusedHandler, other.mUnfocusedHandler)
          && CommonUtils.isEquivalentTo(mFullImpressionHandler, other.mFullImpressionHandler)
          && CommonUtils.isEquivalentTo(mInvisibleHandler, other.mInvisibleHandler)
          && CommonUtils.isEquivalentTo(mVisibilityChangedHandler, other.mVisibilityChangedHandler)
          && CommonUtils.isEquivalentTo(mTouchExpansions, other.mTouchExpansions)
          && CommonUtils.isEquivalentTo(mBorder, other.mBorder)
          && CommonUtils.equals(mTransitionOwnerKey, other.mTransitionOwnerKey)
          && CommonUtils.equals(mTransitionKey, other.mTransitionKey)
          && DrawableUtils.isEquivalentTo(mForeground, other.mForeground);
    }
  }

  public static class DefaultLayoutProps implements CopyableLayoutProps {
    private static final int PFLAG_WIDTH_IS_SET = 1 << 0;
    private static final int PFLAG_WIDTH_PERCENT_IS_SET = 1 << 1;
    private static final int PFLAG_MIN_WIDTH_IS_SET = 1 << 2;
    private static final int PFLAG_MIN_WIDTH_PERCENT_IS_SET = 1 << 3;
    private static final int PFLAG_MAX_WIDTH_IS_SET = 1 << 4;
    private static final int PFLAG_MAX_WIDTH_PERCENT_IS_SET = 1 << 5;
    private static final int PFLAG_HEIGHT_IS_SET = 1 << 6;
    private static final int PFLAG_HEIGHT_PERCENT_IS_SET = 1 << 7;
    private static final int PFLAG_MIN_HEIGHT_IS_SET = 1 << 8;
    private static final int PFLAG_MIN_HEIGHT_PERCENT_IS_SET = 1 << 9;
    private static final int PFLAG_MAX_HEIGHT_IS_SET = 1 << 10;
    private static final int PFLAG_MAX_HEIGHT_PERCENT_IS_SET = 1 << 11;
    private static final int PFLAG_LAYOUT_DIRECTION_IS_SET = 1 << 12;
    private static final int PFLAG_ALIGN_SELF_IS_SET = 1 << 13;
    private static final int PFLAG_FLEX_IS_SET = 1 << 14;
    private static final int PFLAG_FLEX_GROW_IS_SET = 1 << 15;
    private static final int PFLAG_FLEX_SHRINK_IS_SET = 1 << 16;
    private static final int PFLAG_FLEX_BASIS_IS_SET = 1 << 17;
    private static final int PFLAG_FLEX_BASIS_PERCENT_IS_SET = 1 << 18;
    private static final int PFLAG_ASPECT_RATIO_IS_SET = 1 << 19;
    private static final int PFLAG_POSITION_TYPE_IS_SET = 1 << 20;
    private static final int PFLAG_POSITION_IS_SET = 1 << 21;
    private static final int PFLAG_POSITION_PERCENT_IS_SET = 1 << 22;
    private static final int PFLAG_PADDING_IS_SET = 1 << 23;
    private static final int PFLAG_PADDING_PERCENT_IS_SET = 1 << 24;
    private static final int PFLAG_MARGIN_IS_SET = 1 << 25;
    private static final int PFLAG_MARGIN_PERCENT_IS_SET = 1 << 26;
    private static final int PFLAG_MARGIN_AUTO_IS_SET = 1 << 27;
    private static final int PFLAG_IS_REFERENCE_BASELINE_IS_SET = 1 << 28;

    private int mPrivateFlags;

    @Px private int mWidthPx;
    private float mWidthPercent;
    @Px private int mMinWidthPx;
    private float mMinWidthPercent;
    @Px private int mMaxWidthPx;
    private float mMaxWidthPercent;
    @Px private int mHeightPx;
    private float mHeightPercent;
    @Px private int mMinHeightPx;
    private float mMinHeightPercent;
    @Px private int mMaxHeightPx;
    private float mMaxHeightPercent;
    private float mFlex;
    private float mFlexGrow;
    private float mFlexShrink;
    @Px private int mFlexBasisPx;
    private float mFlexBasisPercent;
    private float mAspectRatio;
    @Nullable private YogaDirection mLayoutDirection;
    @Nullable private YogaAlign mAlignSelf;
    @Nullable private YogaPositionType mPositionType;
    @Nullable private Edges mPositions;
    @Nullable private Edges mMargins;
    @Nullable private Edges mMarginPercents;
    @Nullable private List<YogaEdge> mMarginAutos;
    @Nullable private Edges mPaddings;
    @Nullable private Edges mPaddingPercents;
    @Nullable private Edges mPositionPercents;
    private boolean mIsReferenceBaseline;
    private boolean mUseHeightAsBaseline;
    private boolean mHeightAuto;
    private boolean mWidthAuto;
    private boolean mFlexBasisAuto;

    /** Used by {@link DebugLayoutNodeEditor} */
    private @Nullable Edges mBorderEdges;

    @Override
    public void widthPx(@Px int width) {
      mPrivateFlags |= PFLAG_WIDTH_IS_SET;
      mWidthPx = width;
    }

    @Override
    public void widthPercent(float percent) {
      mPrivateFlags |= PFLAG_WIDTH_PERCENT_IS_SET;
      mWidthPercent = percent;
    }

    @Override
    public void minWidthPx(@Px int minWidth) {
      mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
      mMinWidthPx = minWidth;
    }

    @Override
    public void maxWidthPx(@Px int maxWidth) {
      mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
      mMaxWidthPx = maxWidth;
    }

    @Override
    public void minWidthPercent(float percent) {
      mPrivateFlags |= PFLAG_MIN_WIDTH_PERCENT_IS_SET;
      mMinWidthPercent = percent;
    }

    @Override
    public void maxWidthPercent(float percent) {
      mPrivateFlags |= PFLAG_MAX_WIDTH_PERCENT_IS_SET;
      mMaxWidthPercent = percent;
    }

    @Override
    public void heightPx(@Px int height) {
      mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
      mHeightPx = height;
    }

    @Override
    public void heightPercent(float percent) {
      mPrivateFlags |= PFLAG_HEIGHT_PERCENT_IS_SET;
      mHeightPercent = percent;
    }

    @Override
    public void minHeightPx(@Px int minHeight) {
      mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
      mMinHeightPx = minHeight;
    }

    @Override
    public void maxHeightPx(@Px int maxHeight) {
      mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
      mMaxHeightPx = maxHeight;
    }

    @Override
    public void minHeightPercent(float percent) {
      mPrivateFlags |= PFLAG_MIN_HEIGHT_PERCENT_IS_SET;
      mMinHeightPercent = percent;
    }

    @Override
    public void maxHeightPercent(float percent) {
      mPrivateFlags |= PFLAG_MAX_HEIGHT_PERCENT_IS_SET;
      mMaxHeightPercent = percent;
    }

    @Override
    public void layoutDirection(YogaDirection direction) {
      mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
      mLayoutDirection = direction;
    }

    @Override
    public void alignSelf(YogaAlign alignSelf) {
      mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
      mAlignSelf = alignSelf;
    }

    @Override
    public void flex(float flex) {
      mPrivateFlags |= PFLAG_FLEX_IS_SET;
      mFlex = flex;
    }

    @Override
    public void flexGrow(float flexGrow) {
      mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
      mFlexGrow = flexGrow;
    }

    @Override
    public void flexShrink(float flexShrink) {
      mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
      mFlexShrink = flexShrink;
    }

    @Override
    public void flexBasisPx(@Px int flexBasis) {
      mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
      mFlexBasisPx = flexBasis;
    }

    @Override
    public void flexBasisPercent(float percent) {
      mPrivateFlags |= PFLAG_FLEX_BASIS_PERCENT_IS_SET;
      mFlexBasisPercent = percent;
    }

    @Override
    public void aspectRatio(float aspectRatio) {
      mPrivateFlags |= PFLAG_ASPECT_RATIO_IS_SET;
      mAspectRatio = aspectRatio;
    }

    @Override
    public void positionType(@Nullable YogaPositionType positionType) {
      mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
      mPositionType = positionType;
    }

    @Override
    public void positionPx(YogaEdge edge, @Px int position) {
      mPrivateFlags |= PFLAG_POSITION_IS_SET;
      if (mPositions == null) {
        mPositions = new Edges();
      }

      mPositions.set(edge, position);
    }

    @Override
    public void positionPercent(YogaEdge edge, float percent) {
      mPrivateFlags |= PFLAG_POSITION_PERCENT_IS_SET;
      if (mPositionPercents == null) {
        mPositionPercents = new Edges();
      }
      mPositionPercents.set(edge, percent);
    }

    @Override
    public void paddingPx(YogaEdge edge, @Px int padding) {
      mPrivateFlags |= PFLAG_PADDING_IS_SET;
      if (mPaddings == null) {
        mPaddings = new Edges();
      }
      mPaddings.set(edge, padding);
    }

    @Override
    public void paddingPercent(YogaEdge edge, float percent) {
      mPrivateFlags |= PFLAG_PADDING_PERCENT_IS_SET;
      if (mPaddingPercents == null) {
        mPaddingPercents = new Edges();
      }
      mPaddingPercents.set(edge, percent);
    }

    @Override
    public void marginPx(YogaEdge edge, @Px int margin) {
      mPrivateFlags |= PFLAG_MARGIN_IS_SET;

      if (mMargins == null) {
        mMargins = new Edges();
      }
      mMargins.set(edge, margin);
    }

    @Override
    public void marginPercent(YogaEdge edge, float percent) {
      mPrivateFlags |= PFLAG_MARGIN_PERCENT_IS_SET;
      if (mMarginPercents == null) {
        mMarginPercents = new Edges();
      }
      mMarginPercents.set(edge, percent);
    }

    @Override
    public void marginAuto(YogaEdge edge) {
      mPrivateFlags |= PFLAG_MARGIN_AUTO_IS_SET;
      if (mMarginAutos == null) {
        mMarginAutos = new ArrayList<>(2);
      }
      mMarginAutos.add(edge);
    }

    @Override
    public void isReferenceBaseline(boolean isReferenceBaseline) {
      mPrivateFlags |= PFLAG_IS_REFERENCE_BASELINE_IS_SET;
      mIsReferenceBaseline = isReferenceBaseline;
    }

    @Override
    public void useHeightAsBaseline(boolean useHeightAsBaseline) {
      mUseHeightAsBaseline = useHeightAsBaseline;
    }

    @Override
    public void heightAuto() {
      mHeightAuto = true;
    }

    @Override
    public void widthAuto() {
      mWidthAuto = true;
    }

    @Override
    public void flexBasisAuto() {
      mFlexBasisAuto = true;
    }

    /** Used by {@link DebugLayoutNodeEditor} */
    @Override
    public void setBorderWidth(YogaEdge edge, float borderWidth) {
      if (mBorderEdges == null) {
        mBorderEdges = new Edges();
      }
      mBorderEdges.set(edge, borderWidth);
    }

    @Override
    public void copyInto(LayoutProps target) {
      if ((mPrivateFlags & PFLAG_WIDTH_IS_SET) != 0L) {
        target.widthPx(mWidthPx);
      }
      if ((mPrivateFlags & PFLAG_WIDTH_PERCENT_IS_SET) != 0L) {
        target.widthPercent(mWidthPercent);
      }
      if ((mPrivateFlags & PFLAG_MIN_WIDTH_IS_SET) != 0L) {
        target.minWidthPx(mMinWidthPx);
      }
      if ((mPrivateFlags & PFLAG_MIN_WIDTH_PERCENT_IS_SET) != 0L) {
        target.minWidthPercent(mMinWidthPercent);
      }
      if ((mPrivateFlags & PFLAG_MAX_WIDTH_IS_SET) != 0L) {
        target.maxWidthPx(mMaxWidthPx);
      }
      if ((mPrivateFlags & PFLAG_MAX_WIDTH_PERCENT_IS_SET) != 0L) {
        target.maxWidthPercent(mMaxWidthPercent);
      }
      if ((mPrivateFlags & PFLAG_HEIGHT_IS_SET) != 0L) {
        target.heightPx(mHeightPx);
      }
      if ((mPrivateFlags & PFLAG_HEIGHT_PERCENT_IS_SET) != 0L) {
        target.heightPercent(mHeightPercent);
      }
      if ((mPrivateFlags & PFLAG_MIN_HEIGHT_IS_SET) != 0L) {
        target.minHeightPx(mMinHeightPx);
      }
      if ((mPrivateFlags & PFLAG_MIN_HEIGHT_PERCENT_IS_SET) != 0L) {
        target.minHeightPercent(mMinHeightPercent);
      }
      if ((mPrivateFlags & PFLAG_MAX_HEIGHT_IS_SET) != 0L) {
        target.maxHeightPx(mMaxHeightPx);
      }
      if ((mPrivateFlags & PFLAG_MAX_HEIGHT_PERCENT_IS_SET) != 0L) {
        target.maxHeightPercent(mMaxHeightPercent);
      }
      if ((mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) != 0L) {
        target.layoutDirection(mLayoutDirection);
      }
      if ((mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
        target.alignSelf(mAlignSelf);
      }
      if ((mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
        target.flex(mFlex);
      }
      if ((mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
        target.flexGrow(mFlexGrow);
      }
      if ((mPrivateFlags & PFLAG_FLEX_SHRINK_IS_SET) != 0L) {
        target.flexShrink(mFlexShrink);
      }
      if ((mPrivateFlags & PFLAG_FLEX_BASIS_IS_SET) != 0L) {
        target.flexBasisPx(mFlexBasisPx);
      }
      if ((mPrivateFlags & PFLAG_FLEX_BASIS_PERCENT_IS_SET) != 0L) {
        target.flexBasisPercent(mFlexBasisPercent);
      }
      if ((mPrivateFlags & PFLAG_ASPECT_RATIO_IS_SET) != 0L) {
        target.aspectRatio(mAspectRatio);
      }
      if ((mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
        target.positionType(mPositionType);
      }
      if ((mPrivateFlags & PFLAG_POSITION_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mPositions.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            target.positionPx(YogaEdge.fromInt(i), (int) value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_POSITION_PERCENT_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mPositionPercents.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            target.positionPercent(YogaEdge.fromInt(i), value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mPaddings.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            target.paddingPx(YogaEdge.fromInt(i), (int) value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_PADDING_PERCENT_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mPaddingPercents.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            target.paddingPercent(YogaEdge.fromInt(i), value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mMargins.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            target.marginPx(YogaEdge.fromInt(i), (int) value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_MARGIN_PERCENT_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mMarginPercents.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            target.marginPercent(YogaEdge.fromInt(i), value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_MARGIN_AUTO_IS_SET) != 0L) {
        for (YogaEdge edge : mMarginAutos) {
          target.marginAuto(edge);
        }
      }
      if ((mPrivateFlags & PFLAG_IS_REFERENCE_BASELINE_IS_SET) != 0L) {
        target.isReferenceBaseline(mIsReferenceBaseline);
      }
      if (mUseHeightAsBaseline) {
        target.useHeightAsBaseline(mUseHeightAsBaseline);
      }
      if (mHeightAuto) {
        target.heightAuto();
      }
      if (mWidthAuto) {
        target.widthAuto();
      }
      if (mFlexBasisAuto) {
        target.flexBasisAuto();
      }
      if (mBorderEdges != null) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mBorderEdges.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            target.setBorderWidth(YogaEdge.fromInt(i), value);
          }
        }
      }
    }

    @Override
    public boolean isEquivalentTo(CopyableLayoutProps o) {
      if (this == o) {
        return true;
      }

      if (o == null) {
        return false;
      }

      DefaultLayoutProps other = (DefaultLayoutProps) o;
      return mPrivateFlags == other.mPrivateFlags
          && mWidthPx == other.mWidthPx
          && Float.compare(other.mWidthPercent, mWidthPercent) == 0
          && mMinWidthPx == other.mMinWidthPx
          && Float.compare(other.mMinWidthPercent, mMinWidthPercent) == 0
          && mMaxWidthPx == other.mMaxWidthPx
          && Float.compare(other.mMaxWidthPercent, mMaxWidthPercent) == 0
          && mHeightPx == other.mHeightPx
          && Float.compare(other.mHeightPercent, mHeightPercent) == 0
          && mMinHeightPx == other.mMinHeightPx
          && Float.compare(other.mMinHeightPercent, mMinHeightPercent) == 0
          && mMaxHeightPx == other.mMaxHeightPx
          && Float.compare(other.mMaxHeightPercent, mMaxHeightPercent) == 0
          && Float.compare(other.mFlex, mFlex) == 0
          && Float.compare(other.mFlexGrow, mFlexGrow) == 0
          && Float.compare(other.mFlexShrink, mFlexShrink) == 0
          && mFlexBasisPx == other.mFlexBasisPx
          && Float.compare(other.mFlexBasisPercent, mFlexBasisPercent) == 0
          && Float.compare(other.mAspectRatio, mAspectRatio) == 0
          && mIsReferenceBaseline == other.mIsReferenceBaseline
          && mUseHeightAsBaseline == other.mUseHeightAsBaseline
          && mLayoutDirection == other.mLayoutDirection
          && mAlignSelf == other.mAlignSelf
          && mPositionType == other.mPositionType
          && CommonUtils.isEquivalentTo(mPositions, other.mPositions)
          && CommonUtils.isEquivalentTo(mMargins, other.mMargins)
          && CommonUtils.isEquivalentTo(mMarginPercents, other.mMarginPercents)
          && CommonUtils.isEquivalentTo(mPaddings, other.mPaddings)
          && CommonUtils.isEquivalentTo(mPaddingPercents, other.mPaddingPercents)
          && CommonUtils.isEquivalentTo(mPositionPercents, other.mPositionPercents)
          && mHeightAuto == other.mHeightAuto
          && mWidthAuto == other.mWidthAuto
          && mFlexBasisAuto == other.mFlexBasisAuto
          && CommonUtils.isEquivalentTo(mBorderEdges, other.mBorderEdges)
          && CommonUtils.equals(mMarginAutos, other.mMarginAutos);
    }
  }
}
