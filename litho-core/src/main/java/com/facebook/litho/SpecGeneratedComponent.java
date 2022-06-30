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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.Component.RenderData;
import com.facebook.litho.annotations.LayoutSpec;

/** Base class for all component generated via the Spec API (@LayoutSpec and @MountSpec). */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class SpecGeneratedComponent extends Component implements EventTriggerTarget {

  private final String mSimpleName;

  protected SpecGeneratedComponent(String simpleName) {
    mSimpleName = simpleName;
  }

  @VisibleForTesting
  protected SpecGeneratedComponent(int identityHashCode, String simpleName) {
    super(identityHashCode);
    mSimpleName = simpleName;
  }

  /** Should only be used by logging to provide more readable messages. */
  @Override
  public final String getSimpleName() {
    final Component delegate = getSimpleNameDelegate();
    if (delegate == null) {
      return mSimpleName;
    }

    return mSimpleName + "(" + getFirstNonSimpleNameDelegate(delegate).getSimpleName() + ")";
  }

  /**
   * @return the Component this Component should delegate its getSimpleName calls to. See {@link
   *     LayoutSpec#simpleNameDelegate()}
   */
  protected @Nullable Component getSimpleNameDelegate() {
    return null;
  }

  private static Component getFirstNonSimpleNameDelegate(Component component) {
    Component current = component;
    while (current instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) current).getSimpleNameDelegate() != null) {
      current = ((SpecGeneratedComponent) current).getSimpleNameDelegate();
    }
    return current;
  }

  @Override
  protected RenderResult render(ComponentContext c, int widthSpec, int heightSpec) {
    if (Component.isLayoutSpecWithSizeSpec(this)) {
      return new RenderResult(onCreateLayoutWithSizeSpec(c, widthSpec, heightSpec));
    } else {
      return new RenderResult(onCreateLayout(c));
    }
  }

  /**
   * Generate a tree of {@link ComponentLayout} representing the layout structure of the {@link
   * Component} and its sub-components.
   *
   * @param c The {@link ComponentContext} to build a {@link ComponentLayout} tree.
   */
  protected Component onCreateLayout(ComponentContext c) {
    return Column.create(c).build();
  }

  protected Component onCreateLayoutWithSizeSpec(
      ComponentContext c, int widthSpec, int heightSpec) {
    return Column.create(c).build();
  }

  @Override
  protected boolean usesLocalStateContainer() {
    return true;
  }

  @Override
  @Nullable
  public final Object acceptTriggerEvent(
      EventTrigger eventTrigger, Object eventState, Object[] params) {
    try {
      return acceptTriggerEventImpl(eventTrigger, eventState, params);
    } catch (Exception e) {
      if (eventTrigger.mComponentContext != null) {
        ComponentUtils.handle(eventTrigger.mComponentContext, e);
        return null;
      } else {
        throw e;
      }
    }
  }

  protected @Nullable Object acceptTriggerEventImpl(
      EventTrigger eventTrigger, Object eventState, Object[] params) {
    // Do nothing by default
    return null;
  }

  protected void applyPreviousRenderData(RenderData previousRenderData) {}

  protected boolean needsPreviousRenderData() {
    return false;
  }
}
