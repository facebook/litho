/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.util.SimpleArrayMap;
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

/**
 * A DebugComponent represents a node in Litho's component hierarchy. DebugComponent removes the
 * need to worry about implementation details of whether a node is represented by a
 * {@link Component} or a {@link ComponentLayout}. The purpose of this class is for tools such as
 * Stetho's UI inspector to be able to easily visualize a component hierarchy without worrying about
 * implementation details of Litho.
 */
public final class DebugComponent {

  public interface Overrider {
    void applyOverrides(DebugComponent node);
  }

  private static final SimpleArrayMap<String, DebugComponent> mDebugNodes = new SimpleArrayMap<>();

  private String mKey;
  private WeakReference<InternalNode> mNode;
  private int mComponentIndex;
  private Overrider mOverrider;

  private DebugComponent() {}

  static synchronized DebugComponent getInstance(InternalNode node, int componentIndex) {
    final String globalKey = createKey(node, componentIndex);
    DebugComponent debugComponent = mDebugNodes.get(globalKey);

    if (debugComponent == null) {
      debugComponent = new DebugComponent();
      mDebugNodes.put(globalKey, debugComponent);
    }

    debugComponent.mKey = globalKey;
    debugComponent.mNode = new WeakReference<>(node);
    debugComponent.mComponentIndex = componentIndex;

    return debugComponent;
  }

  /**
   * @return The root {@link DebugComponent} of a LithoView. This should be the start of your
   * traversal.
   */
  public static DebugComponent getRootInstance(LithoView view) {
    return getRootInstance(view.getComponentTree());
  }

