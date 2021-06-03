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
  private final Map<String, ComponentContext> mGlobalKeyToScopedContext = new HashMap<>();
  private final Map<String, ScopedComponentInfo> mGlobalKeyToScopedInfo = new HashMap<>();
  private final Map<Integer, InternalNode> mComponentIdToWillRenderLayout = new HashMap<>();

  private static @Nullable LayoutState sTestLayoutState;

  private boolean mIsLayoutStarted = false;

  boolean mIsScopedInfoCopiedFromLSCInstance = false;

  public static LayoutStateContext getTestInstance(ComponentContext c) {
    if (sTestLayoutState == null) {
      sTestLayoutState = new LayoutState(c);
    }

    return new LayoutStateContext(sTestLayoutState, c.getComponentTree(), null);
  }

  void copyScopedInfoFrom(LayoutStateContext from, StateHandler stateHandler) {
    mIsScopedInfoCopiedFromLSCInstance = true;

    mGlobalKeyToScopedContext.clear();
    for (Map.Entry<String, ComponentContext> e : from.mGlobalKeyToScopedContext.entrySet()) {
      final String key = e.getKey();
      final ComponentContext context =
          e.getValue().createUpdatedComponentContext(this, stateHandler);
      mGlobalKeyToScopedContext.put(key, context);
    }

    mGlobalKeyToScopedInfo.clear();
    for (Map.Entry<String, ScopedComponentInfo> e : from.mGlobalKeyToScopedInfo.entrySet()) {
      final String key = e.getKey();
      final ScopedComponentInfo info = e.getValue().copy();
      mGlobalKeyToScopedInfo.put(key, info);
    }

    mComponentIdToWillRenderLayout.clear();
    mComponentIdToWillRenderLayout.putAll(from.mComponentIdToWillRenderLayout);
  }

  @VisibleForTesting
  LayoutStateContext(final LayoutState layoutState, @Nullable final ComponentTree componentTree) {
    this(layoutState, componentTree, null);
  }

  @VisibleForTesting
  LayoutStateContext(
      final LayoutState layoutState,
      final @Nullable ComponentTree componentTree,
      final @Nullable LayoutStateFuture layoutStateFuture) {
    mLayoutStateRef = layoutState;
    mLayoutStateFuture = layoutStateFuture;
    mComponentTree = componentTree;
  }

  void addScopedComponentInfo(
      final String globalKey,
      final Component component,
      final ComponentContext scopedContext,
      final ComponentContext parentContext) {

    mGlobalKeyToScopedContext.put(globalKey, scopedContext);

    final EventHandler<ErrorEvent> errorEventHandler =
        ComponentUtils.createOrGetErrorEventHandler(component, parentContext, scopedContext);

    final ScopedComponentInfo info = new ScopedComponentInfo(component, errorEventHandler);

    final ScopedComponentInfo previous = mGlobalKeyToScopedInfo.put(globalKey, info);
    if (previous != null) {
      previous.transferInto(info);
    }
  }

  ScopedComponentInfo getScopedComponentInfo(String globalKey) {
    final ScopedComponentInfo scopedComponentInfo = mGlobalKeyToScopedInfo.get(globalKey);
    if (scopedComponentInfo == null) {
      throw new IllegalStateException("ScopedComponentInfo is null for globalKey: " + globalKey);
    }

    return scopedComponentInfo;
  }

  ComponentContext getScopedContext(String globalKey) {
    if (globalKey == null) {
      return null;
    }

    final ComponentContext context = mGlobalKeyToScopedContext.get(globalKey);
    if (context == null) {
      throw new IllegalStateException("ComponentContext is null for globalKey: " + globalKey);
    }
    return context;
  }

  @Nullable
  InternalNode consumeLayoutCreatedInWillRender(int componentId) {
    return mComponentIdToWillRenderLayout.remove(componentId);
  }

  @Nullable
  InternalNode getLayoutCreatedInWillRender(int componentId) {
    return mComponentIdToWillRenderLayout.get(componentId);
  }

  void setLayoutCreatedInWillRender(int componentId, final @Nullable InternalNode node) {
    mComponentIdToWillRenderLayout.put(componentId, node);
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

  public @Nullable LayoutStateFuture getLayoutStateFuture() {
    return mLayoutStateFuture;
  }

  boolean isLayoutInterrupted() {
    boolean isInterruptRequested =
        mLayoutStateFuture != null && mLayoutStateFuture.isInterruptRequested();
    boolean isInterruptible = mLayoutStateRef != null && mLayoutStateRef.isInterruptible();

    return isInterruptible && isInterruptRequested;
  }

  boolean isLayoutReleased() {
    return mLayoutStateFuture != null && mLayoutStateFuture.isReleased();
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
