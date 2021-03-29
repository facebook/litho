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

import static com.facebook.litho.LayoutOutput.getLayoutOutput;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.MountItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A DebugComponent represents a node in Litho's component hierarchy. DebugComponent removes the
 * need to worry about implementation details of whether a node is represented by a {@link
 * Component} or a {@link ComponentLayout}. The purpose of this class is for tools such as Stetho's
 * UI inspector to be able to easily visualize a component hierarchy without worrying about
 * implementation details of Litho.
 */
public final class DebugComponent {

  public interface Overrider {
    void applyComponentOverrides(String key, Component component);

    void applyStateOverrides(String key, StateContainer state);

    void applyLayoutOverrides(String key, DebugLayoutNode node);
  }

  private static final Map<String, Overrider> sOverriders = new HashMap<>();

  private String mGlobalKey;
  private LithoLayoutResult mResult;
  private InternalNode mNode;
  private int mComponentIndex;

  private DebugComponent() {}

  static synchronized @Nullable DebugComponent getInstance(
      LithoLayoutResult result, int componentIndex) {
    final DebugComponent debugComponent = new DebugComponent();
    final InternalNode node = result.getInternalNode();
    final ComponentContext context = node.getContext();

    if (componentIndex >= node.getComponents().size()) {
      return null;
    }

    final Component component = node.getComponents().get(componentIndex);
    final String componentKey =
        ComponentUtils.getGlobalKey(
            component,
            node.getComponentKeys() == null ? null : node.getComponentKeys().get(componentIndex));

    debugComponent.mGlobalKey = generateGlobalKey(context, componentKey);
    debugComponent.mResult = result;
    debugComponent.mNode = result.getInternalNode();
    debugComponent.mComponentIndex = componentIndex;
    node.registerDebugComponent(debugComponent);

    return debugComponent;
  }

  /**
   * @return The root {@link DebugComponent} of a LithoView. This should be the start of your
   *     traversal.
   */
  @Nullable
  public static DebugComponent getRootInstance(
      LayoutStateContext layoutStateContext, Component component, String globalKey) {
    return getRootInstance(
        component.getScopedContext(layoutStateContext, globalKey).getComponentTree());
  }

  @Nullable
  public static DebugComponent getRootInstance(LithoView view) {
    return getRootInstance(view.getComponentTree());
  }

  @Nullable
  public static DebugComponent getRootInstance(@Nullable ComponentTree componentTree) {
    final LayoutState layoutState =
        componentTree == null ? null : componentTree.getMainThreadLayoutState();
    final LithoLayoutResult root = layoutState == null ? null : layoutState.getLayoutRoot();
    if (root != null && root != ComponentContext.NULL_LAYOUT) {
      final InternalNode node = root.getInternalNode();
      final int outerWrapperComponentIndex = Math.max(0, node.getComponents().size() - 1);
      return DebugComponent.getInstance(root, outerWrapperComponentIndex);
    }
    return null;
  }

  @Nullable
  public static DebugComponent getRootInstance(LithoLayoutResult rootResult) {
    final InternalNode rootInternalNode = rootResult.getInternalNode();
    final int outerWrapperComponentIndex = Math.max(0, rootInternalNode.getComponents().size() - 1);
    return DebugComponent.getInstance(rootResult, outerWrapperComponentIndex);
  }

  private static String generateGlobalKey(ComponentContext context, String componentKey) {
    final ComponentTree tree = context.getComponentTree();
    return System.identityHashCode(tree) + componentKey;
  }

  static void applyOverrides(ComponentContext context, Component component, String componentKey) {
    final String key = generateGlobalKey(context, componentKey);
    final Overrider overrider = sOverriders.get(key);
    if (overrider != null) {
      overrider.applyComponentOverrides(key, component);
      overrider.applyStateOverrides(
          key, component.getStateContainer(context.getLayoutStateContext(), componentKey));
    }
  }

  static void applyOverrides(ComponentContext context, InternalNode node) {
    if (node.getComponents() == null || node.getComponents().isEmpty()) {
      return;
    }

    final Component component = node.getComponents().get(0);
    final String componentkey =
        ComponentUtils.getGlobalKey(
            component, node.getComponentKeys() == null ? null : node.getComponentKeys().get(0));

    final String key = generateGlobalKey(context, componentkey);
    final Overrider overrider = sOverriders.get(key);
    if (overrider != null) {
      overrider.applyLayoutOverrides(key, new DebugLayoutNode(node));
    }
  }

  public void setOverrider(Overrider overrider) {
    sOverriders.put(mGlobalKey, overrider);
  }