  public static DebugComponent getRootInstance(ComponentTree componentTree) {
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

  /**
   * @return A conanical name for this component. Suitable to present to the user.
   */
  public String getName() {
    return getComponentClass().getName();
  }


  /**
   * @return A simpler conanical name for this component. Suitable to present to the user.
   */
  public String getSimpleName() {
    return getComponentClass().getSimpleName();
  }

  private Class getComponentClass() {
    final InternalNode node = mNode.get();

    if (node.getComponents().isEmpty()) {
      switch (node.mYogaNode.getFlexDirection()) {
        case COLUMN: return Column.class;
        case COLUMN_REVERSE: return ColumnReverse.class;
        case ROW: return Row.class;
        case ROW_REVERSE: return RowReverse.class;
      }
    }

    return node
        .getComponents()
        .get(mComponentIndex)
        .getLifecycle()
        .getClass();
  }

  public void setOverrider(Overrider overrider) {
    mOverrider = overrider;
  }

  /**
   * Get the list of components composed by this component. This will not include any {@link View}s
   * that are mounted by this component as those are not components.
   * Use {@link this#getMountedView} for that.
   *
   * @return A list of child components.
   */
  public List<DebugComponent> getChildComponents() {
    final InternalNode node = mNode.get();
    if (node == null) {
      return Collections.EMPTY_LIST;
    }

    if (mComponentIndex > 0) {
      final int wrappedComponentIndex = mComponentIndex - 1;
      return Arrays.asList(getInstance(node, wrappedComponentIndex));
    }

    final ArrayList<DebugComponent> children = new ArrayList<>();

    for (int i = 0, count = node.getChildCount(); i < count; i++) {
      final InternalNode childNode = node.getChildAt(i);
      final int outerWrapperComponentIndex = Math.max(0, childNode.getComponents().size() - 1);
      children.add(getInstance(childNode, outerWrapperComponentIndex));
    }

    if (node.hasNestedTree()) {
      final InternalNode nestedTree = node.getNestedTree();
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
  public View getMountedView() {
    final InternalNode node = mNode.get();
    final Component component = node == null ? null : node.getRootComponent();
    if (component != null && Component.isMountViewSpec(component)) {
      return (View) getMountedContent();
    }

    return null;
  }

  /**
   * @return A mounted drawable or null if this component does not mount a drawable.
   */
  public Drawable getMountedDrawable() {
    final InternalNode node = mNode.get();
    final Component component = node == null ? null : node.getRootComponent();
    if (component != null && Component.isMountDrawableSpec(component)) {
      return (Drawable) getMountedContent();
    }

    return null;
  }

  /**
   * @return The litho view hosting this component.
   */
  public LithoView getLithoView() {
    final InternalNode node = mNode.get();
    final ComponentContext c = node == null ? null : node.getContext();
    final ComponentTree tree = c == null ? null : c.getComponentTree();
    return tree == null ? null : tree.getLithoView();
  }

  /**
   * @return The bounds of this component relative to its hosting {@link LithoView}.
   */
  public Rect getBoundsInLithoView() {
    final InternalNode node = mNode.get();
    if (node == null) {
      return new Rect();
    }
    final int x = getXFromRoot(node);
    final int y = getYFromRoot(node);
    return new Rect(x, y, x + node.getWidth(), y + node.getHeight());
  }

  /**
   * @return The bounds of this component relative to its parent.
   */
  public Rect getBounds() {
    final InternalNode node = mNode.get();
    if (node == null) {
      return new Rect();
    }
    final int x = node.getX();
    final int y = node.getY();
    return new Rect(x, y, x + node.getWidth(), y + node.getHeight());
  }

  /**
   * @return the {@link ComponentContext} for this component.
   */
  public ComponentContext getContext() {
    return mNode.get().getContext();
  }

  /**
   * @return True if this not has layout information attached to it (backed by a Yoga node)
   */
  public boolean isLayoutNode() {
    return mNode.get().getComponents().isEmpty() || mComponentIndex == 0;
  }

  /**
   * @return This component's testKey or null if none is set.
   */
  public String getTestKey() {
    return isLayoutNode() ? mNode.get().getTestKey() : null;
  }

  /**
   * @return This component's key or null if none is set.
   */
  public String getKey() {
    final InternalNode node = mNode.get();
    if (node != null && !node.getComponents().isEmpty()) {
      final Component component = node.getComponents().get(mComponentIndex);
      return component == null ? null : component.getKey();
    }
    return null;
  }

  /**
   * @return The Component instance this debug component wraps.
   */
  public Component getComponent() {
    final InternalNode node = mNode.get();
    if (node == null || node.getComponents().isEmpty()) {
      return null;
    }
    return node.getComponents().get(mComponentIndex);
  }

  /**
   * @return The Yoga node asscociated with this debug component. May be null.
   */
  public YogaNode getYogaNode() {
    final InternalNode node = mNode.get();
    if (node == null || !isLayoutNode()) {
      return null;
    }

    return node.mYogaNode;
  }

  /**
   * @return The foreground drawable asscociated with this debug component. May be null.
   */
  public Drawable getForeground() {
    final InternalNode node = mNode.get();
    if (node == null || !isLayoutNode()) {
      return null;
    }

    return node.getForeground();
  }

  /**
   * @return The background drawable asscociated with this debug component. May be null.
   */
  public Reference<? extends Drawable> getBackground() {
    final InternalNode node = mNode.get();
    if (node == null || !isLayoutNode()) {
      return null;
    }

    return node.getBackground();
  }

  public void rerender() {
    getLithoView().forceRelayout();
  }

  public void setBackgroundColor(int color) {
    mNode.get().backgroundColor(color);
  }

  public void setForegroundColor(int color) {
    mNode.get().foregroundColor(color);
  }

  public void setLayoutDirection(YogaDirection yogaDirection) {
    mNode.get().layoutDirection(yogaDirection);
  }

  public void setFlexDirection(YogaFlexDirection direction) {
    mNode.get().flexDirection(direction);
  }

  public void setJustifyContent(YogaJustify yogaJustify) {
    mNode.get().justifyContent(yogaJustify);
  }

  public void setAlignItems(YogaAlign yogaAlign) {
    mNode.get().alignItems(yogaAlign);
  }

  public void setAlignSelf(YogaAlign yogaAlign) {
    mNode.get().alignSelf(yogaAlign);
  }

  public void setAlignContent(YogaAlign yogaAlign) {
    mNode.get().alignContent(yogaAlign);
  }

  public void setPositionType(YogaPositionType yogaPositionType) {
    mNode.get().positionType(yogaPositionType);
  }

  public void setFlexGrow(float value) {
    mNode.get().flexGrow(value);
  }

  public void setFlexShrink(float value) {
    mNode.get().flexShrink(value);
  }

  public void setFlexBasis(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().flexBasisAuto();
        break;
      case PERCENT:
        mNode.get().flexBasisPercent(value.value);
        break;
      case POINT:
        mNode.get().flexBasisPx((int) value.value);
        break;
    }
  }

  public void setWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().widthAuto();
        break;
      case PERCENT:
        mNode.get().widthPercent(value.value);
        break;
      case POINT:
        mNode.get().widthPx((int) value.value);
        break;
    }
  }

