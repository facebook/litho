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

import static com.facebook.litho.ComponentContext.NULL_LAYOUT;

import android.animation.StateListAnimator;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.ViewOutlineProvider;
import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.internal.SparseFloatArray;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import java.util.ArrayList;
import java.util.List;

/** Internal class that holds props that are common to all {@link Component}s. */
@ThreadConfined(ThreadConfined.ANY)
class SparseCommonPropsHolder implements CommonProps {

  // Flags used to indicate that a certain attribute was explicitly set on the node.
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L;
  private static final long PFLAG_ALIGN_SELF_IS_SET = 1L << 1;
  private static final long PFLAG_FLEX_IS_SET = 1L << 2;
  private static final long PFLAG_FLEX_GROW_IS_SET = 1L << 3;
  private static final long PFLAG_FLEX_SHRINK_IS_SET = 1L << 4;
  private static final long PFLAG_FLEX_BASIS_IS_SET = 1L << 5;
  private static final long PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1L << 6;
  private static final long PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1L << 7;
  private static final long PFLAG_MARGIN_IS_SET = 1L << 8;
  private static final long PFLAG_PADDING_IS_SET = 1L << 9;
  private static final long PFLAG_POSITION_PERCENT_IS_SET = 1L << 10;
  private static final long PFLAG_MIN_WIDTH_IS_SET = 1L << 11;
  private static final long PFLAG_MAX_WIDTH_IS_SET = 1L << 12;
  private static final long PFLAG_MIN_HEIGHT_IS_SET = 1L << 13;
  private static final long PFLAG_MAX_HEIGHT_IS_SET = 1L << 14;
  private static final long PFLAG_FOREGROUND_IS_SET = 1L << 15;
  private static final long PFLAG_VISIBLE_HANDLER_IS_SET = 1L << 16;
  private static final long PFLAG_FOCUSED_HANDLER_IS_SET = 1L << 17;
  private static final long PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1L << 18;
  private static final long PFLAG_INVISIBLE_HANDLER_IS_SET = 1L << 19;
  private static final long PFLAG_UNFOCUSED_HANDLER_IS_SET = 1L << 20;
  private static final long PFLAG_TOUCH_EXPANSION_IS_SET = 1L << 21;
  private static final long PFLAG_ASPECT_RATIO_IS_SET = 1L << 22;
  private static final long PFLAG_TRANSITION_KEY_IS_SET = 1L << 23;
  private static final long PFLAG_WRAP_IN_VIEW_IS_SET = 1L << 24;
  private static final long PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET = 1L << 25;
  private static final long PFLAG_VISIBLE_WIDTH_RATIO_IS_SET = 1L << 26;
  private static final long PFLAG_FLEX_BASIS_PERCENT_IS_SET = 1L << 27;
  private static final long PFLAG_MARGIN_PERCENT_IS_SET = 1L << 28;
  private static final long PFLAG_MARGIN_AUTO_IS_SET = 1L << 29;
  private static final long PFLAG_PADDING_PERCENT_IS_SET = 1L << 30;
  private static final long PFLAG_WIDTH_PERCENT_IS_SET = 1L << 31;
  private static final long PFLAG_MIN_WIDTH_PERCENT_IS_SET = 1L << 32;
  private static final long PFLAG_MAX_WIDTH_PERCENT_IS_SET = 1L << 33;
  private static final long PFLAG_HEIGHT_PERCENT_IS_SET = 1L << 34;
  private static final long PFLAG_MIN_HEIGHT_PERCENT_IS_SET = 1L << 35;
  private static final long PFLAG_MAX_HEIGHT_PERCENT_IS_SET = 1L << 36;
  private static final long PFLAG_BORDER_IS_SET = 1L << 37;
  private static final long PFLAG_STATE_LIST_ANIMATOR_IS_SET = 1L << 38;
  private static final long PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET = 1L << 39;
  private static final long PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET = 1L << 40;
  private static final long PFLAG_TRANSITION_KEY_TYPE_IS_SET = 1L << 41;
  private static final long PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET = 1L << 42;
  private static final long PFLAG_IS_REFERENCE_BASELINE_IS_SET = 1L << 43;
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 44;
  private static final long PFLAG_POSITION_IS_SET = 1L << 45;
  private static final long PFLAG_WIDTH_IS_SET = 1L << 46;
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 47;
  private static final long PFLAG_BACKGROUND_IS_SET = 1L << 48;
  private static final long PFLAG_TEST_KEY_IS_SET = 1L << 49;