  /**
   * Get the list of components composed by this component. This will not include any {@link View}s
   * that are mounted by this component as those are not components. Use {@link this#getMountedView}
   * for that.
   *
   * @return A list of child components.
   */
  public List<DebugComponent> getChildComponents() {
    if (!isLayoutNode()) {
      final int nextComponentIndex = mComponentIndex - 1;
      DebugComponent component = getInstance(mResult, nextComponentIndex);
      if (component != null) {
        return Collections.singletonList(component);
      } else {
        return Collections.emptyList();
      }
    }

    final List<DebugComponent> children = new ArrayList<>();

    for (int i = 0, count = mResult.getChildCount(); i < count; i++) {
      final LithoLayoutResult childNode = mResult.getChildAt(i);
      final int index = Math.max(0, childNode.getInternalNode().getComponents().size() - 1);
      DebugComponent component = getInstance(childNode, index);
      if (component != null) {
        children.add(component);
      }
    }

    final LithoLayoutResult nestedTree =
        mResult instanceof NestedTreeHolderResult
            ? ((NestedTreeHolderResult) mNode).getNestedResult()
            : null;
    if (nestedTree != null) {
      for (int i = 0, count = nestedTree.getChildCount(); i < count; i++) {
        final LithoLayoutResult childNode = nestedTree.getChildAt(i);
        int index = Math.max(0, childNode.getInternalNode().getComponents().size() - 1);
        DebugComponent component = getInstance(childNode, index);
        if (component != null) {
          children.add(component);
        }
      }
    }

    return children;
  }

  /** @return A mounted view or null if this component does not mount a view. */
  @Nullable
  public View getMountedView() {
    final Component component = mNode.getTailComponent();
    if (Component.isMountViewSpec(component)) {
      return (View) getMountedContent();
    }

    return null;
  }

  /** @return A mounted drawable or null if this component does not mount a drawable. */
  @Nullable
  public Drawable getMountedDrawable() {
    final Component component = mNode.getTailComponent();
    if (Component.isMountDrawableSpec(component)) {
      return (Drawable) getMountedContent();
    }

    return null;
  }

  /** @return The litho view hosting this component. */
  @Nullable
  public LithoView getLithoView() {
    final ComponentContext c = mNode.getContext();
    final ComponentTree tree = c == null ? null : c.getComponentTree();
    return tree == null ? null : tree.getLithoView();
  }

  /** @return The bounds of this component relative to its hosting {@link LithoView}. */
  public Rect getBoundsInLithoView() {
    if (isRoot()) {
      return new Rect(0, 0, mNode.getWidth(), mNode.getHeight());
    }

    final int x = getXFromRoot(mResult);
    final int y = getYFromRoot(mResult);
    return new Rect(x, y, x + mNode.getWidth(), y + mNode.getHeight());
  }

  /** @return The bounds of this component relative to its parent. */
  public Rect getBounds() {
    final int x = mNode.getX();
    final int y = mNode.getY();
    return new Rect(x, y, x + mNode.getWidth(), y + mNode.getHeight());
  }

  /** @return True if this and given debug components share the same internal node */
  public boolean isSameNode(DebugComponent other) {
    return this.mNode == other.mNode;
  }

  /** @return the {@link ComponentContext} for this component. */
  public ComponentContext getContext() {
    return mNode.getContext();
  }

  /** @return True if this not has layout information attached to it (backed by a Yoga node) */
  public boolean isLayoutNode() {
    return mComponentIndex == 0;
  }

  /** @return This component's testKey or null if none is set. */
  @Nullable
  public String getTestKey() {
    return isLayoutNode() ? mNode.getTestKey() : null;
  }

  /**
   * Returns this component's testKey or null if none is set.
   *
   * <p>Unlike {@link #getTestKey()}, this function can return a test key set on any Component,
   * including container Components which resolve into LayoutNodes.
   *
   * <p>Unlike {@link #getTestKey()}, this function can also return test keys set on individual
   * Components even when they are all resolved into a single InternalNode.
   */
  @Nullable
  public String getComponentTestKey() {
    Component component = mNode.getComponents().get(mComponentIndex);
    CommonProps props = component.getCommonProps();
    return props == null ? null : props.getTestKey();
  }

  /**
   * @return This component's componentTag or null if none is set. Unlike {@link #getTestKey}, this
   *     will return tags for any Component, including Components which are not LayoutNodes.
   */
  @Nullable
  public Object getComponentTag() {
    Component component = mNode.getComponents().get(mComponentIndex);
    CommonProps props = component.getCommonProps();
    return props != null ? props.getComponentTag() : null;
  }

  /**
   * @return A concatenated string of all text content within the underlying LithoView. Null if the
   *     node doesn't have an associated LithoView.
   */
  @Nullable
  public String getAllTextContent() {
    final LithoView lithoView = getLithoView();

    if (lithoView == null) {
      return null;
    }

    final MountDelegateTarget mountDelegateTarget = lithoView.getMountDelegateTarget();
    final StringBuilder sb = new StringBuilder();

    for (int i = 0, size = mountDelegateTarget.getMountItemCount(); i < size; i++) {
      final MountItem mountItem = mountDelegateTarget.getMountItemAt(i);
      final Component mountItemComponent =
          mountItem == null ? null : getLayoutOutput(mountItem).getComponent();
      if (mountItemComponent != null) {
        final Object content = mountItem.getContent();

        if (content instanceof TextContent) {
          for (CharSequence charSequence : ((TextContent) content).getTextItems()) {
            sb.append(charSequence);
          }
        } else if (content instanceof TextView) {
          sb.append(((TextView) content).getText());
        }
      }
    }

    return sb.toString();
  }

