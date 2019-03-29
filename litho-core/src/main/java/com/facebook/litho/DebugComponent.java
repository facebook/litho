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

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
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
  private InternalNode mNode;
  private int mComponentIndex;

  private DebugComponent() {}

  static synchronized DebugComponent getInstance(InternalNode node, int componentIndex) {
    final DebugComponent debugComponent = new DebugComponent();
    final ComponentContext context = node.getContext();
    final Component component = node.getComponents().get(componentIndex);

    debugComponent.mGlobalKey = generateGlobalKey(context, component);
    debugComponent.mNode = node;
    debugComponent.mComponentIndex = componentIndex;
    node.registerDebugComponent(debugComponent);

    return debugComponent;
  }

  /**
   * @return The root {@link DebugComponent} of a LithoView. This should be the start of your
   * traversal.
   */
  @Nullable
  public static DebugComponent getRootInstance(Component component) {
    return getRootInstance(component.getScopedContext().getComponentTree());
  }

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

  private static String generateGlobalKey(ComponentContext context, Component component) {
    final ComponentTree tree = context.getComponentTree();
    final String componentKey = component.getGlobalKey();
    return System.identityHashCode(tree) + componentKey;
  }

  static void applyOverrides(ComponentContext context, Component component) {
    final String key = generateGlobalKey(context, component);
    final Overrider overrider = sOverriders.get(key);
    if (overrider != null) {
      overrider.applyComponentOverrides(key, component);
      overrider.applyStateOverrides(key, component.getStateContainer());
    }
  }

  static void applyOverrides(ComponentContext context, InternalNode node) {
    if (node.getComponents() == null || node.getComponents().isEmpty()) {
      return;
    }

    final String key = generateGlobalKey(context, node.getComponents().get(0));
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

    final List<DebugComponent> children = new ArrayList<>();

    for (int i = 0, count = mNode.getChildCount(); i < count; i++) {
      final InternalNode childNode = mNode.getChildAt(i);
      final int outerWrapperComponentIndex = Math.max(0, childNode.getComponents().size() - 1);
      children.add(getInstance(childNode, outerWrapperComponentIndex));
    }

    final InternalNode nestedTree = mNode.getNestedTree();
    if (nestedTree != null && nestedTree.isInitialized()) {
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

    if (lithoView == null) {
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
        } else if (content instanceof TextView) {
          sb.append(((TextView) content).getText());
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

    if (lithoView == null) {
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
    return getComponent().getStateContainer();
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