  // Indexes of Object type properties
  private static final int INDEX_VisibleHandler = 0;
  private static final int INDEX_FocusedHandler = 1;
  private static final int INDEX_UnfocusedHandler = 2;
  private static final int INDEX_FullImpressionHandler = 3;
  private static final int INDEX_InvisibleHandler = 4;
  private static final int INDEX_VisibilityChangedHandler = 5;
  private static final int INDEX_LayoutDirection = 6;
  private static final int INDEX_AlignSelf = 7;
  private static final int INDEX_Margins = 8;
  private static final int INDEX_MarginPercents = 9;
  private static final int INDEX_MarginAutos = 10;
  private static final int INDEX_Paddings = 11;
  private static final int INDEX_PaddingPercents = 12;
  private static final int INDEX_PositionPercents = 13;
  private static final int INDEX_TouchExpansions = 14;
  private static final int INDEX_Foreground = 15;
  private static final int INDEX_TransitionKey = 16;
  private static final int INDEX_TransitionKeyType = 17;
  private static final int INDEX_Border = 18;
  private static final int INDEX_StateListAnimator = 19;
  private static final int INDEX_PositionType = 20;
  private static final int INDEX_Positions = 21;
  private static final int INDEX_Background = 22;
  private static final int INDEX_TestKey = 23;

  // Indexes of float type properties
  private static final int INDEX_WidthPx = 0;
  private static final int INDEX_HeightPx = 1;
  private static final int INDEX_ImportantForAccessibility = 2;
  private static final int INDEX_FlexBasisPx = 3;
  private static final int INDEX_MinWidthPx = 4;
  private static final int INDEX_MaxWidthPx = 5;
  private static final int INDEX_MinHeightPx = 6;
  private static final int INDEX_MaxHeightPx = 7;
  private static final int INDEX_DefStyleAttr = 8;
  private static final int INDEX_DefStyleRes = 9;
  private static final int INDEX_StateListAnimatorRes = 10;

  // Indexes of int type properties
  private static final int INDEX_VisibleHeightRatio = 0;
  private static final int INDEX_VisibleWidthRatio = 1;
  private static final int INDEX_Flex = 2;
  private static final int INDEX_FlexGrow = 3;
  private static final int INDEX_FlexShrink = 4;
  private static final int INDEX_FlexBasisPercent = 5;
  private static final int INDEX_WidthPercent = 6;
  private static final int INDEX_MinWidthPercent = 7;
  private static final int INDEX_MaxWidthPercent = 8;
  private static final int INDEX_HeightPercentage = 9;
  private static final int INDEX_MinHeightPercent = 10;
  private static final int INDEX_MaxHeightPercent = 11;
  private static final int INDEX_AspectRatio = 12;

  private long mPrivateFlags;

  @Nullable private NodeInfo mNodeInfo;
  @Nullable SparseArray<Object> mObjectProps;
  @Nullable SparseIntArray mIntProps;
  @Nullable SparseFloatArray mFloatProps;

  private boolean mDuplicateParentState;
  private boolean mIsReferenceBaseline;
  private boolean mUseHeightAsBaseline;

  @Override
  public void layoutDirection(@Nullable YogaDirection direction) {
    mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
    getOrCreateObjectProps().append(INDEX_LayoutDirection, direction);
  }

  @Override
  public void alignSelf(@Nullable YogaAlign alignSelf) {
    mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
    getOrCreateObjectProps().append(INDEX_AlignSelf, alignSelf);
  }

  @Override
  public void flex(float flex) {
    mPrivateFlags |= PFLAG_FLEX_IS_SET;
    getOrCreateFloatProps().append(INDEX_Flex, flex);
  }

  @Override
  public void flexGrow(float flexGrow) {
    mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
    getOrCreateFloatProps().append(INDEX_FlexGrow, flexGrow);
  }

  @Override
  public void flexShrink(float flexShrink) {
    mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
    getOrCreateFloatProps().append(INDEX_FlexShrink, flexShrink);
  }

