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
import static com.facebook.litho.Component.isHostSpec;
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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.ErrorReporter;
import com.facebook.rendercore.LogLevel;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import java.util.HashMap;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoViewAttributesExtension
    extends MountExtension<Void, LithoViewAttributesExtension.LithoViewAttributesState> {

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
  }

  @Override
  public void onMountItem(
      final ExtensionState<LithoViewAttributesState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    if (renderUnit instanceof LithoRenderUnit) {
      final LithoRenderUnit lithoRenderUnit = (LithoRenderUnit) renderUnit;
      final LayoutOutput output = lithoRenderUnit.getLayoutOutput();
      final LithoViewAttributesState state = extensionState.getState();
      final long id = lithoRenderUnit.getId();

      if (!state.hasDefaultViewAttributes(id)) {

        final int flags;

        // Get the initial view attribute flags for the root LithoView.
        if (renderUnit.getId() == ROOT_HOST_ID) {
          flags = ((LithoView) content).mViewAttributeFlags;
        } else {
          flags = LithoMountData.getViewAttributeFlags(content);
        }

        state.setDefaultViewAttributes(id, flags);
      }

      setViewAttributes(content, output);
    }
  }

  @Override
  public void onUnmountItem(
      final ExtensionState<LithoViewAttributesState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    if (renderUnit instanceof LithoRenderUnit) {
      final LithoRenderUnit lithoRenderUnit = (LithoRenderUnit) renderUnit;
      final LayoutOutput output = lithoRenderUnit.getLayoutOutput();
      final LithoViewAttributesState state = extensionState.getState();
      final int flags = state.getDefaultViewAttributes(lithoRenderUnit.getId());
      unsetViewAttributes(content, output, flags);
    }
  }

  @Override
  public boolean shouldUpdateItem(
      final RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData) {
    if (previousRenderUnit == nextRenderUnit) {
      return false;
    }

    final LithoRenderUnit prevLithoRenderUnit = (LithoRenderUnit) previousRenderUnit;
    final LithoRenderUnit nextLithoRenderUnit = (LithoRenderUnit) nextRenderUnit;

    return (previousRenderUnit instanceof MountSpecLithoRenderUnit
            && nextLithoRenderUnit instanceof MountSpecLithoRenderUnit
            && MountSpecLithoRenderUnit.shouldUpdateMountItem(
                (MountSpecLithoRenderUnit) prevLithoRenderUnit,
                (MountSpecLithoRenderUnit) nextLithoRenderUnit,
                previousLayoutData,
                nextLayoutData))
        || shouldUpdateViewInfo(
            nextLithoRenderUnit.getLayoutOutput(), prevLithoRenderUnit.getLayoutOutput());
  }

  static void setViewAttributes(Object content, LayoutOutput output) {
    final Component component = output.getComponent();
    if (!(content instanceof View)) {
      return;
    }

    final View view = (View) content;
    final NodeInfo nodeInfo = output.getNodeInfo();

    if (nodeInfo != null) {
      setClickHandler(nodeInfo.getClickHandler(), view);
      setLongClickHandler(nodeInfo.getLongClickHandler(), view);
      setFocusChangeHandler(nodeInfo.getFocusChangeHandler(), view);
      setTouchHandler(nodeInfo.getTouchHandler(), view);
      setInterceptTouchHandler(nodeInfo.getInterceptTouchHandler(), view);

      setAccessibilityDelegate(view, nodeInfo);

      setViewTag(view, nodeInfo.getViewTag());
      setViewTags(view, nodeInfo.getViewTags());

      setShadowElevation(view, nodeInfo.getShadowElevation());
      setAmbientShadowColor(view, nodeInfo.getAmbientShadowColor());
      setSpotShadowColor(view, nodeInfo.getSpotShadowColor());
      setOutlineProvider(view, nodeInfo.getOutlineProvider());
      setClipToOutline(view, nodeInfo.getClipToOutline());
      setClipChildren(view, nodeInfo);

      setContentDescription(view, nodeInfo.getContentDescription());

      setFocusable(view, nodeInfo.getFocusState());
      setClickable(view, nodeInfo.getClickableState());
      setEnabled(view, nodeInfo.getEnabledState());
      setSelected(view, nodeInfo.getSelectedState());
      setScale(view, nodeInfo);
      setAlpha(view, nodeInfo);
      setRotation(view, nodeInfo);
      setRotationX(view, nodeInfo);
      setRotationY(view, nodeInfo);
      setTransitionName(view, nodeInfo.getTransitionName());
    }

    setImportantForAccessibility(view, output.getImportantForAccessibility());

    final ViewNodeInfo viewNodeInfo = output.getViewNodeInfo();
    if (viewNodeInfo != null) {
      final boolean isHostSpec = isHostSpec(component);
      setViewLayerType(view, viewNodeInfo);
      setViewStateListAnimator(view, viewNodeInfo);
      if (LayoutOutput.areDrawableOutputsDisabled(output.getFlags())) {
        setViewBackground(view, viewNodeInfo);
        ViewUtils.setViewForeground(view, viewNodeInfo.getForeground());

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
        setViewBackground(view, viewNodeInfo);

        setViewPadding(view, viewNodeInfo);

        ViewUtils.setViewForeground(view, viewNodeInfo.getForeground());

        setViewLayoutDirection(view, viewNodeInfo);
      }
    }
  }

  static void unsetViewAttributes(
      final Object content, final LayoutOutput output, final int mountFlags) {
    final Component component = output.getComponent();
    final boolean isHostView = isHostSpec(component);

    if (!(content instanceof View)) {
      return;
    }

    final View view = (View) content;
    final NodeInfo nodeInfo = output.getNodeInfo();

    if (nodeInfo != null) {
      if (nodeInfo.getClickHandler() != null) {
        unsetClickHandler(view);
      }

      if (nodeInfo.getLongClickHandler() != null) {
        unsetLongClickHandler(view);
      }

      if (nodeInfo.getFocusChangeHandler() != null) {
        unsetFocusChangeHandler(view);
      }

      if (nodeInfo.getTouchHandler() != null) {
        unsetTouchHandler(view);
      }

      if (nodeInfo.getInterceptTouchHandler() != null) {
        unsetInterceptTouchEventHandler(view);
      }

      unsetViewTag(view);
      unsetViewTags(view, nodeInfo.getViewTags());

      unsetShadowElevation(view, nodeInfo.getShadowElevation());
      unsetAmbientShadowColor(view, nodeInfo.getAmbientShadowColor());
      unsetSpotShadowColor(view, nodeInfo.getSpotShadowColor());
      unsetOutlineProvider(view, nodeInfo.getOutlineProvider());
      unsetClipToOutline(view, nodeInfo.getClipToOutline());
      unsetClipChildren(view, nodeInfo.getClipChildren());

      if (!TextUtils.isEmpty(nodeInfo.getContentDescription())) {
        unsetContentDescription(view);
      }

      unsetScale(view, nodeInfo);
      unsetAlpha(view, nodeInfo);
      unsetRotation(view, nodeInfo);
      unsetRotationX(view, nodeInfo);
      unsetRotationY(view, nodeInfo);
    }

    view.setClickable(isViewClickable(mountFlags));
    view.setLongClickable(isViewLongClickable(mountFlags));

    unsetFocusable(view, mountFlags);
    unsetEnabled(view, mountFlags);
    unsetSelected(view, mountFlags);

    if (output.getImportantForAccessibility() != IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      unsetImportantForAccessibility(view);
    }

    unsetAccessibilityDelegate(view);

    final ViewNodeInfo viewNodeInfo = output.getViewNodeInfo();
    if (viewNodeInfo != null) {
      unsetViewStateListAnimator(view, viewNodeInfo);
      // Host view doesn't set its own padding, but gets absolute positions for inner content from
      // Yoga. Also bg/fg is used as separate drawables instead of using View's bg/fg attribute.
      if (LayoutOutput.areDrawableOutputsDisabled(output.getFlags())) {
        unsetViewBackground(view, viewNodeInfo);
        unsetViewForeground(view, viewNodeInfo);
      }
      if (!isHostView) {
        unsetViewPadding(view, output, viewNodeInfo);
        unsetViewBackground(view, viewNodeInfo);
        unsetViewForeground(view, viewNodeInfo);
        unsetViewLayoutDirection(view);
      }
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

  private static void setClipChildren(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isClipChildrenSet() && view instanceof ViewGroup) {
      ((ViewGroup) view).setClipChildren(nodeInfo.getClipChildren());
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

  private static void setFocusable(View view, @NodeInfo.FocusState int focusState) {
    if (focusState == NodeInfo.FOCUS_SET_TRUE) {
      view.setFocusable(true);
    } else if (focusState == NodeInfo.FOCUS_SET_FALSE) {
      view.setFocusable(false);
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

  private static void setClickable(View view, @NodeInfo.ClickableState int clickableState) {
    if (clickableState == NodeInfo.CLICKABLE_SET_TRUE) {
      view.setClickable(true);
    } else if (clickableState == NodeInfo.CLICKABLE_SET_FALSE) {
      view.setClickable(false);
    }
  }

  private static void setEnabled(View view, @NodeInfo.EnabledState int enabledState) {
    if (enabledState == NodeInfo.ENABLED_SET_TRUE) {
      view.setEnabled(true);
    } else if (enabledState == NodeInfo.ENABLED_SET_FALSE) {
      view.setEnabled(false);
    }
  }

  private static void unsetEnabled(View view, int flags) {
    view.setEnabled(isViewEnabled(flags));
  }

  private static void setSelected(View view, @NodeInfo.SelectedState int selectedState) {
    if (selectedState == NodeInfo.SELECTED_SET_TRUE) {
      view.setSelected(true);
    } else if (selectedState == NodeInfo.SELECTED_SET_FALSE) {
      view.setSelected(false);
    }
  }

  private static void unsetSelected(View view, int flags) {
    view.setSelected(isViewSelected(flags));
  }

  private static void setScale(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isScaleSet()) {
      final float scale = nodeInfo.getScale();
      view.setScaleX(scale);
      view.setScaleY(scale);
    }
  }

  private static void unsetScale(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isScaleSet()) {
      if (view.getScaleX() != 1) {
        view.setScaleX(1);
      }
      if (view.getScaleY() != 1) {
        view.setScaleY(1);
      }
    }
  }

  private static void setAlpha(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isAlphaSet()) {
      view.setAlpha(nodeInfo.getAlpha());
    }
  }

  private static void unsetAlpha(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isAlphaSet() && view.getAlpha() != 1) {
      view.setAlpha(1);
    }
  }

  private static void setRotation(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationSet()) {
      view.setRotation(nodeInfo.getRotation());
    }
  }

  private static void unsetRotation(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationSet() && view.getRotation() != 0) {
      view.setRotation(0);
    }
  }

  private static void setRotationX(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationXSet()) {
      view.setRotationX(nodeInfo.getRotationX());
    }
  }

  private static void unsetRotationX(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationXSet() && view.getRotationX() != 0) {
      view.setRotationX(0);
    }
  }

  private static void setRotationY(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationYSet()) {
      view.setRotationY(nodeInfo.getRotationY());
    }
  }

  private static void unsetRotationY(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationYSet() && view.getRotationY() != 0) {
      view.setRotationY(0);
    }
  }

  private static void setViewPadding(View view, ViewNodeInfo viewNodeInfo) {
    if (!viewNodeInfo.hasPadding()) {
      return;
    }

    view.setPadding(
        viewNodeInfo.getPaddingLeft(),
        viewNodeInfo.getPaddingTop(),
        viewNodeInfo.getPaddingRight(),
        viewNodeInfo.getPaddingBottom());
  }

  private static void unsetViewPadding(View view, LayoutOutput output, ViewNodeInfo viewNodeInfo) {
    if (!viewNodeInfo.hasPadding()) {
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
              "From component: " + output.getComponent().getSimpleName(),
              e,
              0,
              null);
    }
  }

  private static void setViewBackground(View view, ViewNodeInfo viewNodeInfo) {
    final Drawable background = viewNodeInfo.getBackground();
    if (background != null) {
      setBackgroundCompat(view, background);
    }
  }

  private static void unsetViewBackground(View view, ViewNodeInfo viewNodeInfo) {
    final Drawable background = viewNodeInfo.getBackground();
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

  private static void unsetViewForeground(View view, ViewNodeInfo viewNodeInfo) {
    final Drawable foreground = viewNodeInfo.getForeground();
    if (foreground != null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        throw new IllegalStateException(
            "MountState has a ViewNodeInfo with foreground however "
                + "the current Android version doesn't support foreground on Views");
      }

      view.setForeground(null);
    }
  }

  private static void setViewLayoutDirection(View view, ViewNodeInfo viewNodeInfo) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return;
    }

    final int viewLayoutDirection;
    switch (viewNodeInfo.getLayoutDirection()) {
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

  private static void setViewStateListAnimator(View view, ViewNodeInfo viewNodeInfo) {
    StateListAnimator stateListAnimator = viewNodeInfo.getStateListAnimator();
    final int stateListAnimatorRes = viewNodeInfo.getStateListAnimatorRes();
    if (stateListAnimator == null && stateListAnimatorRes == 0) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      throw new IllegalStateException(
          "MountState has a ViewNodeInfo with stateListAnimator, "
              + "however the current Android version doesn't support stateListAnimator on Views");
    }
    if (stateListAnimator == null) {
      stateListAnimator =
          AnimatorInflater.loadStateListAnimator(view.getContext(), stateListAnimatorRes);
    }
    view.setStateListAnimator(stateListAnimator);
  }

  private static void unsetViewStateListAnimator(View view, ViewNodeInfo viewNodeInfo) {
    if (viewNodeInfo.getStateListAnimator() == null
        && viewNodeInfo.getStateListAnimatorRes() == 0) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      throw new IllegalStateException(
          "MountState has a ViewNodeInfo with stateListAnimator, "
              + "however the current Android version doesn't support stateListAnimator on Views");
    }
    view.setStateListAnimator(null);
  }

  private static void setViewLayerType(final View view, final ViewNodeInfo info) {
    final int type = info.getLayerType();
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      view.setLayerType(info.getLayerType(), info.getLayoutPaint());
    }
  }

  private static void unsetViewLayerType(final View view, final int mountFlags) {
    int type = LithoMountData.getOriginalLayerType(mountFlags);
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      view.setLayerType(type, null);
    }
  }

  static boolean shouldUpdateViewInfo(
      final LayoutOutput nextLayoutOutput, final LayoutOutput currentLayoutOutput) {

    final ViewNodeInfo nextViewNodeInfo = nextLayoutOutput.getViewNodeInfo();
    final ViewNodeInfo currentViewNodeInfo = currentLayoutOutput.getViewNodeInfo();
    if ((currentViewNodeInfo == null && nextViewNodeInfo != null)
        || (currentViewNodeInfo != null && !currentViewNodeInfo.isEquivalentTo(nextViewNodeInfo))) {

      return true;
    }

    final NodeInfo nextNodeInfo = nextLayoutOutput.getNodeInfo();
    final NodeInfo currentNodeInfo = currentLayoutOutput.getNodeInfo();
    return (currentNodeInfo == null && nextNodeInfo != null)
        || (currentNodeInfo != null && !currentNodeInfo.isEquivalentTo(nextNodeInfo));
  }
}
