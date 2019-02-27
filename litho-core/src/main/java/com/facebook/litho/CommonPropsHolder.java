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

import android.animation.StateListAnimator;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.StyleRes;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import java.util.ArrayList;
import java.util.List;

/** Internal class that holds props that are common to all {@link Component}s. */
@ThreadConfined(ThreadConfined.ANY)
class CommonPropsHolder implements CommonProps, CommonPropsCopyable {

  // Flags used to indicate that a certain attribute was explicitly set on the node.
  private static final byte PFLAG_BACKGROUND_IS_SET = 1 << 0;
  private static final byte PFLAG_TEST_KEY_IS_SET = 1 << 1;

  private byte mPrivateFlags;
  @Nullable private OtherProps mOtherProps;
  @Nullable private NodeInfo mNodeInfo;
  @Nullable private LayoutProps mLayoutProps;
  @Nullable private Reference<? extends Drawable> mBackground;
  @Nullable private String mTestKey;
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
      mLayoutProps = new LayoutProps();
    }

    return mLayoutProps;
  }

  void setStyle(@AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    mDefStyleAttr = defStyleAttr;
    mDefStyleRes = defStyleRes;
  }

  void positionType(YogaPositionType positionType) {
    getOrCreateLayoutProps().positionType(positionType);
  }

  void positionPx(YogaEdge edge, @Px int position) {
    getOrCreateLayoutProps().positionPx(edge, position);
  }

  void widthPx(@Px int width) {
    getOrCreateLayoutProps().widthPx(width);
  }

  void heightPx(@Px int height) {
    getOrCreateLayoutProps().heightPx(height);
  }

  void background(@Nullable Reference<? extends Drawable> background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
  }

  void testKey(String testKey) {
    mPrivateFlags |= PFLAG_TEST_KEY_IS_SET;
    mTestKey = testKey;
  }

  void wrapInView() {
    mWrapInView = true;
  }

  void layoutDirection(YogaDirection direction) {
    getOrCreateLayoutProps().layoutDirection(direction);
  }

  void alignSelf(YogaAlign alignSelf) {
    getOrCreateLayoutProps().alignSelf(alignSelf);
  }

  void flex(float flex) {
    getOrCreateLayoutProps().flex(flex);
  }

  void flexGrow(float flexGrow) {
    getOrCreateLayoutProps().flexGrow(flexGrow);
  }

  void flexShrink(float flexShrink) {
    getOrCreateLayoutProps().flexShrink(flexShrink);
  }

  void flexBasisPx(@Px int flexBasis) {
    getOrCreateLayoutProps().flexBasisPx(flexBasis);
  }

  void flexBasisPercent(float percent) {
    getOrCreateLayoutProps().flexBasisPercent(percent);
  }

  void importantForAccessibility(int importantForAccessibility) {
    getOrCreateOtherProps().importantForAccessibility(importantForAccessibility);
  }

  void duplicateParentState(boolean duplicateParentState) {
    getOrCreateOtherProps().duplicateParentState(duplicateParentState);
  }

  void marginPx(YogaEdge edge, @Px int margin) {
    getOrCreateLayoutProps().marginPx(edge, margin);
  }

  void marginPercent(YogaEdge edge, float percent) {
    getOrCreateLayoutProps().marginPercent(edge, percent);
  }

  void marginAuto(YogaEdge edge) {
    getOrCreateLayoutProps().marginAuto(edge);
  }

  void paddingPx(YogaEdge edge, @Px int padding) {
    getOrCreateLayoutProps().paddingPx(edge, padding);
  }

  void paddingPercent(YogaEdge edge, float percent) {
    getOrCreateLayoutProps().paddingPercent(edge, percent);
  }

  void border(Border border) {
    getOrCreateOtherProps().border(border);
  }

  void stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    getOrCreateOtherProps().stateListAnimator(stateListAnimator);
  }

  void stateListAnimatorRes(@DrawableRes int resId) {
    getOrCreateOtherProps().stateListAnimatorRes(resId);
  }

  void positionPercent(YogaEdge edge, float percent) {
    getOrCreateLayoutProps().positionPercent(edge, percent);
  }

  void widthPercent(float percent) {
    getOrCreateLayoutProps().widthPercent(percent);
  }

  void minWidthPx(@Px int minWidth) {
    getOrCreateLayoutProps().minWidthPx(minWidth);
  }

  void minWidthPercent(float percent) {
    getOrCreateLayoutProps().minWidthPercent(percent);
  }

  void maxWidthPx(@Px int maxWidth) {
    getOrCreateLayoutProps().maxWidthPx(maxWidth);
  }

  void maxWidthPercent(float percent) {
    getOrCreateLayoutProps().maxWidthPercent(percent);
  }

  void heightPercent(float percent) {
    getOrCreateLayoutProps().heightPercent(percent);
  }

  void minHeightPx(@Px int minHeight) {
    getOrCreateLayoutProps().minHeightPx(minHeight);
  }

  void minHeightPercent(float percent) {
    getOrCreateLayoutProps().minHeightPercent(percent);
  }

  void maxHeightPx(@Px int maxHeight) {
    getOrCreateLayoutProps().maxHeightPx(maxHeight);
  }

  void maxHeightPercent(float percent) {
    getOrCreateLayoutProps().maxHeightPercent(percent);
  }

  void aspectRatio(float aspectRatio) {
    getOrCreateLayoutProps().aspectRatio(aspectRatio);
  }

  void isReferenceBaseline(boolean isReferenceBaseline) {
    getOrCreateLayoutProps().isReferenceBaseline(isReferenceBaseline);
  }

  void useHeightAsBaseline(boolean useHeightAsBaseline) {
    getOrCreateLayoutProps().useHeightAsBaseline(useHeightAsBaseline);
  }

  void touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    getOrCreateOtherProps().touchExpansionPx(edge, touchExpansion);
  }

  void foreground(@Nullable ComparableDrawable foreground) {
    getOrCreateOtherProps().foreground(foreground);
  }

  void clickHandler(EventHandler<ClickEvent> clickHandler) {
    getOrCreateNodeInfo().setClickHandler(clickHandler);
  }

  @Nullable
  public Reference<? extends Drawable> getBackground() {
    return mBackground;
  }

  @Override
  @Nullable
  public EventHandler<ClickEvent> getClickHandler() {
    return getOrCreateNodeInfo().getClickHandler();
  }

  void longClickHandler(EventHandler<LongClickEvent> longClickHandler) {
    getOrCreateNodeInfo().setLongClickHandler(longClickHandler);
  }

  @Override
  @Nullable
  public EventHandler<LongClickEvent> getLongClickHandler() {
    return getOrCreateNodeInfo().getLongClickHandler();
  }

  void focusChangeHandler(EventHandler<FocusChangedEvent> focusChangeHandler) {
    getOrCreateNodeInfo().setFocusChangeHandler(focusChangeHandler);
  }

  @Override
  @Nullable
  public EventHandler<FocusChangedEvent> getFocusChangeHandler() {
    return getOrCreateNodeInfo().getFocusChangeHandler();
  }

  void touchHandler(EventHandler<TouchEvent> touchHandler) {
    getOrCreateNodeInfo().setTouchHandler(touchHandler);
  }

  @Override
  @Nullable
  public EventHandler<TouchEvent> getTouchHandler() {
    return getOrCreateNodeInfo().getTouchHandler();
  }

  void interceptTouchHandler(EventHandler<InterceptTouchEvent> interceptTouchHandler) {
    getOrCreateNodeInfo().setInterceptTouchHandler(interceptTouchHandler);
  }

  @Override
  @Nullable
  public EventHandler<InterceptTouchEvent> getInterceptTouchHandler() {
    return getOrCreateNodeInfo().getInterceptTouchHandler();
  }

  void focusable(boolean isFocusable) {
    getOrCreateNodeInfo().setFocusable(isFocusable);
  }

  @Override
  public boolean getFocusable() {
    return getOrCreateNodeInfo().getFocusState() == NodeInfo.FOCUS_SET_TRUE;
  }

  void enabled(boolean isEnabled) {
    getOrCreateNodeInfo().setEnabled(isEnabled);
  }

  void selected(boolean isSelected) {
    getOrCreateNodeInfo().setSelected(isSelected);
  }

  void visibleHeightRatio(float visibleHeightRatio) {
    getOrCreateOtherProps().visibleHeightRatio(visibleHeightRatio);
  }

  void visibleWidthRatio(float visibleWidthRatio) {
    getOrCreateOtherProps().visibleWidthRatio(visibleWidthRatio);
  }

  void visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
    getOrCreateOtherProps().visibleHandler(visibleHandler);
  }

  void focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    getOrCreateOtherProps().focusedHandler(focusedHandler);
  }

  void unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    getOrCreateOtherProps().unfocusedHandler(unfocusedHandler);
  }

  void fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    getOrCreateOtherProps().fullImpressionHandler(fullImpressionHandler);
  }

  void invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    getOrCreateOtherProps().invisibleHandler(invisibleHandler);
  }

  void visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    getOrCreateOtherProps().visibilityChangedHandler(visibilityChangedHandler);
  }

  void contentDescription(@Nullable CharSequence contentDescription) {
    getOrCreateNodeInfo().setContentDescription(contentDescription);
  }

  void viewTag(@Nullable Object viewTag) {
    getOrCreateNodeInfo().setViewTag(viewTag);
  }

  void viewTags(@Nullable SparseArray<Object> viewTags) {
    getOrCreateNodeInfo().setViewTags(viewTags);
  }

  void shadowElevationPx(float shadowElevation) {
    getOrCreateNodeInfo().setShadowElevation(shadowElevation);
  }

  void outlineProvider(@Nullable ViewOutlineProvider outlineProvider) {
    getOrCreateNodeInfo().setOutlineProvider(outlineProvider);
  }

  void clipToOutline(boolean clipToOutline) {
    getOrCreateNodeInfo().setClipToOutline(clipToOutline);
  }

  void clipChildren(boolean clipChildren) {
    getOrCreateNodeInfo().setClipChildren(clipChildren);
  }

  void accessibilityRole(@Nullable @AccessibilityRole.AccessibilityRoleType String role) {
    getOrCreateNodeInfo().setAccessibilityRole(role);
  }

  void accessibilityRoleDescription(@Nullable CharSequence roleDescription) {
    getOrCreateNodeInfo().setAccessibilityRoleDescription(roleDescription);
  }

  void dispatchPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<DispatchPopulateAccessibilityEventEvent>
              dispatchPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setDispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
  }

  void onInitializeAccessibilityEventHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
  }

  void onInitializeAccessibilityNodeInfoHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityNodeInfoEvent>
              onInitializeAccessibilityNodeInfoHandler) {
    getOrCreateNodeInfo()
        .setOnInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
  }

  void onPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
  }

  void onRequestSendAccessibilityEventHandler(
      @Nullable
          EventHandler<OnRequestSendAccessibilityEventEvent>
              onRequestSendAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
  }

  void performAccessibilityActionHandler(
      @Nullable EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    getOrCreateNodeInfo().setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
  }

  void sendAccessibilityEventHandler(
      @Nullable EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    getOrCreateNodeInfo().setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
  }

  void sendAccessibilityEventUncheckedHandler(
      @Nullable
          EventHandler<SendAccessibilityEventUncheckedEvent>
              sendAccessibilityEventUncheckedHandler) {
    getOrCreateNodeInfo()
        .setSendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);
  }

  void scale(float scale) {
    wrapInView();
    getOrCreateNodeInfo().setScale(scale);
  }

  void alpha(float alpha) {
    wrapInView();
    getOrCreateNodeInfo().setAlpha(alpha);
  }

  void rotation(float rotation) {
    wrapInView();
    getOrCreateNodeInfo().setRotation(rotation);
  }

  void transitionKey(@Nullable String key) {
    getOrCreateOtherProps().transitionKey(key);
  }

  @Override
  @Nullable
  public String getTransitionKey() {
    return getOrCreateOtherProps().mTransitionKey;
  }

  void transitionKeyType(@Nullable Transition.TransitionKeyType type) {
    getOrCreateOtherProps().transitionKeyType(type);
  }

  @Nullable
  public Transition.TransitionKeyType getTransitionKeyType() {
    return getOrCreateOtherProps().mTransitionKeyType;
  }

  @Nullable
  NodeInfo getNullableNodeInfo() {
    return mNodeInfo;
  }

  private NodeInfo getOrCreateNodeInfo() {
    if (mNodeInfo == null) {
      mNodeInfo = new NodeInfo();
    }

    return mNodeInfo;
  }

  @Override
  public void copyInto(ComponentContext c, InternalNode node) {
    c.applyStyle(node, mDefStyleAttr, mDefStyleRes);

    if (mNodeInfo != null) {
      mNodeInfo.copyInto(node);
    }

    if ((mPrivateFlags & PFLAG_BACKGROUND_IS_SET) != 0L) {
      node.background(mBackground);
    }
    if ((mPrivateFlags & PFLAG_TEST_KEY_IS_SET) != 0L) {
      node.testKey(mTestKey);
    }
    if (mWrapInView) {
      node.wrapInView();
    }

    if (mLayoutProps != null) {
      mLayoutProps.copyInto(node);
    }

    if (mOtherProps != null) {
      mOtherProps.copyInto(node);
    }
  }

  private static class OtherProps {
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
    @Nullable private Edges mTouchExpansions;
    @Nullable private ComparableDrawable mForeground;
    @Nullable private String mTransitionKey;
    @Nullable private Transition.TransitionKeyType mTransitionKeyType;
    @Nullable private Border mBorder;
    @Nullable private StateListAnimator mStateListAnimator;
    @DrawableRes private int mStateListAnimatorRes;

    private void importantForAccessibility(int importantForAccessibility) {
      mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
      mImportantForAccessibility = importantForAccessibility;
    }

    private void duplicateParentState(boolean duplicateParentState) {
      mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
      mDuplicateParentState = duplicateParentState;
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

    private void foreground(@Nullable ComparableDrawable foreground) {
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

    private void transitionKey(String key) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
      mTransitionKey = key;
    }

    private void transitionKeyType(Transition.TransitionKeyType type) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_TYPE_IS_SET;
      mTransitionKeyType = type;
    }

    private void stateListAnimator(StateListAnimator stateListAnimator) {
      mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_IS_SET;
      mStateListAnimator = stateListAnimator;
    }

    private void stateListAnimatorRes(@DrawableRes int resId) {
      mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET;
      mStateListAnimatorRes = resId;
    }

    void copyInto(InternalNode node) {
      if ((mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) != 0L) {
        node.importantForAccessibility(mImportantForAccessibility);
      }
      if ((mPrivateFlags & PFLAG_DUPLICATE_PARENT_STATE_IS_SET) != 0L) {
        node.duplicateParentState(mDuplicateParentState);
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
        node.transitionKey(mTransitionKey);
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
    }
  }

  static class LayoutProps {
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
    private static final int PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET = 1 << 29;

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

    void widthPx(@Px int width) {
      mPrivateFlags |= PFLAG_WIDTH_IS_SET;
      mWidthPx = width;
    }

    private void widthPercent(float percent) {
      mPrivateFlags |= PFLAG_WIDTH_PERCENT_IS_SET;
      mWidthPercent = percent;
    }

    private void minWidthPx(@Px int minWidth) {
      mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
      mMinWidthPx = minWidth;
    }

    private void maxWidthPx(@Px int maxWidth) {
      mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
      mMaxWidthPx = maxWidth;
    }

    private void minWidthPercent(float percent) {
      mPrivateFlags |= PFLAG_MIN_WIDTH_PERCENT_IS_SET;
      mMinWidthPercent = percent;
    }

    private void maxWidthPercent(float percent) {
      mPrivateFlags |= PFLAG_MAX_WIDTH_PERCENT_IS_SET;
      mMaxWidthPercent = percent;
    }

    void heightPx(@Px int height) {
      mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
      mHeightPx = height;
    }

    private void heightPercent(float percent) {
      mPrivateFlags |= PFLAG_HEIGHT_PERCENT_IS_SET;
      mHeightPercent = percent;
    }

    private void minHeightPx(@Px int minHeight) {
      mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
      mMinHeightPx = minHeight;
    }

    private void maxHeightPx(@Px int maxHeight) {
      mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
      mMaxHeightPx = maxHeight;
    }

    private void minHeightPercent(float percent) {
      mPrivateFlags |= PFLAG_MIN_HEIGHT_PERCENT_IS_SET;
      mMinHeightPercent = percent;
    }

    private void maxHeightPercent(float percent) {
      mPrivateFlags |= PFLAG_MAX_HEIGHT_PERCENT_IS_SET;
      mMaxHeightPercent = percent;
    }

    private void layoutDirection(YogaDirection direction) {
      mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
      mLayoutDirection = direction;
    }

    private void alignSelf(YogaAlign alignSelf) {
      mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
      mAlignSelf = alignSelf;
    }

    private void flex(float flex) {
      mPrivateFlags |= PFLAG_FLEX_IS_SET;
      mFlex = flex;
    }

    private void flexGrow(float flexGrow) {
      mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
      mFlexGrow = flexGrow;
    }

    private void flexShrink(float flexShrink) {
      mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
      mFlexShrink = flexShrink;
    }

    private void flexBasisPx(@Px int flexBasis) {
      mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
      mFlexBasisPx = flexBasis;
    }

    private void flexBasisPercent(float percent) {
      mPrivateFlags |= PFLAG_FLEX_BASIS_PERCENT_IS_SET;
      mFlexBasisPercent = percent;
    }

    private void aspectRatio(float aspectRatio) {
      mPrivateFlags |= PFLAG_ASPECT_RATIO_IS_SET;
      mAspectRatio = aspectRatio;
    }

    void positionType(@Nullable YogaPositionType positionType) {
      mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
      mPositionType = positionType;
    }

    void positionPx(YogaEdge edge, @Px int position) {
      mPrivateFlags |= PFLAG_POSITION_IS_SET;
      if (mPositions == null) {
        mPositions = new Edges();
      }

      mPositions.set(edge, position);
    }

    private void positionPercent(YogaEdge edge, float percent) {
      mPrivateFlags |= PFLAG_POSITION_PERCENT_IS_SET;
      if (mPositionPercents == null) {
        mPositionPercents = new Edges();
      }
      mPositionPercents.set(edge, percent);
    }

    private void paddingPx(YogaEdge edge, @Px int padding) {
      mPrivateFlags |= PFLAG_PADDING_IS_SET;
      if (mPaddings == null) {
        mPaddings = new Edges();
      }
      mPaddings.set(edge, padding);
    }

    private void paddingPercent(YogaEdge edge, float percent) {
      mPrivateFlags |= PFLAG_PADDING_PERCENT_IS_SET;
      if (mPaddingPercents == null) {
        mPaddingPercents = new Edges();
      }
      mPaddingPercents.set(edge, percent);
    }

    private void marginPx(YogaEdge edge, @Px int margin) {
      mPrivateFlags |= PFLAG_MARGIN_IS_SET;

      if (mMargins == null) {
        mMargins = new Edges();
      }
      mMargins.set(edge, margin);
    }

    private void marginPercent(YogaEdge edge, float percent) {
      mPrivateFlags |= PFLAG_MARGIN_PERCENT_IS_SET;
      if (mMarginPercents == null) {
        mMarginPercents = new Edges();
      }
      mMarginPercents.set(edge, percent);
    }

    private void marginAuto(YogaEdge edge) {
      mPrivateFlags |= PFLAG_MARGIN_AUTO_IS_SET;
      if (mMarginAutos == null) {
        mMarginAutos = new ArrayList<>(2);
      }
      mMarginAutos.add(edge);
    }

    private void isReferenceBaseline(boolean isReferenceBaseline) {
      mPrivateFlags |= PFLAG_IS_REFERENCE_BASELINE_IS_SET;
      mIsReferenceBaseline = isReferenceBaseline;
    }

    private void useHeightAsBaseline(boolean useHeightAsBaseline) {
      mPrivateFlags |= PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET;
      mUseHeightAsBaseline = useHeightAsBaseline;
    }

    void copyInto(InternalNode node) {
      if ((mPrivateFlags & PFLAG_WIDTH_IS_SET) != 0L) {
        node.widthPx(mWidthPx);
      }
      if ((mPrivateFlags & PFLAG_WIDTH_PERCENT_IS_SET) != 0L) {
        node.widthPercent(mWidthPercent);
      }
      if ((mPrivateFlags & PFLAG_MIN_WIDTH_IS_SET) != 0L) {
        node.minWidthPx(mMinWidthPx);
      }
      if ((mPrivateFlags & PFLAG_MIN_WIDTH_PERCENT_IS_SET) != 0L) {
        node.minWidthPercent(mMinWidthPercent);
      }
      if ((mPrivateFlags & PFLAG_MAX_WIDTH_IS_SET) != 0L) {
        node.maxWidthPx(mMaxWidthPx);
      }
      if ((mPrivateFlags & PFLAG_MAX_WIDTH_PERCENT_IS_SET) != 0L) {
        node.maxWidthPercent(mMaxWidthPercent);
      }
      if ((mPrivateFlags & PFLAG_HEIGHT_IS_SET) != 0L) {
        node.heightPx(mHeightPx);
      }
      if ((mPrivateFlags & PFLAG_HEIGHT_PERCENT_IS_SET) != 0L) {
        node.heightPercent(mHeightPercent);
      }
      if ((mPrivateFlags & PFLAG_MIN_HEIGHT_IS_SET) != 0L) {
        node.minHeightPx(mMinHeightPx);
      }
      if ((mPrivateFlags & PFLAG_MIN_HEIGHT_PERCENT_IS_SET) != 0L) {
        node.minHeightPercent(mMinHeightPercent);
      }
      if ((mPrivateFlags & PFLAG_MAX_HEIGHT_IS_SET) != 0L) {
        node.maxHeightPx(mMaxHeightPx);
      }
      if ((mPrivateFlags & PFLAG_MAX_HEIGHT_PERCENT_IS_SET) != 0L) {
        node.maxHeightPercent(mMaxHeightPercent);
      }
      if ((mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) != 0L) {
        node.layoutDirection(mLayoutDirection);
      }
      if ((mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
        node.alignSelf(mAlignSelf);
      }
      if ((mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
        node.flex(mFlex);
      }
      if ((mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
        node.flexGrow(mFlexGrow);
      }
      if ((mPrivateFlags & PFLAG_FLEX_SHRINK_IS_SET) != 0L) {
        node.flexShrink(mFlexShrink);
      }
      if ((mPrivateFlags & PFLAG_FLEX_BASIS_IS_SET) != 0L) {
        node.flexBasisPx(mFlexBasisPx);
      }
      if ((mPrivateFlags & PFLAG_FLEX_BASIS_PERCENT_IS_SET) != 0L) {
        node.flexBasisPercent(mFlexBasisPercent);
      }
      if ((mPrivateFlags & PFLAG_ASPECT_RATIO_IS_SET) != 0L) {
        node.aspectRatio(mAspectRatio);
      }
      if ((mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
        node.positionType(mPositionType);
      }
      if ((mPrivateFlags & PFLAG_POSITION_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mPositions.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            node.positionPx(YogaEdge.fromInt(i), (int) value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_POSITION_PERCENT_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mPositionPercents.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            node.positionPercent(YogaEdge.fromInt(i), value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mPaddings.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            node.paddingPx(YogaEdge.fromInt(i), (int) value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_PADDING_PERCENT_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mPaddingPercents.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            node.paddingPercent(YogaEdge.fromInt(i), value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mMargins.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            node.marginPx(YogaEdge.fromInt(i), (int) value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_MARGIN_PERCENT_IS_SET) != 0L) {
        for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
          final float value = mMarginPercents.getRaw(i);
          if (!YogaConstants.isUndefined(value)) {
            node.marginPercent(YogaEdge.fromInt(i), value);
          }
        }
      }
      if ((mPrivateFlags & PFLAG_MARGIN_AUTO_IS_SET) != 0L) {
        for (YogaEdge edge : mMarginAutos) {
          node.marginAuto(edge);
        }
      }
      if ((mPrivateFlags & PFLAG_IS_REFERENCE_BASELINE_IS_SET) != 0L) {
        node.isReferenceBaseline(mIsReferenceBaseline);
      }
      if ((mPrivateFlags & PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET) != 0L) {
        node.useHeightAsBaselineFunction(mUseHeightAsBaseline);
      }
    }
  }
}