  @Override
  public void flexBasisPx(@Px int flexBasis) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    getOrCreateIntProps().append(INDEX_FlexBasisPx, flexBasis);
  }

  @Override
  public void flexBasisPercent(float percent) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_PERCENT_IS_SET;
    getOrCreateFloatProps().append(INDEX_FlexBasisPercent, percent);
  }

  @Override
  public void importantForAccessibility(int importantForAccessibility) {
    mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
    getOrCreateIntProps().append(INDEX_ImportantForAccessibility, importantForAccessibility);
  }

  @Override
  public void duplicateParentState(boolean duplicateParentState) {
    mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
    mDuplicateParentState = duplicateParentState;
  }

  @Override
  public void marginPx(YogaEdge edge, @Px int margin) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    Edges mMargins = (Edges) getOrCreateObjectProps().get(INDEX_Margins);
    if (mMargins == null) {
      mMargins = new Edges();
      getOrCreateObjectProps().append(INDEX_Margins, mMargins);
    }
    mMargins.set(edge, margin);
  }

  @Override
  public void marginPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_MARGIN_PERCENT_IS_SET;
    Edges mMarginPercents = (Edges) getOrCreateObjectProps().get(INDEX_MarginPercents);
    if (mMarginPercents == null) {
      mMarginPercents = new Edges();
      getOrCreateObjectProps().append(INDEX_MarginPercents, mMarginPercents);
    }
    mMarginPercents.set(edge, percent);
  }

  @Override
  public void marginAuto(YogaEdge edge) {
    mPrivateFlags |= PFLAG_MARGIN_AUTO_IS_SET;
    ArrayList<YogaEdge> mMarginAutos =
        (ArrayList<YogaEdge>) getOrCreateObjectProps().get(INDEX_MarginAutos);
    if (mMarginAutos == null) {
      mMarginAutos = new ArrayList<>(2);
      getOrCreateObjectProps().append(INDEX_MarginAutos, mMarginAutos);
    }
    mMarginAutos.add(edge);
  }

  @Override
  public void paddingPx(YogaEdge edge, @Px int padding) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;
    Edges mPaddings = (Edges) getOrCreateObjectProps().get(INDEX_Paddings);
    if (mPaddings == null) {
      mPaddings = new Edges();
      getOrCreateObjectProps().append(INDEX_Paddings, mPaddings);
    }
    mPaddings.set(edge, padding);
  }

  @Override
  public void paddingPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_PADDING_PERCENT_IS_SET;
    Edges mPaddingPercents = (Edges) getOrCreateObjectProps().get(INDEX_PaddingPercents);
    if (mPaddingPercents == null) {
      mPaddingPercents = new Edges();
      getOrCreateObjectProps().append(INDEX_PaddingPercents, mPaddingPercents);
    }
    mPaddingPercents.set(edge, percent);
  }

  @Override
  public void border(@Nullable Border border) {
    if (border != null) {
      mPrivateFlags |= PFLAG_BORDER_IS_SET;
      getOrCreateObjectProps().append(INDEX_Border, border);
    }
  }

  @Override
  public void positionPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_POSITION_PERCENT_IS_SET;
    Edges mPositionPercents = (Edges) getOrCreateObjectProps().get(INDEX_PositionPercents);
    if (mPositionPercents == null) {
      mPositionPercents = new Edges();
      getOrCreateObjectProps().append(INDEX_PositionPercents, mPositionPercents);
    }
    mPositionPercents.set(edge, percent);
  }

  @Override
  public void widthPercent(float percent) {
    mPrivateFlags |= PFLAG_WIDTH_PERCENT_IS_SET;
    getOrCreateFloatProps().append(INDEX_WidthPercent, percent);
  }

  @Override
  public void minWidthPx(@Px int minWidth) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    getOrCreateIntProps().append(INDEX_MinWidthPx, minWidth);
  }

  @Override
  public void minWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_PERCENT_IS_SET;
    getOrCreateFloatProps().append(INDEX_MinWidthPercent, percent);
  }

  @Override
  public void maxWidthPx(@Px int maxWidth) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    getOrCreateIntProps().append(INDEX_MaxWidthPx, maxWidth);
  }

  @Override
  public void maxWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_PERCENT_IS_SET;
    getOrCreateFloatProps().append(INDEX_MaxWidthPercent, percent);
  }

  @Override
  public void heightPercent(float percent) {
    mPrivateFlags |= PFLAG_HEIGHT_PERCENT_IS_SET;
    getOrCreateFloatProps().append(INDEX_HeightPercentage, percent);
  }

  @Override
  public void minHeightPx(@Px int minHeight) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    getOrCreateIntProps().append(INDEX_MinHeightPx, minHeight);
  }

  @Override
  public void minHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_PERCENT_IS_SET;
    getOrCreateFloatProps().append(INDEX_MinHeightPercent, percent);
  }

  @Override
  public void maxHeightPx(@Px int maxHeight) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    getOrCreateIntProps().append(INDEX_MaxHeightPx, maxHeight);
  }

  @Override
  public void maxHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_PERCENT_IS_SET;
    getOrCreateFloatProps().append(INDEX_MaxHeightPercent, percent);
  }

  @Override
  public void aspectRatio(float aspectRatio) {
    mPrivateFlags |= PFLAG_ASPECT_RATIO_IS_SET;
    getOrCreateFloatProps().append(INDEX_AspectRatio, aspectRatio);
  }

  @Override
  public void isReferenceBaseline(boolean isReferenceBaseline) {
    mPrivateFlags |= PFLAG_IS_REFERENCE_BASELINE_IS_SET;
    mIsReferenceBaseline = isReferenceBaseline;
  }

  @Override
  public void touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    mPrivateFlags |= PFLAG_TOUCH_EXPANSION_IS_SET;
    Edges mTouchExpansions = (Edges) getOrCreateObjectProps().get(INDEX_TouchExpansions);
    if (mTouchExpansions == null) {
      mTouchExpansions = new Edges();
      getOrCreateObjectProps().append(INDEX_TouchExpansions, mTouchExpansions);
    }
    mTouchExpansions.set(edge, touchExpansion);
  }

  @Override
  public void foreground(@Nullable ComparableDrawable foreground) {
    mPrivateFlags |= PFLAG_FOREGROUND_IS_SET;
    getOrCreateObjectProps().append(INDEX_Foreground, foreground);
  }

  @Override
  public void visibleHeightRatio(float visibleHeightRatio) {
    mPrivateFlags |= PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET;
    getOrCreateFloatProps().append(INDEX_VisibleHeightRatio, visibleHeightRatio);
  }

  @Override
  public void visibleWidthRatio(float visibleWidthRatio) {
    mPrivateFlags |= PFLAG_VISIBLE_WIDTH_RATIO_IS_SET;
    getOrCreateFloatProps().append(INDEX_VisibleWidthRatio, visibleWidthRatio);
  }

  @Override
  public void visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_HANDLER_IS_SET;
    getOrCreateObjectProps().append(INDEX_VisibleHandler, visibleHandler);
  }

  @Override
  public void focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    mPrivateFlags |= PFLAG_FOCUSED_HANDLER_IS_SET;
    getOrCreateObjectProps().append(INDEX_FocusedHandler, focusedHandler);
  }

  @Override
  public void unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    mPrivateFlags |= PFLAG_UNFOCUSED_HANDLER_IS_SET;
    getOrCreateObjectProps().append(INDEX_UnfocusedHandler, unfocusedHandler);
  }

  @Override
  public void fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    mPrivateFlags |= PFLAG_FULL_IMPRESSION_HANDLER_IS_SET;
    getOrCreateObjectProps().append(INDEX_FullImpressionHandler, fullImpressionHandler);
  }

  @Override
  public void invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    mPrivateFlags |= PFLAG_INVISIBLE_HANDLER_IS_SET;
    getOrCreateObjectProps().append(INDEX_InvisibleHandler, invisibleHandler);
  }

  @Override
  public void visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    mPrivateFlags |= PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET;
    getOrCreateObjectProps().append(INDEX_VisibilityChangedHandler, visibilityChangedHandler);
  }

  @Override
  public void transitionKey(@Nullable String key) {
    mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
    getOrCreateObjectProps().append(INDEX_TransitionKey, key);
  }

  @Override
  public void transitionKeyType(@Nullable Transition.TransitionKeyType type) {
    mPrivateFlags |= PFLAG_TRANSITION_KEY_TYPE_IS_SET;
    getOrCreateObjectProps().append(INDEX_TransitionKeyType, type);
  }

  @Nullable
  @Override
  public Transition.TransitionKeyType getTransitionKeyType() {
    return mObjectProps != null
        ? (Transition.TransitionKeyType) mObjectProps.get(INDEX_TransitionKeyType)
        : null;
  }

  @Override
  public void stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_IS_SET;
    getOrCreateObjectProps().append(INDEX_StateListAnimator, stateListAnimator);
  }

  @Override
  public void stateListAnimatorRes(@DrawableRes int resId) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET;
    getOrCreateIntProps().append(INDEX_StateListAnimatorRes, resId);
  }

  @Override
  public void useHeightAsBaseline(boolean useHeightAsBaseline) {
    mPrivateFlags |= PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET;
    mUseHeightAsBaseline = useHeightAsBaseline;
  }

  @Override
  public void setStyle(@AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    getOrCreateIntProps().append(INDEX_DefStyleAttr, defStyleAttr);
    getOrCreateIntProps().append(INDEX_DefStyleRes, defStyleRes);
  }

  @Override
  public void positionType(@Nullable YogaPositionType positionType) {
    mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
    getOrCreateObjectProps().append(INDEX_PositionType, positionType);
  }

  @Override
  public void positionPx(YogaEdge edge, @Px int position) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    Edges mPositions = (Edges) getOrCreateObjectProps().get(INDEX_Positions);
    if (mPositions == null) {
      mPositions = new Edges();
      getOrCreateObjectProps().append(INDEX_Positions, mPositions);
    }

    mPositions.set(edge, position);
  }

  @Override
  public void widthPx(@Px int width) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    getOrCreateIntProps().append(INDEX_WidthPx, width);
  }

  @Override
  public void heightPx(@Px int height) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    getOrCreateIntProps().append(INDEX_HeightPx, height);
  }

  @Override
  public void background(ComparableDrawable background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    getOrCreateObjectProps().append(INDEX_Background, background);
  }

  @Override
  public void testKey(String testKey) {
    mPrivateFlags |= PFLAG_TEST_KEY_IS_SET;
    getOrCreateObjectProps().append(INDEX_TestKey, testKey);
  }

  @Override
  public void wrapInView() {
    mPrivateFlags |= PFLAG_WRAP_IN_VIEW_IS_SET;
  }

  @Nullable
  @Override
  public ComparableDrawable getBackground() {
    return (ComparableDrawable) getOrCreateObjectProps().get(INDEX_Background);
  }

  @Override
  public void clickHandler(EventHandler<ClickEvent> clickHandler) {
    getOrCreateNodeInfo().setClickHandler(clickHandler);
  }

  @Override
  @Nullable
  public EventHandler<ClickEvent> getClickHandler() {
    return getOrCreateNodeInfo().getClickHandler();
  }

  @Override
  public void longClickHandler(EventHandler<LongClickEvent> longClickHandler) {
    getOrCreateNodeInfo().setLongClickHandler(longClickHandler);
  }

  @Override
  @Nullable
  public EventHandler<LongClickEvent> getLongClickHandler() {
    return getOrCreateNodeInfo().getLongClickHandler();
  }

  @Override
  public void focusChangeHandler(EventHandler<FocusChangedEvent> focusChangeHandler) {
    getOrCreateNodeInfo().setFocusChangeHandler(focusChangeHandler);
  }

  @Override
  @Nullable
  public EventHandler<FocusChangedEvent> getFocusChangeHandler() {
    return getOrCreateNodeInfo().getFocusChangeHandler();
  }

  @Override
  public void touchHandler(EventHandler<TouchEvent> touchHandler) {
    getOrCreateNodeInfo().setTouchHandler(touchHandler);
  }

  @Override
  @Nullable
  public EventHandler<TouchEvent> getTouchHandler() {
    return getOrCreateNodeInfo().getTouchHandler();
  }

  @Override
  public void interceptTouchHandler(EventHandler<InterceptTouchEvent> interceptTouchHandler) {
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

  @Nullable
  @Override
  public String getTransitionKey() {
    return mObjectProps != null ? (String) mObjectProps.get(INDEX_TransitionKey) : null;
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
    wrapInView();
  }

  @Override
  public void alpha(float alpha) {
    getOrCreateNodeInfo().setAlpha(alpha);
    wrapInView();
  }

  @Override
  public void rotation(float rotation) {
    getOrCreateNodeInfo().setRotation(rotation);
    wrapInView();
  }

  @Override
  public void rotationX(float rotationX) {
    getOrCreateNodeInfo().setRotationX(rotationX);
    wrapInView();
  }

  @Override
  public void rotationY(float rotationY) {
    getOrCreateNodeInfo().setRotationY(rotationY);
    wrapInView();
  }

  @Nullable
  @Override
  public NodeInfo getNullableNodeInfo() {
    return mNodeInfo;
  }

  @Override
  public NodeInfo getOrCreateNodeInfo() {
    if (mNodeInfo == null) {
      if (ComponentsConfiguration.isSparseNodeInfoIsEnabled) {
        mNodeInfo = new SparseNodeInfo();
      } else {
        mNodeInfo = new DefaultNodeInfo();
      }
    }

    return mNodeInfo;
  }

  @Override
  public void copyInto(ComponentContext c, InternalNode node) {
    if (node == NULL_LAYOUT) {
      return;
    }

    if (mIntProps != null) {
      c.applyStyle(node, mIntProps.get(INDEX_DefStyleAttr), mIntProps.get(INDEX_DefStyleRes));
    }

    if (mNodeInfo != null) {
      mNodeInfo.copyInto(node.getOrCreateNodeInfo());
    }

    if ((mPrivateFlags & PFLAG_BACKGROUND_IS_SET) != 0L) {
      node.background((ComparableDrawable) mObjectProps.get(INDEX_Background));
    }
    if ((mPrivateFlags & PFLAG_TEST_KEY_IS_SET) != 0L) {
      node.testKey((String) mObjectProps.get(INDEX_TestKey));
    }
    if ((mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
      node.positionType((YogaPositionType) mObjectProps.get(INDEX_PositionType));
    }
    if ((mPrivateFlags & PFLAG_POSITION_IS_SET) != 0L) {
      Edges mPositions = (Edges) mObjectProps.get(INDEX_Positions);
      for (int i = 0, length = YogaEdge.values().length; i < length; i++) {
        final YogaEdge edge = YogaEdge.fromInt(i);
        final float value = mPositions.getRaw(edge);
        if (!YogaConstants.isUndefined(value)) {
          node.positionPx(edge, (int) value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_WIDTH_IS_SET) != 0L) {
      node.widthPx(mIntProps.get(INDEX_WidthPx));
    }
    if ((mPrivateFlags & PFLAG_HEIGHT_IS_SET) != 0L) {
      node.heightPx(mIntProps.get(INDEX_HeightPx));
    }
    if ((mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) != 0L) {
      node.layoutDirection((YogaDirection) mObjectProps.get(INDEX_LayoutDirection));
    }
    if ((mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) != 0L) {
      node.importantForAccessibility(mIntProps.get(INDEX_ImportantForAccessibility));
    }
    if ((mPrivateFlags & PFLAG_DUPLICATE_PARENT_STATE_IS_SET) != 0L) {
      node.duplicateParentState(mDuplicateParentState);
    }
    if ((mPrivateFlags & PFLAG_FOREGROUND_IS_SET) != 0L) {
      node.foreground((ComparableDrawable) mObjectProps.get(INDEX_Foreground));
    }
    if ((mPrivateFlags & PFLAG_WRAP_IN_VIEW_IS_SET) != 0L) {
      node.wrapInView();
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_HANDLER_IS_SET) != 0L) {
      node.visibleHandler((EventHandler<VisibleEvent>) mObjectProps.get(INDEX_VisibleHandler));
    }
    if ((mPrivateFlags & PFLAG_FOCUSED_HANDLER_IS_SET) != 0L) {
      node.focusedHandler(
          (EventHandler<FocusedVisibleEvent>) mObjectProps.get(INDEX_FocusedHandler));
    }
    if ((mPrivateFlags & PFLAG_FULL_IMPRESSION_HANDLER_IS_SET) != 0L) {
      node.fullImpressionHandler(
          (EventHandler<FullImpressionVisibleEvent>) mObjectProps.get(INDEX_FullImpressionHandler));
    }
    if ((mPrivateFlags & PFLAG_INVISIBLE_HANDLER_IS_SET) != 0L) {
      node.invisibleHandler(
          (EventHandler<InvisibleEvent>) mObjectProps.get(INDEX_InvisibleHandler));
    }
    if ((mPrivateFlags & PFLAG_UNFOCUSED_HANDLER_IS_SET) != 0L) {
      node.unfocusedHandler(
          (EventHandler<UnfocusedVisibleEvent>) mObjectProps.get(INDEX_UnfocusedHandler));
    }
    if ((mPrivateFlags & PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET) != 0) {
      node.visibilityChangedHandler(
          (EventHandler<VisibilityChangedEvent>) mObjectProps.get(INDEX_VisibilityChangedHandler));
    }
    if ((mPrivateFlags & PFLAG_TRANSITION_KEY_IS_SET) != 0L) {
      node.transitionKey((String) mObjectProps.get(INDEX_TransitionKey));
    }
    if ((mPrivateFlags & PFLAG_TRANSITION_KEY_TYPE_IS_SET) != 0L) {
      node.transitionKeyType(
          (Transition.TransitionKeyType) mObjectProps.get(INDEX_TransitionKeyType));
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET) != 0L) {
      node.visibleHeightRatio(mFloatProps.get(INDEX_VisibleHeightRatio));
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_WIDTH_RATIO_IS_SET) != 0L) {
      node.visibleWidthRatio(mFloatProps.get(INDEX_VisibleWidthRatio));
    }
    if ((mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
      node.alignSelf((YogaAlign) mObjectProps.get(INDEX_AlignSelf));
    }
    if ((mPrivateFlags & PFLAG_POSITION_PERCENT_IS_SET) != 0L) {
      Edges mPositionPercents = (Edges) mObjectProps.get(INDEX_PositionPercents);
      ;
      for (int i = 0, length = YogaEdge.values().length; i < length; i++) {
        final YogaEdge edge = YogaEdge.fromInt(i);
        final float value = mPositionPercents.getRaw(edge);
        if (!YogaConstants.isUndefined(value)) {
          node.positionPercent(edge, value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
      node.flex(mFloatProps.get(INDEX_Flex));
    }
    if ((mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
      node.flexGrow(mFloatProps.get(INDEX_FlexGrow));
    }
    if ((mPrivateFlags & PFLAG_FLEX_SHRINK_IS_SET) != 0L) {
      node.flexShrink(mFloatProps.get(INDEX_FlexShrink));
    }
    if ((mPrivateFlags & PFLAG_FLEX_BASIS_IS_SET) != 0L) {
      node.flexBasisPx(mIntProps.get(INDEX_FlexBasisPx));
    }
    if ((mPrivateFlags & PFLAG_FLEX_BASIS_PERCENT_IS_SET) != 0L) {
      node.flexBasisPercent(mFloatProps.get(INDEX_FlexBasisPercent));
    }
    if ((mPrivateFlags & PFLAG_WIDTH_PERCENT_IS_SET) != 0L) {
      node.widthPercent(mFloatProps.get(INDEX_WidthPercent));
    }
    if ((mPrivateFlags & PFLAG_MIN_WIDTH_IS_SET) != 0L) {
      node.minWidthPx(mIntProps.get(INDEX_MinWidthPx));
    }
    if ((mPrivateFlags & PFLAG_MIN_WIDTH_PERCENT_IS_SET) != 0L) {
      node.minWidthPercent(mFloatProps.get(INDEX_MinWidthPercent));
    }
    if ((mPrivateFlags & PFLAG_MAX_WIDTH_IS_SET) != 0L) {
      node.maxWidthPx(mIntProps.get(INDEX_MaxWidthPx));
    }
    if ((mPrivateFlags & PFLAG_MAX_WIDTH_PERCENT_IS_SET) != 0L) {
      node.maxWidthPercent(mFloatProps.get(INDEX_MaxWidthPercent));
    }
    if ((mPrivateFlags & PFLAG_HEIGHT_PERCENT_IS_SET) != 0L) {
      node.heightPercent(mFloatProps.get(INDEX_HeightPercentage));
    }
    if ((mPrivateFlags & PFLAG_MIN_HEIGHT_IS_SET) != 0L) {
      node.minHeightPx(mIntProps.get(INDEX_MinHeightPx));
    }
    if ((mPrivateFlags & PFLAG_MIN_HEIGHT_PERCENT_IS_SET) != 0L) {
      node.minHeightPercent(mFloatProps.get(INDEX_MinHeightPercent));
    }
    if ((mPrivateFlags & PFLAG_MAX_HEIGHT_IS_SET) != 0L) {
      node.maxHeightPx(mIntProps.get(INDEX_MaxHeightPx));
    }
    if ((mPrivateFlags & PFLAG_MAX_HEIGHT_PERCENT_IS_SET) != 0L) {
      node.maxHeightPercent(mFloatProps.get(INDEX_MaxHeightPercent));
    }
    if ((mPrivateFlags & PFLAG_ASPECT_RATIO_IS_SET) != 0L) {
      node.aspectRatio(mFloatProps.get(INDEX_AspectRatio));
    }
    if ((mPrivateFlags & PFLAG_IS_REFERENCE_BASELINE_IS_SET) != 0L) {
      node.isReferenceBaseline(mIsReferenceBaseline);
    }
    if ((mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
      Edges mMargins = (Edges) mObjectProps.get(INDEX_Margins);
      for (int i = 0, length = YogaEdge.values().length; i < length; i++) {
        final YogaEdge edge = YogaEdge.fromInt(i);
        final float value = mMargins.getRaw(edge);
        if (!YogaConstants.isUndefined(value)) {
          node.marginPx(edge, (int) value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_MARGIN_PERCENT_IS_SET) != 0L) {
      Edges mMarginPercents = (Edges) mObjectProps.get(INDEX_MarginPercents);
      for (int i = 0, length = YogaEdge.values().length; i < length; i++) {
        final YogaEdge edge = YogaEdge.fromInt(i);
        final float value = mMarginPercents.getRaw(edge);
        if (!YogaConstants.isUndefined(value)) {
          node.marginPercent(edge, value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_MARGIN_AUTO_IS_SET) != 0L) {
      List<YogaEdge> mMarginAutos = (List<YogaEdge>) mObjectProps.get(INDEX_MarginAutos);
      for (YogaEdge edge : mMarginAutos) {
        node.marginAuto(edge);
      }
    }
    if ((mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L) {
      Edges mPaddings = (Edges) mObjectProps.get(INDEX_Paddings);
      for (int i = 0, length = YogaEdge.values().length; i < length; i++) {
        final YogaEdge edge = YogaEdge.fromInt(i);
        final float value = mPaddings.getRaw(edge);
        if (!YogaConstants.isUndefined(value)) {
          node.paddingPx(edge, (int) value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_PADDING_PERCENT_IS_SET) != 0L) {
      Edges mPaddingPercents = (Edges) mObjectProps.get(INDEX_PaddingPercents);
      for (int i = 0, length = YogaEdge.values().length; i < length; i++) {
        final YogaEdge edge = YogaEdge.fromInt(i);
        final float value = mPaddingPercents.getRaw(edge);
        if (!YogaConstants.isUndefined(value)) {
          node.paddingPercent(edge, value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_TOUCH_EXPANSION_IS_SET) != 0L) {
      Edges mTouchExpansions = (Edges) mObjectProps.get(INDEX_TouchExpansions);
      for (int i = 0, length = YogaEdge.values().length; i < length; i++) {
        final YogaEdge edge = YogaEdge.fromInt(i);
        final float value = mTouchExpansions.getRaw(edge);
        if (!YogaConstants.isUndefined(value)) {
          node.touchExpansionPx(edge, (int) value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_BORDER_IS_SET) != 0L) {
      node.border((Border) mObjectProps.get(INDEX_Border));
    }
    if ((mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_IS_SET) != 0L) {
      node.stateListAnimator((StateListAnimator) mObjectProps.get(INDEX_StateListAnimator));
    }
    if ((mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET) != 0L) {
      node.stateListAnimatorRes(mIntProps.get(INDEX_StateListAnimatorRes));
    }
    if ((mPrivateFlags & PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET) != 0L) {
      node.useHeightAsBaseline(mUseHeightAsBaseline);
    }
  }

  SparseArray<Object> getOrCreateObjectProps() {
    if (mObjectProps == null) {
      mObjectProps = new SparseArray<>(2);
    }
    return mObjectProps;
  }

  SparseIntArray getOrCreateIntProps() {
    if (mIntProps == null) {
      mIntProps = new SparseIntArray(2);
    }
    return mIntProps;
  }

  SparseFloatArray getOrCreateFloatProps() {
    if (mFloatProps == null) {
      mFloatProps = new SparseFloatArray(2);
    }
    return mFloatProps;
  }
}