  /**
   * @return The text content of the component wrapped by the debug component, or null if no
   *     TextContent/TextView are found.
   */
  @Nullable
  public String getTextContent() {
    final LithoView lithoView = getLithoView();
    if (lithoView == null) {
      return null;
    }

    final Component component = getComponent();
    final MountDelegateTarget mountDelegateTarget = lithoView.getMountDelegateTarget();
    for (int i = 0, size = mountDelegateTarget.getMountItemCount(); i < size; i++) {
      final MountItem mountItem = mountDelegateTarget.getMountItemAt(i);
      final Component mountItemComponent =
          mountItem == null ? null : getLayoutOutput(mountItem).getComponent();
      if (mountItemComponent != null && mountItemComponent.getId() == component.getId()) {
        final Object content = mountItem.getContent();
        final StringBuilder sb = new StringBuilder();
        if (content instanceof TextContent) {
          for (CharSequence charSequence : ((TextContent) content).getTextItems()) {
            sb.append(charSequence);
          }
        } else if (content instanceof TextView) {
          sb.append(((TextView) content).getText());
        }
        if (sb.length() != 0) {
          return sb.toString();
        }
      }
    }
    return null;
  }

  /** @return The {@link ComponentHost} that wraps this component or null if one cannot be found. */
  @Nullable
  public ComponentHost getComponentHost() {
    final LithoView lithoView = getLithoView();
    final Component component = getComponent();

    if (lithoView == null) {
      return null;
    }
    final MountDelegateTarget mountDelegateTarget = lithoView.getMountDelegateTarget();
    for (int i = 0, size = mountDelegateTarget.getMountItemCount(); i < size; i++) {
      final MountItem mountItem = mountDelegateTarget.getMountItemAt(i);
      final Component mountItemComponent =
          mountItem == null ? null : getLayoutOutput(mountItem).getComponent();
      if (mountItemComponent != null && mountItemComponent.isEquivalentTo(component)) {
        return (ComponentHost) mountItem.getHost();
      }
    }

    return null;
  }

  /** @return This component's key or null if none is set. */
  @Nullable
  public String getKey() {
    return mNode.getComponents().get(mComponentIndex).getKey();
  }

  /** @return The Component instance this debug component wraps. */
  public Component getComponent() {
    return mNode.getComponents().get(mComponentIndex);
  }

  private @Nullable String getGlobalKeyFromNode() {
    return mNode.getComponentKeys() == null ? null : mNode.getComponentKeys().get(mComponentIndex);
  }

  /** @return If this debug component represents a layout node, return it. */
  @Nullable
  public DebugLayoutNode getLayoutNode() {
    if (isLayoutNode()) {
      return new DebugLayoutNode(mNode);
    }
    return null;
  }

  public void rerender() {
    final LithoView lithoView = getLithoView();
    if (lithoView != null) {
      lithoView.forceRelayout();
    }
  }

  @Nullable
  public StateContainer getStateContainer() {
    final LayoutStateContext layoutStateContext = getContext().getLayoutStateContext();
    final Component component = getComponent();
    final String globalKey = ComponentUtils.getGlobalKey(component, getGlobalKeyFromNode());

    return getComponent().getStateContainer(layoutStateContext, globalKey);
  }

  @Nullable
  private static LithoLayoutResult parent(LithoLayoutResult node) {
    return node.getParent();
  }

  private static int getXFromRoot(@Nullable LithoLayoutResult node) {
    if (node == null) {
      return 0;
    }
    return node.getX() + getXFromRoot(parent(node));
  }

  private static int getYFromRoot(@Nullable LithoLayoutResult node) {
    if (node == null) {
      return 0;
    }
    return node.getY() + getYFromRoot(parent(node));
  }

  public String getGlobalKey() {
    return mGlobalKey;
  }

  public boolean canResolve() {
    return getComponent().canResolve();
  }

  public boolean isRoot() {
    return mComponentIndex == 0 && mNode.getParent() == null;
  }

  @Nullable
  private Object getMountedContent() {
    if (!isLayoutNode()) {
      return null;
    }

    final ComponentContext context = mNode.getContext();
    final ComponentTree tree = context == null ? null : context.getComponentTree();
    final LithoView view = tree == null ? null : tree.getLithoView();
    final MountDelegateTarget mountDelegateTarget =
        view == null ? null : view.getMountDelegateTarget();

    if (mountDelegateTarget != null) {
      for (int i = 0, count = mountDelegateTarget.getMountItemCount(); i < count; i++) {
        final MountItem mountItem = mountDelegateTarget.getMountItemAt(i);
        final Component component =
            mountItem == null ? null : getLayoutOutput(mountItem).getComponent();

        if (component != null && component == mNode.getTailComponent()) {
          return mountItem.getContent();
        }
      }
    }

    return null;
  }
}
