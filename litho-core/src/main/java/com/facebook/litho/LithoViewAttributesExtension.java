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

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static com.facebook.litho.ComponentHost.COMPONENT_NODE_INFO_ID;
import static com.facebook.litho.LithoMountData.isViewClickable;
import static com.facebook.litho.LithoMountData.isViewEnabled;
import static com.facebook.litho.LithoMountData.isViewFocusable;
import static com.facebook.litho.LithoMountData.isViewLongClickable;
import static com.facebook.litho.LithoMountData.isViewSelected;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.LithoViewAttributesExtension.LithoViewAttributesState;
import com.facebook.litho.LithoViewAttributesExtension.ViewAttributesInput;
import com.facebook.rendercore.ErrorReporter;
import com.facebook.rendercore.LogLevel;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.OnItemCallbacks;
import com.facebook.rendercore.primitives.utils.EquivalenceUtils;
import java.util.HashMap;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoViewAttributesExtension
    extends MountExtension<ViewAttributesInput, LithoViewAttributesState>
    implements OnItemCallbacks<LithoViewAttributesState> {

  private static final LithoViewAttributesExtension sInstance = new LithoViewAttributesExtension();

  private LithoViewAttributesExtension() {}

  static LithoViewAttributesExtension getInstance() {
    return sInstance;
  }

  @Override
  protected LithoViewAttributesState createState() {
    return new LithoViewAttributesState();
  }

  static class LithoViewAttributesState {
    private Map<Long, Integer> mDefaultViewAttributes = new HashMap<>();
    private @Nullable Map<Long, ViewAttributes> mCurentUnits;
    private @Nullable Map<Long, ViewAttributes> mNewUnits;

    void setDefaultViewAttributes(long renderUnitId, int flags) {
      mDefaultViewAttributes.put(renderUnitId, flags);
    }

    int getDefaultViewAttributes(long renderUnitId) {
      final Integer flags = mDefaultViewAttributes.get(renderUnitId);
      if (flags == null) {
        throw new IllegalStateException(
            "View attributes not found, did you call onUnbindItem without onBindItem?");
      }

      return flags;
    }

    boolean hasDefaultViewAttributes(long renderUnitId) {
      return mDefaultViewAttributes.containsKey(renderUnitId);
    }

    @Nullable
    ViewAttributes getCurrentViewAttributes(long id) {
      return mCurentUnits != null ? mCurentUnits.get(id) : null;
    }

    @Nullable
    ViewAttributes getNewViewAttributes(long id) {
      return mNewUnits != null ? mNewUnits.get(id) : null;
    }
  }

  @Override
  public void beforeMount(
      final ExtensionState<LithoViewAttributesState> extensionState,
      final @Nullable ViewAttributesInput viewAttributesInput,
      final Rect localVisibleRect) {
    if (viewAttributesInput != null) {
      extensionState.getState().mNewUnits = viewAttributesInput.getViewAttributes();
    }
  }

  @Override
  public void afterMount(ExtensionState<LithoViewAttributesState> extensionState) {
    extensionState.getState().mCurentUnits = extensionState.getState().mNewUnits;
  }

  @Override
  public void onMountItem(
      final ExtensionState<LithoViewAttributesState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final LithoViewAttributesState state = extensionState.getState();
    final long id = renderUnit.getId();
    final @Nullable ViewAttributes viewAttributes = state.getNewViewAttributes(id);

    if (viewAttributes != null) {
      // Get the initial view attribute flags for the root LithoView.
      if (!state.hasDefaultViewAttributes(id)) {
        final int flags;
        if (renderUnit.getId() == ROOT_HOST_ID) {
          flags = ((LithoView) content).mViewAttributeFlags;
        } else {
          flags = LithoMountData.getViewAttributeFlags(content);
        }
        state.setDefaultViewAttributes(id, flags);
      }
      setViewAttributes(content, viewAttributes, renderUnit);
    }
  }

  @Override
  public void onUnmountItem(
      final ExtensionState<LithoViewAttributesState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final LithoViewAttributesState state = extensionState.getState();
    final long id = renderUnit.getId();
    final @Nullable ViewAttributes viewAttributes = state.getCurrentViewAttributes(id);

    if (viewAttributes != null) {
      final int flags = state.getDefaultViewAttributes(id);
      unsetViewAttributes(content, viewAttributes, flags);
    }
  }

  @Override
  public void beforeMountItem(
      ExtensionState<LithoViewAttributesState> extensionState,
      RenderTreeNode renderTreeNode,
      int index) {}

  @Override
  public void onBindItem(
      ExtensionState<LithoViewAttributesState> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  @Override
  public void onUnbindItem(
      ExtensionState<LithoViewAttributesState> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  @Override
  public void onBoundsAppliedToItem(
      ExtensionState<LithoViewAttributesState> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  @Override
  public boolean shouldUpdateItem(
      final ExtensionState<LithoViewAttributesState> extensionState,
      final RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData) {
    if (previousRenderUnit == nextRenderUnit) {
      return false;
    }

    final long id = previousRenderUnit.getId();
    final LithoViewAttributesState state = extensionState.getState();
    final @Nullable ViewAttributes currentAttributes = state.getCurrentViewAttributes(id);
    final @Nullable ViewAttributes nextAttributes = state.getNewViewAttributes(id);
    if (previousRenderUnit instanceof LithoRenderUnit
        && nextRenderUnit instanceof LithoRenderUnit) {
      return (previousRenderUnit instanceof MountSpecLithoRenderUnit
              && nextRenderUnit instanceof MountSpecLithoRenderUnit
              && MountSpecLithoRenderUnit.shouldUpdateMountItem(
                  (MountSpecLithoRenderUnit) previousRenderUnit,
                  (MountSpecLithoRenderUnit) nextRenderUnit,
                  previousLayoutData,
                  nextLayoutData))
          || shouldUpdateViewInfo(nextAttributes, currentAttributes);
    } else {
      return shouldUpdateViewInfo(nextAttributes, currentAttributes);
    }
  }

  @Override
  public void onUnmount(ExtensionState<LithoViewAttributesState> extensionState) {
    extensionState.getState().mCurentUnits = null;
    extensionState.getState().mNewUnits = null;
  }

  static void setViewAttributes(Object content, ViewAttributes attributes, RenderUnit<?> unit) {
    if (!(content instanceof View)) {
      return;
    }

    final View view = (View) content;

    setClickHandler(attributes.getClickHandler(), view);
    setLongClickHandler(attributes.getLongClickHandler(), view);
    setFocusChangeHandler(attributes.getFocusChangeHandler(), view);
    setTouchHandler(attributes.getTouchHandler(), view);
    setInterceptTouchHandler(attributes.getInterceptTouchHandler(), view);

    if (unit instanceof LithoRenderUnit) {
      final NodeInfo nodeInfo = ((LithoRenderUnit) unit).getNodeInfo();
      if (nodeInfo != null) setAccessibilityDelegate(view, nodeInfo);
    }

    setViewId(view, attributes.getViewId());
    if (attributes.isTagSet()) {
      setViewTag(view, attributes.getViewTag());
    }
    setViewTags(view, attributes.getViewTags());

    setShadowElevation(view, attributes.getShadowElevation());
    setAmbientShadowColor(view, attributes.getAmbientShadowColor());
    setSpotShadowColor(view, attributes.getSpotShadowColor());
    setOutlineProvider(view, attributes.getOutlineProvider());
    setClipToOutline(view, attributes.getClipToOutline());
    setClipChildren(view, attributes);

    setContentDescription(view, attributes.getContentDescription());

    setFocusable(view, attributes);
    setClickable(view, attributes);
    setEnabled(view, attributes);
    setSelected(view, attributes);
    setScale(view, attributes);
    setAlpha(view, attributes);
    setRotation(view, attributes);
    setRotationX(view, attributes);
    setRotationY(view, attributes);
    setTransitionName(view, attributes.getTransitionName());

    setImportantForAccessibility(view, attributes.getImportantForAccessibility());

    final boolean isHostSpec = attributes.isHostSpec();
    setViewLayerType(view, attributes);
    setViewStateListAnimator(view, attributes);
    if (attributes.getDisableDrawableOutputs()) {
      setViewBackground(view, attributes);
      ViewUtils.setViewForeground(view, attributes.getForeground());

      // when background outputs are disabled, they are wrapped by a ComponentHost.
      // A background can set the padding of a view, but ComponentHost should not have
      // any padding because the layout calculation has already accounted for padding by
      // translating the bounds of its children.
      if (isHostSpec) {
        view.setPadding(0, 0, 0, 0);
      }
    }
    if (!isHostSpec) {
      // Set view background, if applicable.  Do this before padding
      // as it otherwise overrides the padding.
      setViewBackground(view, attributes);

      setViewPadding(view, attributes);

      ViewUtils.setViewForeground(view, attributes.getForeground());

      setViewLayoutDirection(view, attributes);
    }
  }

  static void unsetViewAttributes(
      final Object content, final ViewAttributes attributes, final int mountFlags) {
    final boolean isHostView = attributes.isHostSpec();

    if (!(content instanceof View)) {
      return;
    }

    final View view = (View) content;

    if (attributes.getClickHandler() != null) {
      unsetClickHandler(view);
    }

    if (attributes.getLongClickHandler() != null) {
      unsetLongClickHandler(view);
    }

    if (attributes.getFocusChangeHandler() != null) {
      unsetFocusChangeHandler(view);
    }

    if (attributes.getTouchHandler() != null) {
      unsetTouchHandler(view);
    }

    if (attributes.getInterceptTouchHandler() != null) {
      unsetInterceptTouchEventHandler(view);
    }

    if (attributes.isViewIdSet()) {
      unsetViewId(view);
    }

    if (attributes.isTagSet()) {
      unsetViewTag(view);
    }
    unsetViewTags(view, attributes.getViewTags());

    unsetShadowElevation(view, attributes.getShadowElevation());
    unsetAmbientShadowColor(view, attributes.getAmbientShadowColor());
    unsetSpotShadowColor(view, attributes.getSpotShadowColor());
    unsetOutlineProvider(view, attributes.getOutlineProvider());
    unsetClipToOutline(view, attributes.getClipToOutline());
    unsetClipChildren(view, attributes.getClipChildren());

    if (!TextUtils.isEmpty(attributes.getContentDescription())) {
      unsetContentDescription(view);
    }

    unsetScale(view, attributes);
    unsetAlpha(view, attributes);
    unsetRotation(view, attributes);
    unsetRotationX(view, attributes);
    unsetRotationY(view, attributes);

    view.setClickable(isViewClickable(mountFlags));
    view.setLongClickable(isViewLongClickable(mountFlags));

    unsetFocusable(view, mountFlags);
    unsetEnabled(view, mountFlags);
    unsetSelected(view, mountFlags);

    if (attributes.getImportantForAccessibility() != IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      unsetImportantForAccessibility(view);
    }

    unsetAccessibilityDelegate(view);

    unsetViewStateListAnimator(view, attributes);
    // Host view doesn't set its own padding, but gets absolute positions for inner content from
    // Yoga. Also bg/fg is used as separate drawables instead of using View's bg/fg attribute.
    if (attributes.getDisableDrawableOutputs()) {
      unsetViewBackground(view, attributes);
      unsetViewForeground(view, attributes);
    }
    if (!isHostView) {
      unsetViewPadding(view, attributes);
      unsetViewBackground(view, attributes);
      unsetViewForeground(view, attributes);
      unsetViewLayoutDirection(view);
    }

    unsetViewLayerType(view, mountFlags);
  }

  /**
   * Store a {@link NodeInfo} as a tag in {@code view}. {@link LithoView} contains the logic for
   * setting/unsetting it whenever accessibility is enabled/disabled
   *
   * <p>For non {@link ComponentHost}s this is only done if any {@link EventHandler}s for
   * accessibility events have been implemented, we want to preserve the original behaviour since
   * {@code view} might have had a default delegate.
   */
  private static void setAccessibilityDelegate(View view, NodeInfo nodeInfo) {
    if (!(view instanceof ComponentHost) && !nodeInfo.needsAccessibilityDelegate()) {
      return;
    }

    view.setTag(COMPONENT_NODE_INFO_ID, nodeInfo);
  }

  private static void unsetAccessibilityDelegate(View view) {
    if (!(view instanceof ComponentHost) && view.getTag(COMPONENT_NODE_INFO_ID) == null) {
      return;
    }
    view.setTag(COMPONENT_NODE_INFO_ID, null);
    if (!(view instanceof ComponentHost)) {
      ViewCompat.setAccessibilityDelegate(view, null);
    }
  }

  /**
   * Installs the click listeners that will dispatch the click handler defined in the component's
   * props. Unconditionally set the clickable flag on the view.
   */
  private static void setClickHandler(@Nullable EventHandler<ClickEvent> clickHandler, View view) {
    if (clickHandler == null) {
      return;
    }

    view.setOnClickListener(new ComponentClickListener(clickHandler));
    view.setClickable(true);
  }

  private static void unsetClickHandler(View view) {
    view.setOnClickListener(null);
    view.setClickable(false);
  }

  /**
   * Installs the long click listeners that will dispatch the click handler defined in the
   * component's props. Unconditionally set the clickable flag on the view.
   */
  private static void setLongClickHandler(
      @Nullable EventHandler<LongClickEvent> longClickHandler, View view) {
    if (longClickHandler != null) {
      ComponentLongClickListener listener = getComponentLongClickListener(view);

      if (listener == null) {
        listener = new ComponentLongClickListener();
        setComponentLongClickListener(view, listener);
      }

      listener.setEventHandler(longClickHandler);

      view.setLongClickable(true);
    }
  }

  private static void unsetLongClickHandler(View view) {
    final ComponentLongClickListener listener = getComponentLongClickListener(view);

    if (listener != null) {
      listener.setEventHandler(null);
    }
  }

  @Nullable
  static ComponentLongClickListener getComponentLongClickListener(View v) {
    if (v instanceof ComponentHost) {
      return ((ComponentHost) v).getComponentLongClickListener();
    } else {
      return (ComponentLongClickListener) v.getTag(R.id.component_long_click_listener);
    }
  }

  static void setComponentLongClickListener(View v, ComponentLongClickListener listener) {
    if (v instanceof ComponentHost) {
      ((ComponentHost) v).setComponentLongClickListener(listener);
    } else {
      v.setOnLongClickListener(listener);
      v.setTag(R.id.component_long_click_listener, listener);
    }
  }

  /**
   * Installs the on focus change listeners that will dispatch the click handler defined in the
   * component's props. Unconditionally set the clickable flag on the view.
   */
  private static void setFocusChangeHandler(
      @Nullable EventHandler<FocusChangedEvent> focusChangeHandler, View view) {
    if (focusChangeHandler == null) {
      return;
    }

    ComponentFocusChangeListener listener = getComponentFocusChangeListener(view);

    if (listener == null) {
      listener = new ComponentFocusChangeListener();
      setComponentFocusChangeListener(view, listener);
    }

    listener.setEventHandler(focusChangeHandler);
  }

  private static void unsetFocusChangeHandler(View view) {
    final ComponentFocusChangeListener listener = getComponentFocusChangeListener(view);

    if (listener != null) {
      listener.setEventHandler(null);
    }
  }

  static @Nullable ComponentFocusChangeListener getComponentFocusChangeListener(View v) {
    if (v instanceof ComponentHost) {
      return ((ComponentHost) v).getComponentFocusChangeListener();
    } else {
      return (ComponentFocusChangeListener) v.getTag(R.id.component_focus_change_listener);
    }
  }

  static void setComponentFocusChangeListener(View v, ComponentFocusChangeListener listener) {
    if (v instanceof ComponentHost) {
      ((ComponentHost) v).setComponentFocusChangeListener(listener);
    } else {
      v.setOnFocusChangeListener(listener);
      v.setTag(R.id.component_focus_change_listener, listener);
    }
  }

  /**
   * Installs the touch listeners that will dispatch the touch handler defined in the component's
   * props.
   */
  private static void setTouchHandler(@Nullable EventHandler<TouchEvent> touchHandler, View view) {
    if (touchHandler != null) {
      ComponentTouchListener listener = getComponentTouchListener(view);

      if (listener == null) {
        listener = new ComponentTouchListener();
        setComponentTouchListener(view, listener);
      }

      listener.setEventHandler(touchHandler);
    }
  }

  private static void unsetTouchHandler(View view) {
    final ComponentTouchListener listener = getComponentTouchListener(view);

    if (listener != null) {
      listener.setEventHandler(null);
    }
  }

  /** Sets the intercept touch handler defined in the component's props. */
  private static void setInterceptTouchHandler(
      @Nullable EventHandler<InterceptTouchEvent> interceptTouchHandler, View view) {
    if (interceptTouchHandler == null) {
      return;
    }

    if (view instanceof ComponentHost) {
      ((ComponentHost) view).setInterceptTouchEventHandler(interceptTouchHandler);
    }
  }

  private static void unsetInterceptTouchEventHandler(View view) {
    if (view instanceof ComponentHost) {
      ((ComponentHost) view).setInterceptTouchEventHandler(null);
    }
  }

  @Nullable
  static ComponentTouchListener getComponentTouchListener(View v) {
    if (v instanceof ComponentHost) {
      return ((ComponentHost) v).getComponentTouchListener();
    } else {
      return (ComponentTouchListener) v.getTag(R.id.component_touch_listener);
    }
  }

  static void setComponentTouchListener(View v, ComponentTouchListener listener) {
    if (v instanceof ComponentHost) {
      ((ComponentHost) v).setComponentTouchListener(listener);
    } else {
      v.setOnTouchListener(listener);
      v.setTag(R.id.component_touch_listener, listener);
    }
  }

  private static void setViewId(View view, @IdRes int id) {
    if (id != View.NO_ID) {
      view.setId(id);
    }
  }

  private static void unsetViewId(View view) {
    view.setId(View.NO_ID);
  }

  private static void setViewTag(View view, @Nullable Object viewTag) {
    view.setTag(viewTag);
  }

  private static void setViewTags(View view, @Nullable SparseArray<Object> viewTags) {
    if (viewTags == null) {
      return;
    }

    if (view instanceof ComponentHost) {
      final ComponentHost host = (ComponentHost) view;
      host.setViewTags(viewTags);
    } else {
      for (int i = 0, size = viewTags.size(); i < size; i++) {
        view.setTag(viewTags.keyAt(i), viewTags.valueAt(i));
      }
    }
  }

  private static void unsetViewTag(View view) {
    view.setTag(null);
  }

  private static void unsetViewTags(View view, @Nullable SparseArray<Object> viewTags) {
    if (view instanceof ComponentHost) {
      final ComponentHost host = (ComponentHost) view;
      host.setViewTags(null);
    } else {
      if (viewTags != null) {
        for (int i = 0, size = viewTags.size(); i < size; i++) {
          view.setTag(viewTags.keyAt(i), null);
        }
      }
    }
  }

  private static void setShadowElevation(View view, float shadowElevation) {
    if (shadowElevation != 0) {
      ViewCompat.setElevation(view, shadowElevation);
    }
  }

  private static void setAmbientShadowColor(View view, @ColorInt int ambientShadowColor) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      view.setOutlineAmbientShadowColor(ambientShadowColor);
    }
  }

  private static void setSpotShadowColor(View view, @ColorInt int spotShadowColor) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      view.setOutlineSpotShadowColor(spotShadowColor);
    }
  }

  private static void unsetShadowElevation(View view, float shadowElevation) {
    if (shadowElevation != 0) {
      ViewCompat.setElevation(view, 0);
    }
  }

  private static void unsetAmbientShadowColor(View view, @ColorInt int ambientShadowColor) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ambientShadowColor != Color.BLACK) {
      // Android documentation says black is the default:
      // https://developer.android.com/reference/android/view/View#getOutlineAmbientShadowColor()
      view.setOutlineAmbientShadowColor(Color.BLACK);
    }
  }

  private static void unsetSpotShadowColor(View view, @ColorInt int spotShadowColor) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && spotShadowColor != Color.BLACK) {
      // Android documentation says black is the default:
      // https://developer.android.com/reference/android/view/View#getOutlineSpotShadowColor()
      view.setOutlineSpotShadowColor(Color.BLACK);
    }
  }

  private static void setOutlineProvider(View view, @Nullable ViewOutlineProvider outlineProvider) {
    if (outlineProvider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setOutlineProvider(outlineProvider);
    }
  }

  private static void unsetOutlineProvider(
      View view, @Nullable ViewOutlineProvider outlineProvider) {
    if (outlineProvider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
    }
  }

  private static void setClipToOutline(View view, boolean clipToOutline) {
    if (clipToOutline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setClipToOutline(clipToOutline);
    }
  }

  private static void unsetClipToOutline(View view, boolean clipToOutline) {
    if (clipToOutline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setClipToOutline(false);
    }
  }

  private static void setClipChildren(View view, ViewAttributes attributes) {
    if (attributes.isClipChildrenSet() && view instanceof ViewGroup) {
      ((ViewGroup) view).setClipChildren(attributes.getClipChildren());
    }
  }

  private static void unsetClipChildren(View view, boolean clipChildren) {
    if (!clipChildren && view instanceof ViewGroup) {
      // Default value for clipChildren is 'true'.
      // If this ViewGroup had clipChildren set to 'false' before mounting we would reset this
      // property here on recycling.
      ((ViewGroup) view).setClipChildren(true);
    }
  }

  private static void setContentDescription(View view, @Nullable CharSequence contentDescription) {
    if (TextUtils.isEmpty(contentDescription)) {
      return;
    }

    view.setContentDescription(contentDescription);
  }

  private static void unsetContentDescription(View view) {
    view.setContentDescription(null);
  }

  private static void setImportantForAccessibility(View view, int importantForAccessibility) {
    if (importantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      return;
    }

    ViewCompat.setImportantForAccessibility(view, importantForAccessibility);
  }

  private static void unsetImportantForAccessibility(View view) {
    ViewCompat.setImportantForAccessibility(view, IMPORTANT_FOR_ACCESSIBILITY_AUTO);
  }

  private static void setFocusable(View view, ViewAttributes attributes) {
    if (attributes.isFocusableSet()) {
      view.setFocusable(attributes.isFocusable());
    }
  }

  private static void unsetFocusable(View view, int flags) {
    view.setFocusable(isViewFocusable(flags));
  }

  private static void setTransitionName(View view, @Nullable String transitionName) {
    if (transitionName != null) {
      ViewCompat.setTransitionName(view, transitionName);
    }
  }

  private static void setClickable(View view, ViewAttributes attributes) {
    if (attributes.isClickableSet()) {
      view.setClickable(attributes.isClickable());
    }
  }

  private static void setEnabled(View view, ViewAttributes attributes) {
    if (attributes.isEnabledSet()) {
      view.setEnabled(attributes.isEnabled());
    }
  }

  private static void unsetEnabled(View view, int flags) {
    view.setEnabled(isViewEnabled(flags));
  }

  private static void setSelected(View view, ViewAttributes attributes) {
    if (attributes.isSelectedSet()) {
      view.setSelected(attributes.isSelected());
    }
  }

  private static void unsetSelected(View view, int flags) {
    view.setSelected(isViewSelected(flags));
  }

  private static void setScale(View view, ViewAttributes attributes) {
    if (attributes.isScaleSet()) {
      final float scale = attributes.getScale();
      view.setScaleX(scale);
      view.setScaleY(scale);
    }
  }

  private static void unsetScale(View view, ViewAttributes attributes) {
    if (attributes.isScaleSet()) {
      if (view.getScaleX() != 1) {
        view.setScaleX(1);
      }
      if (view.getScaleY() != 1) {
        view.setScaleY(1);
      }
    }
  }

  private static void setAlpha(View view, ViewAttributes attributes) {
    if (attributes.isAlphaSet()) {
      view.setAlpha(attributes.getAlpha());
    }
  }

  private static void unsetAlpha(View view, ViewAttributes attributes) {
    if (attributes.isAlphaSet() && view.getAlpha() != 1) {
      view.setAlpha(1);
    }
  }

  private static void setRotation(View view, ViewAttributes attributes) {
    if (attributes.isRotationSet()) {
      view.setRotation(attributes.getRotation());
    }
  }

  private static void unsetRotation(View view, ViewAttributes attributes) {
    if (attributes.isRotationSet() && view.getRotation() != 0) {
      view.setRotation(0);
    }
  }

  private static void setRotationX(View view, ViewAttributes attributes) {
    if (attributes.isRotationXSet()) {
      view.setRotationX(attributes.getRotationX());
    }
  }

  private static void unsetRotationX(View view, ViewAttributes attributes) {
    if (attributes.isRotationXSet() && view.getRotationX() != 0) {
      view.setRotationX(0);
    }
  }

  private static void setRotationY(View view, ViewAttributes attributes) {
    if (attributes.isRotationYSet()) {
      view.setRotationY(attributes.getRotationY());
    }
  }

  private static void unsetRotationY(View view, ViewAttributes attributes) {
    if (attributes.isRotationYSet() && view.getRotationY() != 0) {
      view.setRotationY(0);
    }
  }

  private static void setViewPadding(View view, ViewAttributes attributes) {
    if (!attributes.hasPadding()) {
      return;
    }

    view.setPadding(
        attributes.getPaddingLeft(),
        attributes.getPaddingTop(),
        attributes.getPaddingRight(),
        attributes.getPaddingBottom());
  }

  private static void unsetViewPadding(View view, ViewAttributes attributes) {
    if (!attributes.hasPadding()) {
      return;
    }

    try {
      view.setPadding(0, 0, 0, 0);
    } catch (NullPointerException e) {
      // T53931759 Gathering extra info around this NPE
      ErrorReporter.getInstance()
          .report(
              LogLevel.ERROR,
              "LITHO:NPE:UNSET_PADDING",
              "From component: " + attributes.getComponentName(),
              e,
              0,
              null);
    }
  }

  private static void setViewBackground(View view, ViewAttributes attributes) {
    final Drawable background = attributes.getBackground();
    if (background != null) {
      setBackgroundCompat(view, background);
    }
  }

  private static void unsetViewBackground(View view, ViewAttributes attributes) {
    final Drawable background = attributes.getBackground();
    if (background != null) {
      setBackgroundCompat(view, null);
    }
  }

  @SuppressWarnings("deprecation")
  private static void setBackgroundCompat(View view, @Nullable Drawable drawable) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      view.setBackgroundDrawable(drawable);
    } else {
      view.setBackground(drawable);
    }
  }

  private static void unsetViewForeground(View view, ViewAttributes attributes) {
    final Drawable foreground = attributes.getForeground();
    if (foreground != null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        throw new IllegalStateException(
            "MountState has a ViewAttributes with foreground however "
                + "the current Android version doesn't support foreground on Views");
      }

      view.setForeground(null);
    }
  }

  private static void setViewLayoutDirection(View view, ViewAttributes attributes) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return;
    }

    final int viewLayoutDirection;
    switch (attributes.getLayoutDirection()) {
      case LTR:
        viewLayoutDirection = View.LAYOUT_DIRECTION_LTR;
        break;
      case RTL:
        viewLayoutDirection = View.LAYOUT_DIRECTION_RTL;
        break;
      default:
        viewLayoutDirection = View.LAYOUT_DIRECTION_INHERIT;
    }

    view.setLayoutDirection(viewLayoutDirection);
  }

  private static void unsetViewLayoutDirection(View view) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return;
    }

    view.setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
  }

  private static void setViewStateListAnimator(View view, ViewAttributes attributes) {
    StateListAnimator stateListAnimator = attributes.getStateListAnimator();
    final int stateListAnimatorRes = attributes.getStateListAnimatorRes();
    if (stateListAnimator == null && stateListAnimatorRes == 0) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      throw new IllegalStateException(
          "MountState has a ViewAttributes with stateListAnimator, "
              + "however the current Android version doesn't support stateListAnimator on Views");
    }
    if (stateListAnimator == null) {
      stateListAnimator =
          AnimatorInflater.loadStateListAnimator(view.getContext(), stateListAnimatorRes);
    }
    view.setStateListAnimator(stateListAnimator);
  }

  private static void unsetViewStateListAnimator(View view, ViewAttributes attributes) {
    if (attributes.getStateListAnimator() == null && attributes.getStateListAnimatorRes() == 0) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      throw new IllegalStateException(
          "MountState has a ViewAttributes with stateListAnimator, "
              + "however the current Android version doesn't support stateListAnimator on Views");
    }
    view.setStateListAnimator(null);
  }

  private static void setViewLayerType(final View view, final ViewAttributes attributes) {
    final int type = attributes.getLayerType();
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      view.setLayerType(attributes.getLayerType(), attributes.getLayoutPaint());
    }
  }

  private static void unsetViewLayerType(final View view, final int mountFlags) {
    int type = LithoMountData.getOriginalLayerType(mountFlags);
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      view.setLayerType(type, null);
    }
  }

  static boolean shouldUpdateViewInfo(
      @Nullable final ViewAttributes nextAttributes,
      @Nullable final ViewAttributes currentAttributes) {
    return !EquivalenceUtils.equals(currentAttributes, nextAttributes);
  }

  public interface ViewAttributesInput {
    Map<Long, ViewAttributes> getViewAttributes();
  }
}