  public void setMinWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().minWidthPx(Integer.MIN_VALUE);
        break;
      case PERCENT:
        mNode.get().minWidthPercent(value.value);
        break;
      case POINT:
        mNode.get().minWidthPx((int) value.value);
        break;
    }
  }

  public void setMaxWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().maxWidthPx(Integer.MAX_VALUE);
        break;
      case PERCENT:
        mNode.get().maxWidthPercent(value.value);
        break;
      case POINT:
        mNode.get().maxWidthPx((int) value.value);
        break;
    }
  }

  public void setHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().heightAuto();
        break;
      case PERCENT:
        mNode.get().heightPercent(value.value);
        break;
      case POINT:
        mNode.get().heightPx((int) value.value);
        break;
    }
  }

  public void setMinHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().minHeightPx(Integer.MIN_VALUE);
        break;
      case PERCENT:
        mNode.get().minHeightPercent(value.value);
        break;
      case POINT:
        mNode.get().minHeightPx((int) value.value);
        break;
    }
  }

  public void setMaxHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().maxHeightPx(Integer.MAX_VALUE);
        break;
      case PERCENT:
        mNode.get().maxHeightPercent(value.value);
        break;
      case POINT:
        mNode.get().maxHeightPx((int) value.value);
        break;
    }
  }

  public void setAspectRatio(float aspectRatio) {
    mNode.get().aspectRatio(aspectRatio);
  }

  public void setMargin(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
        mNode.get().marginPx(edge, 0);
        break;
      case AUTO:
        mNode.get().marginAuto(edge);
        break;
      case PERCENT:
        mNode.get().marginPercent(edge, value.value);
        break;
      case POINT:
        mNode.get().marginPx(edge, (int) value.value);
        break;
    }
  }

  public void setPadding(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().paddingPx(edge, 0);
        break;
      case PERCENT:
        mNode.get().paddingPercent(edge, value.value);
        break;
      case POINT:
        mNode.get().paddingPx(edge, (int) value.value);
        break;
    }
  }

  public void setPosition(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.get().positionPercent(edge, YogaConstants.UNDEFINED);
        break;
      case PERCENT:
        mNode.get().positionPercent(edge, value.value);
        break;
      case POINT:
        mNode.get().positionPx(edge, (int) value.value);
        break;
    }
  }

  public void setBorderWidth(YogaEdge edge, float value) {
    mNode.get().borderWidthPx(edge, (int) value);
  }

  public ComponentLifecycle.StateContainer getStateContainer() {
    final Component component = getComponent();
    return component == null ? null : component.getStateContainer();
  }

  void applyOverrides() {
    final InternalNode node = mNode.get();
    if (node != null && mOverrider != null) {
      mOverrider.applyOverrides(this);
    }
  }

  private InternalNode parent(InternalNode node) {
    final InternalNode parent = node.getParent();
    return parent != null ? parent : node.getNestedTreeHolder();
  }

  private int getXFromRoot(InternalNode node) {
    if (node == null) {
      return 0;
    }
    return node.getX() + getXFromRoot(parent(node));
  }

  private int getYFromRoot(InternalNode node) {
    if (node == null) {
      return 0;
    }
    return node.getY() + getYFromRoot(parent(node));
  }

  private static String createKey(InternalNode node, int componentIndex) {
    final InternalNode parent = node.getParent();
    final InternalNode nestedTreeHolder = node.getNestedTreeHolder();

    String key;
    if (parent != null) {
      key = createKey(parent, 0) + "." + parent.getChildIndex(node);
    } else if (nestedTreeHolder != null) {
      key = createKey(nestedTreeHolder, 0) + ".nested";
    } else {
      final ComponentContext c = node.getContext();
      final ComponentTree tree = c.getComponentTree();
      key = Integer.toString(System.identityHashCode(tree));
    }

    return key + "(" + componentIndex + ")";
  }

  public String getId() {
    return mKey;
  }

  public boolean isClickable() {
    if (mComponentIndex > 0) {
      return false;
    }

    final InternalNode node = mNode.get();
    if (node == null) {
      return false;
    }

    return node.isClickable();
  }

  private Object getMountedContent() {
    if (mComponentIndex > 0) {
      return null;
    }

    final InternalNode node = mNode.get();
    final ComponentContext context = node == null ? null : node.getContext();
    final ComponentTree tree = context == null ? null : context.getComponentTree();
    final LithoView view = tree == null ? null : tree.getLithoView();
    final MountState mountState = view == null ? null : view.getMountState();

    if (mountState != null) {
      for (int i = 0, count = mountState.getItemCount(); i < count; i++) {
        final MountItem mountItem = mountState.getItemAt(i);
        final Component component = mountItem == null ? null : mountItem.getComponent();

        if (component != null &&
            component == node.getRootComponent()) {
          return mountItem.getContent();
        }
      }
    }

    return null;
  }
}
