/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
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
  private final static YogaEdge[] edges = YogaEdge.values();
  private final static SimpleArrayMap<String, DebugComponent> mDebugNodes = new SimpleArrayMap<>();

  private String mKey;
  private WeakReference<InternalNode> mNode;
  private int mComponentIndex;
  private final SimpleArrayMap<String, SimpleArrayMap<String, Object>> mStyleOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, SimpleArrayMap<String, Object>> mPropOverrides =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, SimpleArrayMap<String, Object>> mStateOverrides =
      new SimpleArrayMap<>();

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
    final InternalNode node = mNode.get();
    if (node == null) {
      return null;
    }

    if (node.getComponents().isEmpty()) {
      switch (node.mYogaNode.getFlexDirection()) {
        case COLUMN: return Column.class.getName();
        case COLUMN_REVERSE: return ColumnReverse.class.getName();
        case ROW: return Row.class.getName();
        case ROW_REVERSE: return RowReverse.class.getName();
      }
    }

    return node
        .getComponents()
        .get(mComponentIndex)
        .getLifecycle()
        .getClass()
        .getName();
  }

  /**
   * Get the list of components composed by this component. This will not include any {@link View}s
   * that are mounted by this component as those are not components.
   * Use {@link this#getMountedViews} for that.
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
   * @return A list of mounted views.
   */
  public List<View> getMountedViews() {
    if (mComponentIndex > 0) {
      return Collections.EMPTY_LIST;
    }

    final InternalNode node = mNode.get();
    final ComponentContext context = node == null ? null : node.getContext();
    final ComponentTree tree = context == null ? null : context.getComponentTree();
    final LithoView view = tree == null ? null : tree.getLithoView();
    final MountState mountState = view == null ? null : view.getMountState();
    final ArrayList<View> children = new ArrayList<>();

    if (mountState != null) {
      for (int i = 0, count = mountState.getItemCount(); i < count; i++) {
        final MountItem mountItem = mountState.getItemAt(i);
        final Component component = mountItem == null ? null : mountItem.getComponent();

        if (component != null &&
            component == node.getRootComponent() &&
            Component.isMountViewSpec(component)) {
          children.add((View) mountItem.getContent());
        }
      }
    }

    return children;
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
   * @return Key-value mapping of this components layout styles.
   */
  public Map<String, Object> getStyles() {
    final InternalNode node = mNode.get();
    if (node == null || !isLayoutNode()) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, Object> styles = new ArrayMap<>();
    final YogaNode yogaNode = node.mYogaNode;
    final YogaNode defaults = ComponentsPools.acquireYogaNode(node.getContext());
    final ComponentContext context = node.getContext();

    styles.put("background", getReferenceColor(context, node.getBackground()));
    styles.put("foreground", getDrawableColor(node.getForeground()));

    styles.put("direction", yogaNode.getStyleDirection());
    styles.put("flex-direction", yogaNode.getFlexDirection());
    styles.put("justify-content", yogaNode.getJustifyContent());
    styles.put("align-items", yogaNode.getAlignItems());
    styles.put("align-self", yogaNode.getAlignSelf());
    styles.put("align-content", yogaNode.getAlignContent());
    styles.put("position", yogaNode.getPositionType());
    styles.put("flex-grow", yogaNode.getFlexGrow());
    styles.put("flex-shrink", yogaNode.getFlexShrink());
    styles.put("flex-basis", yogaNode.getFlexBasis());

    styles.put("width", yogaNode.getWidth());
    styles.put("min-width", yogaNode.getMinWidth());
    styles.put("max-width", yogaNode.getMaxWidth());
    styles.put("height", yogaNode.getHeight());
    styles.put("min-height", yogaNode.getMinHeight());
    styles.put("max-height", yogaNode.getMaxHeight());

    for (YogaEdge edge : edges) {
      final String key = "margin-" + edge.toString().toLowerCase();
      styles.put(key, yogaNode.getMargin(edge));
    }

    for (YogaEdge edge : edges) {
      final String key = "padding-" + edge.toString().toLowerCase();
      styles.put(key, yogaNode.getPadding(edge));
    }

    for (YogaEdge edge : edges) {
      final String key = "position-" + edge.toString().toLowerCase();
      styles.put(key, yogaNode.getPosition(edge));
    }

    for (YogaEdge edge : edges) {
      final String key = "border-" + edge.toString().toLowerCase();
      final float border = yogaNode.getBorder(edge);
      styles.put(key, Float.isNaN(border) ? 0 : border);
    }

    ComponentsPools.release(defaults);
    return styles;
  }

  private Object getDrawableColor(Drawable drawable) {
    if (drawable instanceof ColorDrawable) {
      return ((ColorDrawable) drawable).getColor();
    }
    return 0;
  }

  private Object getReferenceColor(ComponentContext c, Reference<? extends Drawable> reference) {
    if (reference != null) {
      getDrawableColor(Reference.acquire(c, reference));
    }
    return 0;
  }

  /**
   * @return Key-value mapping of this components props.
   */
  public Map<String, Pair<Prop, Object>> getProps() {
    final InternalNode node = mNode.get();
    final Component component = node == null || node.getComponents().isEmpty()
        ? null
        : node.getComponents().get(mComponentIndex);
    if (component == null) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, Pair<Prop, Object>> props = new ArrayMap<>();
    final ComponentLifecycle.StateContainer stateContainer = component.getStateContainer();

    for (Field field : component.getClass().getDeclaredFields()) {
      try {
        field.setAccessible(true);
        final Prop propAnnotation = field.getAnnotation(Prop.class);
        if (propAnnotation != null) {
          final Object value = field.get(component);
          if (value != stateContainer && !(value instanceof ComponentLifecycle)) {
            props.put(field.getName(), new Pair<>(propAnnotation, value));
          }
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return props;
  }

  /**
   * @return Key-value mapping of this components state.
   */
  public Map<String, Object> getState() {
    final InternalNode node = mNode.get();
    final Component component = node == null || node.getComponents().isEmpty()
        ? null
        : node.getComponents().get(mComponentIndex);
    if (component == null) {
      return Collections.EMPTY_MAP;
    }

    final ComponentLifecycle.StateContainer stateContainer = component.getStateContainer();
    if (stateContainer == null) {
      return Collections.EMPTY_MAP;
    }

    final Map<String, Object> state = new ArrayMap<>();

    for (Field field : stateContainer.getClass().getDeclaredFields()) {
      try {
        field.setAccessible(true);
        if (field.getAnnotation(State.class) != null) {
          final Object value = field.get(stateContainer);
          if (!(value instanceof ComponentLifecycle)) {
            state.put(field.getName(), value);
          }
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return state;
  }

  /**
   * @return Registed an override for a style key with a certain value. This override will be used
   * The next time this component is rendered.
   */
  public synchronized void setStyleOverride(String key, Object value) {
    SimpleArrayMap<String, Object> styles = mStyleOverrides.get(mKey);
    if (styles == null) {
      styles = new SimpleArrayMap<>();
      mStyleOverrides.put(mKey, styles);
    }

    styles.put(key, value);
    getLithoView().forceRelayout();
  }

  /**
   * @return Registed an override for a prop key with a certain value. This override will be used
   * The next time this component is rendered.
   */
  public synchronized void setPropOverride(String key, Object value) {
    SimpleArrayMap<String, Object> props = mPropOverrides.get(mKey);
    if (props == null) {
      props = new SimpleArrayMap<>();
      mPropOverrides.put(mKey, props);
    }

    props.put(key, value);
    getLithoView().forceRelayout();
  }

  /**
   * @return Registed an override for a state key with a certain value. This override will be used
   * The next time this component is rendered.
   */
  public synchronized void setStateOverride(String key, Object value) {
    SimpleArrayMap<String, Object> props = mStateOverrides.get(mKey);
    if (props == null) {
      props = new SimpleArrayMap<>();
      mStateOverrides.put(mKey, props);
    }

    props.put(key, value);
    getLithoView().forceRelayout();
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

  void applyOverrides() {
    final InternalNode node = mNode.get();
    if (node == null) {
      return;
    }

    if (mStyleOverrides.containsKey(mKey)) {
      final SimpleArrayMap<String, Object> styles = mStyleOverrides.get(mKey);
      for (int i = 0, size = styles.size(); i < size; i++) {
        final String key = styles.keyAt(i);
        final Object value = styles.get(key);

        try {
          if (key.equals("background")) {
            node.backgroundColor((Integer) value);
          }

          if (key.equals("foreground")) {
            node.foregroundColor((Integer) value);
          }

          if (key.equals("direction")) {
            node.layoutDirection(YogaDirection.valueOf(((String) value).toUpperCase()));
          }

          if (key.equals("flex-direction")) {
            node.flexDirection(YogaFlexDirection.valueOf(((String) value).toUpperCase()));
          }

          if (key.equals("justify-content")) {
            node.justifyContent(YogaJustify.valueOf(((String) value).toUpperCase()));
          }

          if (key.equals("align-items")) {
            node.alignItems(YogaAlign.valueOf(((String) value).toUpperCase()));
          }

          if (key.equals("align-self")) {
            node.alignSelf(YogaAlign.valueOf(((String) value).toUpperCase()));
          }

          if (key.equals("align-content")) {
            node.alignContent(YogaAlign.valueOf(((String) value).toUpperCase()));
          }

          if (key.equals("position")) {
            node.positionType(YogaPositionType.valueOf(((String) value).toUpperCase()));
          }

          if (key.equals("flex-grow")) {
            node.flexGrow((Float) value);
          }

          if (key.equals("flex-shrink")) {
            node.flexShrink((Float) value);
          }
        } catch (IllegalArgumentException ignored) {
          // ignore errors when the user suplied an invalid enum value
        }

        if (key.equals("flex-basis")) {
          final YogaValue flexBasis = YogaValue.parse(((String) value).toLowerCase());
          if (flexBasis == null) {
            continue;
          }
          switch (flexBasis.unit) {
            case AUTO:
              node.flexBasisAuto();
              break;
            case UNDEFINED:
            case POINT:
              node.flexBasisPx(FastMath.round(flexBasis.value));
              break;
            case PERCENT:
              node.flexBasisPercent(FastMath.round(flexBasis.value));
              break;
          }
        }

        if (key.equals("width")) {
          final YogaValue width = YogaValue.parse(((String) value).toLowerCase());
          if (width == null) {
            continue;
          }
          switch (width.unit) {
            case AUTO:
              node.widthAuto();
              break;
            case UNDEFINED:
            case POINT:
              node.widthPx(FastMath.round(width.value));
              break;
            case PERCENT:
              node.widthPercent(FastMath.round(width.value));
              break;
          }
        }

        if (key.equals("min-width")) {
          final YogaValue minWidth = YogaValue.parse(((String) value).toLowerCase());
          if (minWidth == null) {
            continue;
          }
          switch (minWidth.unit) {
            case UNDEFINED:
            case POINT:
              node.minWidthPx(FastMath.round(minWidth.value));
              break;
            case PERCENT:
              node.minWidthPercent(FastMath.round(minWidth.value));
              break;
          }
        }

        if (key.equals("max-width")) {
          final YogaValue maxWidth = YogaValue.parse(((String) value).toLowerCase());
          if (maxWidth == null) {
            continue;
          }
          switch (maxWidth.unit) {
            case UNDEFINED:
            case POINT:
              node.maxWidthPx(FastMath.round(maxWidth.value));
              break;
            case PERCENT:
              node.maxWidthPercent(FastMath.round(maxWidth.value));
              break;
          }
        }

        if (key.equals("height")) {
          final YogaValue height = YogaValue.parse(((String) value).toLowerCase());
          if (height == null) {
            continue;
          }
          switch (height.unit) {
            case AUTO:
              node.heightAuto();
              break;
            case UNDEFINED:
            case POINT:
              node.heightPx(FastMath.round(height.value));
              break;
            case PERCENT:
              node.heightPercent(FastMath.round(height.value));
              break;
          }
        }

        if (key.equals("min-height")) {
          final YogaValue minHeight = YogaValue.parse(((String) value).toLowerCase());
          if (minHeight == null) {
            continue;
          }
          switch (minHeight.unit) {
            case UNDEFINED:
            case POINT:
              node.minHeightPx(FastMath.round(minHeight.value));
              break;
            case PERCENT:
              node.minHeightPercent(FastMath.round(minHeight.value));
              break;
          }
        }

        if (key.equals("max-height")) {
          final YogaValue maxHeight = YogaValue.parse(((String) value).toLowerCase());
          if (maxHeight == null) {
            continue;
          }
          switch (maxHeight.unit) {
            case UNDEFINED:
            case POINT:
              node.maxHeightPx(FastMath.round(maxHeight.value));
              break;
            case PERCENT:
              node.maxHeightPercent(FastMath.round(maxHeight.value));
              break;
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("margin-" + edge.toString().toLowerCase())) {
            final YogaValue margin = YogaValue.parse(((String) value).toLowerCase());
            if (margin == null) {
              continue;
            }
            switch (margin.unit) {
              case UNDEFINED:
              case POINT:
                node.marginPx(edge, FastMath.round(margin.value));
                break;
              case AUTO:
                node.marginAuto(edge);
                break;
              case PERCENT:
                node.marginPercent(edge, FastMath.round(margin.value));
                break;
            }
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("padding-" + edge.toString().toLowerCase())) {
            final YogaValue padding = YogaValue.parse(((String) value).toLowerCase());
            if (padding == null) {
              continue;
            }
            switch (padding.unit) {
              case UNDEFINED:
              case POINT:
                node.paddingPx(edge, FastMath.round(padding.value));
                break;
              case PERCENT:
                node.paddingPercent(edge, FastMath.round(padding.value));
                break;
            }
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("position-" + edge.toString().toLowerCase())) {
            final YogaValue position = YogaValue.parse(((String) value).toLowerCase());
            if (position == null) {
              continue;
            }
            switch (position.unit) {
              case UNDEFINED:
              case POINT:
                node.positionPx(edge, FastMath.round(position.value));
                break;
              case PERCENT:
                node.positionPercent(edge, FastMath.round(position.value));
                break;
            }
          }
        }

        for (YogaEdge edge : edges) {
          if (key.equals("border-" + edge.toString().toLowerCase())) {
            node.borderWidthPx(edge, FastMath.round((Float) value));
          }
        }
      }
    }

    if (mPropOverrides.containsKey(mKey)) {
      final Component component = node.getRootComponent();
      if (component != null) {
        final SimpleArrayMap<String, Object> props = mPropOverrides.get(mKey);
        for (int i = 0, size = props.size(); i < size; i++) {
          final String key = props.keyAt(i);
          applyReflectiveOverride(component, key, props.get(key));
        }
      }
    }

    if (mStateOverrides.containsKey(mKey)) {
      final Component component = node.getRootComponent();
      final ComponentLifecycle.StateContainer stateContainer =
          component == null ? null : component.getStateContainer();
      if (stateContainer != null) {
        final SimpleArrayMap<String, Object> state = mStateOverrides.get(mKey);
        for (int i = 0, size = state.size(); i < size; i++) {
          final String key = state.keyAt(i);
          applyReflectiveOverride(stateContainer, key, state.get(key));
        }
      }
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

  private void applyReflectiveOverride(Object o, String key, Object value) {
    try {
      final Field field = o.getClass().getDeclaredField(key);
      field.setAccessible(true);
      field.set(o, value);
    } catch (Exception e) {
      e.printStackTrace();
    }
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
}
