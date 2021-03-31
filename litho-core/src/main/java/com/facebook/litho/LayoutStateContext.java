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
  private @Nullable Map<String, ComponentContext> mGlobalKeyToScopedContext;
  private @Nullable Map<String, ScopedComponentInfo> mGlobalKeyToScopedInfo;
  private @Nullable LithoYogaMeasureFunction mLithoYogaMeasureFunction;

  private static @Nullable LayoutState sTestLayoutState;

  private boolean mIsLayoutStarted = false;

  private boolean mIsScopedInfoCopiedFromLSCInstance = false;

  public static LayoutStateContext getTestInstance(ComponentContext c) {
    if (sTestLayoutState == null) {
      sTestLayoutState = new LayoutState(c);
    }

    return new LayoutStateContext(sTestLayoutState, c.getComponentTree(), null);
  }

  void copyScopedInfoFrom(LayoutStateContext layoutStateContext) {
    mIsScopedInfoCopiedFromLSCInstance = true;

    if (mGlobalKeyToScopedContext == null) {
      mGlobalKeyToScopedContext = new HashMap<>();
    }

    if (mGlobalKeyToScopedInfo == null) {
      mGlobalKeyToScopedInfo = new HashMap<>();
    }

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
  }

  void addScopedComponentInfo(
      String globalKey,
      Component component,
      ComponentContext scopedContext,
      ComponentContext parentContext) {
    if (mGlobalKeyToScopedContext == null) {
      mGlobalKeyToScopedContext = new HashMap<>();
    }

    mGlobalKeyToScopedContext.put(globalKey, scopedContext);

    if (mGlobalKeyToScopedInfo == null) {
      mGlobalKeyToScopedInfo = new HashMap<>();
    }

    InterStagePropsContainer newInterStagePropsContainer =
        component.createInterStagePropsContainer();

    if (mGlobalKeyToScopedInfo.containsKey(globalKey)) {
      InterStagePropsContainer prevInterStagePropsContainer =
          mGlobalKeyToScopedInfo.get(globalKey).getInterStagePropsContainer();

      try {
        component.copyInterStageImpl(newInterStagePropsContainer, prevInterStagePropsContainer);
      } catch (NullPointerException ex) {
        if (ComponentsConfiguration.throwExceptionInterStagePropsContainerNull) {
          throw new IllegalStateException(
              "Encountered NPE while copying ISPContainer: "
                  + component
                  + " ,globalKey: "
                  + globalKey
                  + " ,newISPContainer: "
                  + newInterStagePropsContainer
                  + " ,prevISPContainer: "
                  + prevInterStagePropsContainer
                  + " ,scopedInfoCopiedFromLSC: "
                  + mIsScopedInfoCopiedFromLSCInstance
                  + " ,hasCachedLayout: "
                  + (mLayoutStateRef == null ? "null" : mLayoutStateRef.hasCachedLayout(component)),
              ex);
        }
      }
    }

    mGlobalKeyToScopedInfo.put(
        globalKey,
        new ScopedComponentInfo(
            component,
            newInterStagePropsContainer,
            ComponentUtils.createOrGetErrorEventHandler(component, parentContext, scopedContext)));
  }

  @Nullable
  ScopedComponentInfo getScopedComponentInfo(String globalKey) {
    return mGlobalKeyToScopedInfo == null ? null : mGlobalKeyToScopedInfo.get(globalKey);
  }

  @Nullable
  ComponentContext getScopedContext(String globalKey) {
    return mGlobalKeyToScopedContext == null ? null : mGlobalKeyToScopedContext.get(globalKey);
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
    if (mLithoYogaMeasureFunction == null) {
      mLithoYogaMeasureFunction =
          new LithoYogaMeasureFunction(
              this, mLayoutStateRef == null ? null : mLayoutStateRef.getPrevLayoutStateContext());
    }

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
