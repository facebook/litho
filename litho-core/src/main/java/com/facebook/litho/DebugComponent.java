/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A DebugComponent represents a node in Litho's component hierarchy. DebugComponent removes the
 * need to worry about implementation details of whether a node is represented by a {@link
 * Component} or a {@link ActualComponentLayout}. The purpose of this class is for tools such as
 * Stetho's UI inspector to be able to easily visualize a component hierarchy without worrying about
 * implementation details of Litho.
 */
public final class DebugComponent {

  public interface Overrider {
    void applyOverrides(DebugComponent node);
  }

  private static final Map<String, Overrider> sOverriders = new HashMap<>();

  private String mGlobalKey;
  private InternalNode mNode;
  private int mComponentIndex;
  private Overrider mOverrider;

  private DebugComponent() {}

  static synchronized DebugComponent getInstance(InternalNode node, int componentIndex) {
    final DebugComponent debugComponent = new DebugComponent();
    debugComponent.mGlobalKey = createKey(node, componentIndex);
    debugComponent.mNode = node;
    debugComponent.mComponentIndex = componentIndex;
    debugComponent.mOverrider = sOverriders.get(debugComponent.mGlobalKey);
    node.registerDebugComponent(debugComponent);

    return debugComponent;
  }

  /**
   * @return The root {@link DebugComponent} of a LithoView. This should be the start of your
   * traversal.
   */
  @Nullable
  public static DebugComponent getRootInstance(LithoView view) {
    return getRootInstance(view.getComponentTree());
  }

  @Nullable
  public static DebugComponent getRootInstance(@Nullable ComponentTree componentTree) {
    final LayoutState layoutState = componentTree == null ?
        null :
        componentTree.getMainThreadLayoutState();
    final InternalNode root = layoutState == null ? null : layoutState.getLayoutRoot();
    if (root != null) {
      final int outerWrapperComponentIndex = Math.max(0, root.getComponents().size() - 1);
      return DebugComponent.getInstance(root, outerWrapperComponentIndex);
    }
    return null;
  }

  private static String createKey(InternalNode node, int componentIndex) {
    final ComponentContext context = node.getContext();
    final ComponentTree tree = context.getComponentTree();
    final String componentKey = node.getComponents().get(componentIndex).getGlobalKey();
    return System.identityHashCode(tree) + componentKey;
  }

  public void setOverrider(Overrider overrider) {
    mOverrider = overrider;
    sOverriders.put(mGlobalKey, overrider);
  }

  /**
   * Get the list of components composed by this component. This will not include any {@link View}s
   * that are mounted by this component as those are not components.
   * Use {@link this#getMountedView} for that.
   *
   * @return A list of child components.
   */
  public List<DebugComponent> getChildComponents() {
    if (!isLayoutNode()) {
      final int nextComponentIndex = mComponentIndex - 1;
      return Arrays.asList(getInstance(mNode, nextComponentIndex));
    }

    final ArrayList<DebugComponent> children = new ArrayList<>();

    for (int i = 0, count = mNode.getChildCount(); i < count; i++) {
      final InternalNode childNode = mNode.getChildAt(i);
      final int outerWrapperComponentIndex = Math.max(0, childNode.getComponents().size() - 1);
      children.add(getInstance(childNode, outerWrapperComponentIndex));
    }

    final InternalNode nestedTree = mNode.getNestedTree();
    if (nestedTree != null) {
      for (int i = 0, count = nestedTree.getChildCount(); i < count; i++) {
        final InternalNode childNode = nestedTree.getChildAt(i);
        children.add(getInstance(childNode, Math.max(0, childNode.getComponents().size() - 1)));
      }
    }

    return children;
  }

  /**
   * @return A mounted view or null if this component does not mount a view.
   */
  @Nullable
  public View getMountedView() {
    final Component component = mNode.getRootComponent();
    if (component != null && Component.isMountViewSpec(component)) {
      return (View) getMountedContent();
    }

    return null;
  }

  /**
   * @return A mounted drawable or null if this component does not mount a drawable.
   */
  @Nullable
  public Drawable getMountedDrawable() {
    final Component component = mNode.getRootComponent();
    if (component != null && Component.isMountDrawableSpec(component)) {
      return (Drawable) getMountedContent();
    }

    return null;
  }

  /**
   * @return The litho view hosting this component.
   */
  @Nullable
  public LithoView getLithoView() {
    final ComponentContext c = mNode.getContext();
    final ComponentTree tree = c == null ? null : c.getComponentTree();
    return tree == null ? null : tree.getLithoView();
  }

  /**
   * @return The bounds of this component relative to its hosting {@link LithoView}.
   */
  public Rect getBoundsInLithoView() {
    if (isRoot()) {
      return new Rect(0, 0, mNode.getWidth(), mNode.getHeight());
    }

    final int x = getXFromRoot(mNode);
    final int y = getYFromRoot(mNode);
    return new Rect(x, y, x + mNode.getWidth(), y + mNode.getHeight());
  }

