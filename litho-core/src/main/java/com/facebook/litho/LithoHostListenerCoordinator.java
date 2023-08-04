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

import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.DynamicPropsExtension.DynamicPropsExtensionState;
import com.facebook.litho.LithoViewAttributesExtension.LithoViewAttributesState;
import com.facebook.litho.TransitionsExtension.TransitionsExtensionState;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.VisibleBoundsCallbacks;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import com.facebook.rendercore.visibility.VisibilityMountExtension;
import com.facebook.rendercore.visibility.VisibilityMountExtension.VisibilityMountExtensionState;

/** Helper for dispatching events to multiple MountListenerExtensions in Litho. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoHostListenerCoordinator {

  private final MountDelegateTarget mMountDelegateTarget;

  private @Nullable ExtensionState<LithoViewAttributesState> mViewAttributesExtensionState;
  private @Nullable ExtensionState<Void> mNestedLithoViewsExtensionState;
  private @Nullable ExtensionState<DynamicPropsExtensionState> mDynamicPropsExtensionState;
  private @Nullable ExtensionState<VisibilityMountExtensionState> mVisibilityExtensionState;
  private @Nullable ExtensionState<TransitionsExtensionState> mTransitionsExtensionState;
  private @Nullable ExtensionState<IncrementalMountExtensionState> mIncrementalMountExtensionState;
  private @Nullable ExtensionState<Void> mEndToEndTestingExtensionState;
  private @Nullable MountExtension mUIDebuggerExtension;

  public LithoHostListenerCoordinator(MountDelegateTarget mountDelegateTarget) {
    mMountDelegateTarget = mountDelegateTarget;
  }

  public void setCollectNotifyVisibleBoundsChangedCalls(boolean value) {
    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();

    if (mountDelegate != null) {
      mountDelegate.setCollectVisibleBoundsChangedCalls(value);
    }
  }

  private void startNotifyVisibleBoundsChangedSection() {
    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();

    if (mountDelegate != null) {
      mountDelegate.startNotifyVisibleBoundsChangedSection();
    }
  }

  private void endNotifyVisibleBoundsChangedSection() {
    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();

    if (mountDelegate != null) {
      mountDelegate.endNotifyVisibleBoundsChangedSection();
    }
  }

  public void beforeMount(LayoutState input, Rect localVisibleRect) {

    if (mNestedLithoViewsExtensionState != null) {
      mNestedLithoViewsExtensionState.beforeMount(localVisibleRect, input);
    }

    if (mTransitionsExtensionState != null) {
      mTransitionsExtensionState.beforeMount(localVisibleRect, input);
    }

    if (mEndToEndTestingExtensionState != null) {
      mEndToEndTestingExtensionState.beforeMount(localVisibleRect, input);
    }

    if (mViewAttributesExtensionState != null) {
      mViewAttributesExtensionState.beforeMount(localVisibleRect, input);
    }

    if (mDynamicPropsExtensionState != null) {
      mDynamicPropsExtensionState.beforeMount(localVisibleRect, input);
    }

    if (mVisibilityExtensionState != null) {
      mVisibilityExtensionState.beforeMount(localVisibleRect, input);
    }

    if (mIncrementalMountExtensionState != null) {
      mIncrementalMountExtensionState.beforeMount(localVisibleRect, input);
    }
  }

  public void processVisibilityOutputs(Rect localVisibleRect, boolean isDirty) {
    startNotifyVisibleBoundsChangedSection();

    if (mVisibilityExtensionState != null) {
      if (isDirty) {
        final boolean processVisibilityOutputs =
            VisibilityMountExtension.shouldProcessVisibilityOutputs(mVisibilityExtensionState);

        if (processVisibilityOutputs) {
          VisibilityMountExtension.processVisibilityOutputs(
              mVisibilityExtensionState, localVisibleRect, true);
        }
      } else {
        MountExtension extension = mVisibilityExtensionState.getExtension();
        if (extension instanceof VisibleBoundsCallbacks) {
          ((VisibleBoundsCallbacks) extension)
              .onVisibleBoundsChanged(mVisibilityExtensionState, localVisibleRect);
        }
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  void enableIncrementalMount(boolean useGapWorker) {
    if (mIncrementalMountExtensionState != null) {
      return;
    }

    mIncrementalMountExtensionState =
        mMountDelegateTarget.registerMountExtension(
            IncrementalMountExtension.getInstance(useGapWorker));
  }

  void disableIncrementalMount() {
    if (mIncrementalMountExtensionState == null) {
      return;
    }

    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();
    if (mountDelegate != null) {
      mountDelegate.unregisterMountExtension(mIncrementalMountExtensionState.getExtension());
    }

    mIncrementalMountExtensionState = null;
  }

  void enableVisibilityProcessing(BaseMountingView lithoView) {
    if (mVisibilityExtensionState != null) {
      return;
    }

    mVisibilityExtensionState =
        mMountDelegateTarget.registerMountExtension(VisibilityMountExtension.getInstance());
    if (mVisibilityExtensionState != null) {
      VisibilityMountExtension.setRootHost(mVisibilityExtensionState, lithoView);
    }
  }

  void disableVisibilityProcessing() {
    if (mVisibilityExtensionState == null) {
      return;
    }

    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();
    if (mountDelegate != null) {
      mountDelegate.unregisterMountExtension(mVisibilityExtensionState.getExtension());
    }

    mVisibilityExtensionState = null;
  }

  void enableEndToEndTestProcessing() {
    if (mEndToEndTestingExtensionState != null) {
      throw new IllegalStateException(
          "End to end test processing has already been enabled on this coordinator");
    }

    mEndToEndTestingExtensionState =
        mMountDelegateTarget.registerMountExtension(
            new EndToEndTestingExtension(mMountDelegateTarget));
  }

  void enableViewAttributes() {
    if (mViewAttributesExtensionState != null) {
      throw new IllegalStateException(
          "View attributes extension has already been enabled on this coordinator");
    }

    mViewAttributesExtensionState =
        mMountDelegateTarget.registerMountExtension(LithoViewAttributesExtension.getInstance());
  }

  void enableNestedLithoViewsExtension() {
    if (mNestedLithoViewsExtensionState != null) {
      throw new IllegalStateException(
          "Nested LithoView extension has already been enabled on this coordinator");
    }

    mNestedLithoViewsExtensionState =
        mMountDelegateTarget.registerMountExtension(new NestedLithoViewsExtension());
  }

  @VisibleForTesting
  public @Nullable ExtensionState getVisibilityExtensionState() {
    return mVisibilityExtensionState;
  }

  @VisibleForTesting
  public @Nullable ExtensionState getIncrementalMountExtensionState() {
    return mIncrementalMountExtensionState;
  }

  void clearLastMountedTreeId() {
    if (mTransitionsExtensionState != null) {
      TransitionsExtension.clearLastMountedTreeId(mTransitionsExtensionState);
    }
  }

  @Nullable
  EndToEndTestingExtension getEndToEndTestingExtension() {
    return mEndToEndTestingExtensionState != null
        ? (EndToEndTestingExtension) mEndToEndTestingExtensionState.getExtension()
        : null;
  }

  void enableTransitions() {
    if (mTransitionsExtensionState != null) {
      throw new IllegalStateException("Transitions have already been enabled on this coordinator.");
    }

    mTransitionsExtensionState =
        mMountDelegateTarget.registerMountExtension(
            TransitionsExtension.getInstance(
                (AnimationsDebug.ENABLED ? AnimationsDebug.TAG : null)));
  }

  void collectAllTransitions(LayoutState layoutState) {
    if (mTransitionsExtensionState == null) {
      return;
    }

    if (mTransitionsExtensionState != null) {
      TransitionsExtension.collectAllTransitions(mTransitionsExtensionState, layoutState);
    }
  }

  @VisibleForTesting
  void useVisibilityExtension(VisibilityMountExtension extension) {
    mVisibilityExtensionState = mMountDelegateTarget.registerMountExtension(extension);
  }

  public void enableDynamicProps() {
    if (mDynamicPropsExtensionState != null) {
      return;
    }

    mDynamicPropsExtensionState =
        mMountDelegateTarget.registerMountExtension(DynamicPropsExtension.getInstance());
  }

  @VisibleForTesting
  public @Nullable DynamicPropsManager getDynamicPropsManager() {
    if (mDynamicPropsExtensionState == null) {
      return null;
    }

    return mDynamicPropsExtensionState.getState().getDynamicPropsManager();
  }

  public void clearVisibilityItems() {
    if (mVisibilityExtensionState != null) {
      VisibilityMountExtension.clearVisibilityItems(mVisibilityExtensionState);
    }
  }

  public void registerUIDebugger(MountExtension extension) {
    if (mUIDebuggerExtension == extension) {
      return;
    }

    unregisterUIDebugger();
    mMountDelegateTarget.registerMountExtension(extension);
    mUIDebuggerExtension = extension;
  }

  public void unregisterUIDebugger() {
    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();
    if (mUIDebuggerExtension != null && mountDelegate != null) {
      mountDelegate.unregisterMountExtension(mUIDebuggerExtension);
      mUIDebuggerExtension = null;
    }
  }
}
