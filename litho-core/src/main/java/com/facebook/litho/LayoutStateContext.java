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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.ComponentTree.LayoutStateFuture;
import com.facebook.litho.config.ComponentsConfiguration;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps objects which should only be available for the duration of a LayoutState, to access them in
 * other classes such as ComponentContext during layout state calculation. When the layout
 * calculation finishes, the LayoutState reference is nullified. Using a wrapper instead of passing
 * the instances directly helps with clearing out the reference from all objects that hold on to it,
 * without having to keep track of all these objects to clear out the references.
 */
public class LayoutStateContext {
  private @Nullable LayoutState mLayoutStateRef;
  private @Nullable ComponentTree mComponentTree;
  private @Nullable LayoutStateFuture mLayoutStateFuture;
  private final @Nullable Map<String, ComponentContext> mGlobalKeyToScopedContext;
  private final @Nullable Map<String, ScopedComponentInfo> mGlobalKeyToScopedInfo;
  private @Nullable LithoYogaMeasureFunction mLithoYogaMeasureFunction;

  private static @Nullable LayoutState sTestLayoutState;

  private boolean mIsLayoutStarted = false;

  public static LayoutStateContext getTestInstance(ComponentContext c) {
    if (sTestLayoutState == null) {
      sTestLayoutState = new LayoutState(c);
    }

    return new LayoutStateContext(sTestLayoutState, c.getComponentTree(), null);
  }

  void copyScopedInfoFrom(LayoutStateContext layoutStateContext) {
    if (mGlobalKeyToScopedContext != null && layoutStateContext.mGlobalKeyToScopedContext != null) {
      mGlobalKeyToScopedContext.putAll(layoutStateContext.mGlobalKeyToScopedContext);
    }

    if (mGlobalKeyToScopedInfo != null && layoutStateContext.mGlobalKeyToScopedInfo != null) {
      mGlobalKeyToScopedInfo.putAll(layoutStateContext.mGlobalKeyToScopedInfo);
    }
  }

  @VisibleForTesting
  LayoutStateContext(final LayoutState layoutState, @Nullable final ComponentTree componentTree) {
    this(layoutState, componentTree, null);
  }

  @VisibleForTesting
  LayoutStateContext(
      final LayoutState layoutState,
      @Nullable final ComponentTree componentTree,
      final @Nullable LayoutStateFuture layoutStateFuture) {
    mLayoutStateRef = layoutState;
    mLayoutStateFuture = layoutStateFuture;
    mComponentTree = componentTree;
    if (ComponentsConfiguration.useStatelessComponent) {
      mGlobalKeyToScopedContext = new HashMap<>();
      mGlobalKeyToScopedInfo = new HashMap<>();
      mLithoYogaMeasureFunction =
          new LithoYogaMeasureFunction(
              this, layoutState == null ? null : layoutState.getPrevLayoutStateContext());
    } else {
      mGlobalKeyToScopedContext = null;
      mGlobalKeyToScopedInfo = null;
      mLithoYogaMeasureFunction = null;
    }
  }

  void addScopedComponentInfo(
      String globalKey,
      Component component,
      ComponentContext scopedContext,
      ComponentContext parentContext) {
    mGlobalKeyToScopedContext.put(globalKey, scopedContext);
    mGlobalKeyToScopedInfo.put(
        globalKey,
        new ScopedComponentInfo(
            component,
            ComponentUtils.createOrGetErrorEventHandler(component, parentContext, scopedContext)));
  }

  @Nullable
  ScopedComponentInfo getScopedComponentInfo(String globalKey) {
    return mGlobalKeyToScopedInfo.get(globalKey);
  }

  @Nullable
  ComponentContext getScopedContext(String globalKey) {
    return mGlobalKeyToScopedInfo == null ? null : mGlobalKeyToScopedContext.get(globalKey);
  }

  void releaseReference() {
    mLayoutStateRef = null;
    mLayoutStateFuture = null;
    mComponentTree = null;
  }

  /** Returns the LayoutState instance or null if the layout state has been released. */
  @Nullable
  LayoutState getLayoutState() {
    return mLayoutStateRef;
  }

  @Nullable
  ComponentTree getComponentTree() {
    return mComponentTree;
  }

  @Nullable
  LithoYogaMeasureFunction getLithoYogaMeasureFunction() {
    return mLithoYogaMeasureFunction;
  }

  public @Nullable LayoutStateFuture getLayoutStateFuture() {
    return mLayoutStateFuture;
  }

  boolean isLayoutInterrupted() {
    boolean isInterruptRequested =
        mLayoutStateFuture == null ? false : mLayoutStateFuture.isInterruptRequested();
    boolean isInterruptible = mLayoutStateRef == null ? false : mLayoutStateRef.isInterruptible();

    return isInterruptible && isInterruptRequested;
  }

  boolean isLayoutReleased() {
    return mLayoutStateFuture == null ? false : mLayoutStateFuture.isReleased();
  }

  public void markLayoutUninterruptible() {
    if (mLayoutStateRef != null) {
      mLayoutStateRef.setInterruptible(false);
    }
  }

  void markLayoutStarted() {
    if (mIsLayoutStarted) {
      throw new IllegalStateException(
          "Duplicate layout of a component: "
              + (mComponentTree != null ? mComponentTree.getRoot() : null));
    }
    mIsLayoutStarted = true;
  }
}