  /**
   * @return The bounds of this component relative to its parent.
   */
  public Rect getBounds() {
    final int x = mNode.getX();
    final int y = mNode.getY();
    return new Rect(x, y, x + mNode.getWidth(), y + mNode.getHeight());
  }

  /**
   * @return the {@link ComponentContext} for this component.
   */
  public ComponentContext getContext() {
    return mNode.getContext();
  }

  /**
   * @return True if this not has layout information attached to it (backed by a Yoga node)
   */
  public boolean isLayoutNode() {
    return mComponentIndex == 0;
  }

  /**
   * @return This component's testKey or null if none is set.
   */
  @Nullable
  public String getTestKey() {
    return isLayoutNode() ? mNode.getTestKey() : null;
  }

  /**
   * @return A concatenated string of all text content within the underlying LithoView.
   *         Null if the node doesn't have an associated LithoView.
   */
  @Nullable
  public String getTextContent() {
    final LithoView lithoView = getLithoView();
    final Component component = getComponent();

    if (lithoView == null || component == null) {
      return null;
    }

    final MountState mountState = lithoView.getMountState();
    final StringBuilder sb = new StringBuilder();

    for (int i = 0, size = mountState.getItemCount(); i < size; i++) {
      final MountItem mountItem = mountState.getItemAt(i);
      final Component mountItemComponent = mountItem == null ? null : mountItem.getComponent();
      if (mountItemComponent != null && mountItemComponent.isEquivalentTo(component)) {
        final Object content = mountItem.getContent();

        if (content instanceof TextContent) {
          for (CharSequence charSequence : ((TextContent) content).getTextItems()) {
            sb.append(charSequence);
          }
        }
      }
    }

    return sb.toString();
  }

  /**
   * @return The {@link ComponentHost} that wraps this component or null if one cannot be found.
   */
  @Nullable
  public ComponentHost getComponentHost() {
    final LithoView lithoView = getLithoView();
    final Component component = getComponent();

    if (lithoView == null || component == null) {
      return null;
    }

    for (int i = 0, size = lithoView.getMountState().getItemCount(); i < size; i++) {
      final MountItem mountItem = lithoView.getMountState().getItemAt(i);
      final Component mountItemComponent = mountItem == null ? null : mountItem.getComponent();
      if (mountItemComponent != null && mountItemComponent.isEquivalentTo(component)) {
        return mountItem.getHost();
      }
    }

    return null;
  }

  /**
   * @return This component's key or null if none is set.
   */
  @Nullable
  public String getKey() {
    return mNode.getComponents().get(mComponentIndex).getKey();
  }

  /**
   * @return The Component instance this debug component wraps.
   */
  public Component getComponent() {
    return mNode.getComponents().get(mComponentIndex);
  }

  /**
   * @return The Yoga node asscociated with this debug component. May be null.
   */
  @Nullable
  public YogaNode getYogaNode() {
    if (!isLayoutNode()) {
      return null;
    }

    return mNode.mYogaNode;
  }

  /**
   * @return The foreground drawable asscociated with this debug component. May be null.
   */
  @Nullable
  public Drawable getForeground() {
    if (!isLayoutNode()) {
      return null;
    }

    return mNode.getForeground();
  }

  /**
   * @return The background drawable asscociated with this debug component. May be null.
   */
  @Nullable
  public Reference<? extends Drawable> getBackground() {
    if (!isLayoutNode()) {
      return null;
    }

    return mNode.getBackground();
  }

  /**
   * @return The int value of the importantForAccessibility property on this debug component.
   */
  @Nullable
  public Integer getImportantForAccessibility() {
    return mNode.getImportantForAccessibility();
  }

