/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentHost;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.DebugComponent;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaNode;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Wraps a {@link com.facebook.litho.DebugComponent} exposing only information that are safe to use
 * for test assertions.
 */
@Immutable
public class InspectableComponent {
  private final DebugComponent mComponent;

  private InspectableComponent(DebugComponent component) {
    mComponent = component;
  }

  /**
   * @return The root {@link InspectableComponent} of a LithoView.
   */
  @Nullable
  public static InspectableComponent getRootInstance(LithoView view) {
    final DebugComponent rootInstance = DebugComponent.getRootInstance(view);

    if (rootInstance == null) {
      return null;
    }

    return new InspectableComponent(rootInstance);
  }

  /** @return A conanical name for this component. Suitable to present to the user. */
  public String getName() {
    return mComponent.getName();
  }

  /** @return A simpler canonical name for this component. Suitable to present to the user. */
  public String getSimpleName() {
    return mComponent.getSimpleName();
  }

  /**
   * @return The class of the underlying Component.
   */
  public Class getComponentClass() {
    return mComponent.getComponentClass();
  }

  /**
   * Get the list of components composed by this component. This will not include any {@link View}s
   * that are mounted by this component as those are not components. Use {@link this#getMountedView}
   * for that.
   *
   * @return A list of child components.
   */
  public List<InspectableComponent> getChildComponents() {
    final List<DebugComponent> debugComponents = mComponent.getChildComponents();
    final List<InspectableComponent> res = new ArrayList<>(debugComponents.size());

    for (DebugComponent debugComponent : debugComponents) {
      res.add(new InspectableComponent(debugComponent));
    }

    return res;
  }

  /** @return A mounted view or null if this component does not mount a view. */
  @Nullable
  public View getMountedView() {
    return mComponent.getMountedView();
  }

  /** @return A mounted drawable or null if this component does not mount a drawable. */
  @Nullable
  public Drawable getMountedDrawable() {
    return mComponent.getMountedDrawable();
  }

  /** @return The litho view hosting this component. */
  @Nullable
  public LithoView getLithoView() {
    return mComponent.getLithoView();
  }

  /** @return The bounds of this component relative to its hosting {@link LithoView}. */
  public Rect getBoundsInLithoView() {
    return mComponent.getBoundsInLithoView();
  }

  /** @return The bounds of this component relative to its parent. */
  public Rect getBounds() {
    return mComponent.getBounds();
  }

  /** @return This component's testKey or null if none is set. */
  @Nullable
  public String getTestKey() {
    return mComponent.getTestKey();
  }

  /**
   * @return A concatenated string of all text content within the underlying LithoView. Null if the
   *     node doesn't have an associated LithoView.
   */
  @Nullable
  public String getTextContent() {
    return mComponent.getTextContent();
  }

  /** @return The {@link ComponentHost} that wraps this component or null if one cannot be found. */
  @Nullable
  public ComponentHost getComponentHost() {
    return mComponent.getComponentHost();
  }

  /** @return This component's key or null if none is set. */
  @Nullable
  public String getKey() {
    return mComponent.getKey();
  }

  /** @return The Component instance this debug component wraps. */
  @Nullable
  public Component getComponent() {
    return mComponent.getComponent();
  }

  /** @return The Yoga node asscociated with this debug component. May be null. */
  @Nullable
  public YogaNode getYogaNode() {
    return mComponent.getYogaNode();
  }

  /** @return The foreground drawable asscociated with this debug component. May be null. */
  @Nullable
  public Drawable getForeground() {
    return mComponent.getForeground();
  }

  /** @return The background drawable asscociated with this debug component. May be null. */
  @Nullable
  public Reference<? extends Drawable> getBackground() {
    return mComponent.getBackground();
  }

  /** @return The int value of the importantForAccessibility property on this debug component. */
  @Nullable
  public Integer getImportantForAccessibility() {
    return mComponent.getImportantForAccessibility();
  }

  /** @return The boolean value of the focusable property on this debug component. */
  public boolean getFocusable() {
    return mComponent.getFocusable();
  }

  /** @return The content description CharSequence on this debug component. May be null. */
  @Nullable
  public CharSequence getContentDescription() {
    return mComponent.getContentDescription();
  }

  @Nullable
  public ComponentLifecycle.StateContainer getStateContainer() {
    return mComponent.getStateContainer();
  }

  public String getId() {
    return mComponent.getId();
  }

  @Nullable
  public EventHandler getClickHandler() {
    return mComponent.getClickHandler();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("InspectableComponent of ");
    sb.append(getSimpleName());
    sb.append(" with hierarchy\n");
    sb.append(LithoViewTestHelper.viewToString(mComponent.getLithoView()));

    return sb.toString();
  }
}