  /**
   * @return The boolean value of the focusable property on this debug component.
   */
  public boolean getFocusable() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getFocusState() == NodeInfo.FOCUS_SET_TRUE;
    }
    return false;
  }

  /**
   * @return The content description CharSequence on this debug component.  May be null.
   */
  @Nullable
  public CharSequence getContentDescription() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getContentDescription();
    }
    return null;
  }

  /** @return Whether this component is the root of its hierarchy */
  public boolean isRoot() {
    return mNode.getParent() == null;
  }

  public void rerender() {
    final LithoView lithoView = getLithoView();
    if (lithoView != null) {
      lithoView.forceRelayout();
    }
  }

  public void setBackgroundColor(int color) {
    mNode.backgroundColor(color);
  }

  public void setForegroundColor(int color) {
    mNode.foregroundColor(color);
  }

  public void setLayoutDirection(YogaDirection yogaDirection) {
    mNode.layoutDirection(yogaDirection);
  }

  public void setFlexDirection(YogaFlexDirection direction) {
    mNode.flexDirection(direction);
  }

  public void setJustifyContent(YogaJustify yogaJustify) {
    mNode.justifyContent(yogaJustify);
  }

  public void setAlignItems(YogaAlign yogaAlign) {
    mNode.alignItems(yogaAlign);
  }

  public void setAlignSelf(YogaAlign yogaAlign) {
    mNode.alignSelf(yogaAlign);
  }

  public void setAlignContent(YogaAlign yogaAlign) {
    mNode.alignContent(yogaAlign);
  }

  public void setPositionType(YogaPositionType yogaPositionType) {
    mNode.positionType(yogaPositionType);
  }

  public void setFlexGrow(float value) {
    mNode.flexGrow(value);
  }

  public void setFlexShrink(float value) {
    mNode.flexShrink(value);
  }

  public void setFlexBasis(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.flexBasisAuto();
        break;
      case PERCENT:
        mNode.flexBasisPercent(value.value);
        break;
      case POINT:
        mNode.flexBasisPx((int) value.value);
        break;
    }
  }

  public void setWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.widthAuto();
        break;
      case PERCENT:
        mNode.widthPercent(value.value);
        break;
      case POINT:
        mNode.widthPx((int) value.value);
        break;
    }
  }

  public void setMinWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.minWidthPx(Integer.MIN_VALUE);
        break;
      case PERCENT:
        mNode.minWidthPercent(value.value);
        break;
      case POINT:
        mNode.minWidthPx((int) value.value);
        break;
    }
  }

  public void setMaxWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.maxWidthPx(Integer.MAX_VALUE);
        break;
      case PERCENT:
        mNode.maxWidthPercent(value.value);
        break;
      case POINT:
        mNode.maxWidthPx((int) value.value);
        break;
    }
  }

  public void setHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.heightAuto();
        break;
      case PERCENT:
        mNode.heightPercent(value.value);
        break;
      case POINT:
        mNode.heightPx((int) value.value);
        break;
    }
  }

  public void setMinHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.minHeightPx(Integer.MIN_VALUE);
        break;
      case PERCENT:
        mNode.minHeightPercent(value.value);
        break;
      case POINT:
        mNode.minHeightPx((int) value.value);
        break;
    }
  }

  public void setMaxHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.maxHeightPx(Integer.MAX_VALUE);
        break;
      case PERCENT:
        mNode.maxHeightPercent(value.value);
        break;
      case POINT:
        mNode.maxHeightPx((int) value.value);
        break;
    }
  }

  public void setAspectRatio(float aspectRatio) {
    mNode.aspectRatio(aspectRatio);
  }

  public void setMargin(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
        mNode.marginPx(edge, 0);
        break;
      case AUTO:
        mNode.marginAuto(edge);
        break;
      case PERCENT:
        mNode.marginPercent(edge, value.value);
        break;
      case POINT:
        mNode.marginPx(edge, (int) value.value);
        break;
    }
  }

  public void setPadding(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.paddingPx(edge, 0);
        break;
      case PERCENT:
        mNode.paddingPercent(edge, value.value);
        break;
      case POINT:
        mNode.paddingPx(edge, (int) value.value);
        break;
    }
  }

  public void setPosition(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.positionPercent(edge, YogaConstants.UNDEFINED);
        break;
      case PERCENT:
        mNode.positionPercent(edge, value.value);
        break;
      case POINT:
        mNode.positionPx(edge, (int) value.value);
        break;
    }
  }

  public void setBorderWidth(YogaEdge edge, float value) {
    mNode.setBorderWidth(edge, (int) value);
  }

  public void setContentDescription(CharSequence contentDescription) {
    mNode.contentDescription(contentDescription);
  }

  public void setImportantForAccessibility(int importantForAccessibility) {
    mNode.importantForAccessibility(importantForAccessibility);
  }

  public void setFocusable(boolean focusable) {
    mNode.focusable(focusable);
  }

  @Nullable
  public ComponentLifecycle.StateContainer getStateContainer() {
    return getComponent().getStateContainer();
  }

  void applyOverrides() {
    if (mOverrider != null) {
      mOverrider.applyOverrides(this);
    }
  }

  private static InternalNode parent(InternalNode node) {
    final InternalNode parent = node.getParent();
    return parent != null ? parent : node.getNestedTreeHolder();
  }

  private static int getXFromRoot(InternalNode node) {
    if (node == null) {
      return 0;
    }
    return node.getX() + getXFromRoot(parent(node));
  }

  private static int getYFromRoot(InternalNode node) {
    if (node == null) {
      return 0;
    }
    return node.getY() + getYFromRoot(parent(node));
  }

  public String getGlobalKey() {
    return mGlobalKey;
  }

  @Nullable
  public EventHandler getClickHandler() {
    if (!isLayoutNode()) {
      return null;
    }

    return mNode.getClickHandler();
  }

  @Nullable
  private Object getMountedContent() {
    if (!isLayoutNode()) {
      return null;
    }

    final ComponentContext context = mNode.getContext();
    final ComponentTree tree = context == null ? null : context.getComponentTree();
    final LithoView view = tree == null ? null : tree.getLithoView();
    final MountState mountState = view == null ? null : view.getMountState();

    if (mountState != null) {
      for (int i = 0, count = mountState.getItemCount(); i < count; i++) {
        final MountItem mountItem = mountState.getItemAt(i);
        final Component component = mountItem == null ? null : mountItem.getComponent();

        if (component != null && component == mNode.getRootComponent()) {
          return mountItem.getContent();
        }
      }
    }

    return null;
  }
}
