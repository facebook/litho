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
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import com.facebook.rendercore.visibility.VisibilityMountExtension;
import com.facebook.rendercore.visibility.VisibilityMountExtension.VisibilityMountExtensionState;

/** Helper for dispatching events to multiple MountListenerExtensions in Litho. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoHostListenerCoordinator {

  private final MountDelegateTarget mMountDelegateTarget;
  @Nullable private IncrementalMountExtension mIncrementalMountExtension;
  @Nullable private VisibilityMountExtension mVisibilityExtension;
  @Nullable private TransitionsExtension mTransitionsExtension;
  @Nullable private EndToEndTestingExtension mEndToEndTestingExtension;
  @Nullable private DynamicPropsExtension mDynamicPropsExtension;
  @Nullable private LithoViewAttributesExtension mViewAttributesExtension;
  @Nullable private NestedLithoViewsExtension mNestedLithoViewsExtension;

  private @Nullable ExtensionState<LithoViewAttributesState> mViewAttributesExtensionState;
  private @Nullable ExtensionState<Void> mNestedLithoViewsExtensionState;
  private @Nullable ExtensionState<DynamicPropsExtensionState> mDynamicPropsExtensionState;
  private @Nullable ExtensionState<VisibilityMountExtensionState> mVisibilityExtensionState;
  private @Nullable ExtensionState<TransitionsExtensionState> mTransitionsExtensionState;
  private @Nullable ExtensionState<IncrementalMountExtensionState> mIncrementalMountExtensionState;
  private @Nullable ExtensionState<Void> mEndToEndTestingExtensionState;

  public LithoHostListenerCoordinator(MountDelegateTarget mountDelegateTarget) {
    mMountDelegateTarget = mountDelegateTarget;
  }

  public void setCollectNotifyVisibleBoundsChangedCalls(boolean value) {
    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();

    if (mountDelegate != null) {
      mountDelegate.setCollectVisibleBoundsChangedCalls(value);
    }
  }

  public void setSkipNotifyVisibleBoundsChanged(boolean value) {
    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();

    if (mountDelegate != null) {
      mountDelegate.setSkipNotifyVisibleBoundsChanged(value);
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

    if (mNestedLithoViewsExtensionState != null && mNestedLithoViewsExtension != null) {
      mNestedLithoViewsExtension.beforeMount(
          mNestedLithoViewsExtensionState, null, localVisibleRect);
    }

    if (mTransitionsExtension != null && mTransitionsExtensionState != null) {
      mTransitionsExtension.beforeMount(mTransitionsExtensionState, input, localVisibleRect);
    }

    if (mEndToEndTestingExtension != null && mEndToEndTestingExtensionState != null) {
      mEndToEndTestingExtension.beforeMount(
          mEndToEndTestingExtensionState, input, localVisibleRect);
    }

    if (mViewAttributesExtensionState != null && mViewAttributesExtension != null) {
      mViewAttributesExtension.beforeMount(mViewAttributesExtensionState, null, localVisibleRect);
    }

    if (mDynamicPropsExtensionState != null && mDynamicPropsExtension != null) {
      mDynamicPropsExtension.beforeMount(mDynamicPropsExtensionState, null, localVisibleRect);
    }

    if (mVisibilityExtension != null && mVisibilityExtensionState != null) {
      mVisibilityExtension.beforeMount(mVisibilityExtensionState, input, localVisibleRect);
    }

    if (mIncrementalMountExtension != null && mIncrementalMountExtensionState != null) {
      mIncrementalMountExtension.beforeMount(
          mIncrementalMountExtensionState, input, localVisibleRect);
    }
  }

  public void processVisibilityOutputs(Rect localVisibleRect, boolean isDirty) {
    startNotifyVisibleBoundsChangedSection();

    if (mVisibilityExtension != null && mVisibilityExtensionState != null) {
      if (isDirty) {
        mVisibilityExtension.afterMount(mVisibilityExtensionState);
      } else {
        mVisibilityExtension.onVisibleBoundsChanged(mVisibilityExtensionState, localVisibleRect);
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  void enableIncrementalMount() {
    if (mIncrementalMountExtension != null) {
      return;
    }

    mIncrementalMountExtension = IncrementalMountExtension.getInstance();

    mIncrementalMountExtensionState =
        mMountDelegateTarget.registerMountExtension(mIncrementalMountExtension);
  }

  void disableIncrementalMount() {
    if (mIncrementalMountExtension == null) {
      return;
    }

    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();
    if (mountDelegate != null) {
      mountDelegate.unregisterMountExtension(mIncrementalMountExtension);
    }

    mIncrementalMountExtension = null;
    mIncrementalMountExtensionState = null;
  }

  void enableVisibilityProcessing(LithoView lithoView) {
    if (mVisibilityExtension != null) {
      return;
    }

    mVisibilityExtension = VisibilityMountExtension.getInstance();
    mVisibilityExtensionState = mMountDelegateTarget.registerMountExtension(mVisibilityExtension);
    if (mVisibilityExtensionState != null) {
      VisibilityMountExtension.setRootHost(mVisibilityExtensionState, lithoView);
    }
  }

  void disableVisibilityProcessing() {
    if (mVisibilityExtension == null) {
      return;
    }

    final MountDelegate mountDelegate = mMountDelegateTarget.getMountDelegate();
    if (mountDelegate != null) {
      mountDelegate.unregisterMountExtension(mVisibilityExtension);
    }

    mVisibilityExtension = null;
    mVisibilityExtensionState = null;
  }

  void enableEndToEndTestProcessing() {
    if (mEndToEndTestingExtension != null) {
      throw new IllegalStateException(
          "End to end test processing has already been enabled on this coordinator");
    }

    mEndToEndTestingExtension = new EndToEndTestingExtension(mMountDelegateTarget);
    mEndToEndTestingExtensionState =
        mMountDelegateTarget.registerMountExtension(mEndToEndTestingExtension);
  }

  void enableViewAttributes() {
    if (mViewAttributesExtension != null) {
      throw new IllegalStateException(
          "View attributes extension has already been enabled on this coordinator");
    }

    mViewAttributesExtension = LithoViewAttributesExtension.getInstance();
    mMountDelegateTarget.registerMountExtension(mViewAttributesExtension);
  }

  void enableNestedLithoViewsExtension() {
    if (mNestedLithoViewsExtension != null) {
      throw new IllegalStateException(
          "Nested LithoView extension has already been enabled on this coordinator");
    }

    mNestedLithoViewsExtension = new NestedLithoViewsExtension();
    mMountDelegateTarget.registerMountExtension(mNestedLithoViewsExtension);
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
    if (mTransitionsExtension != null && mTransitionsExtensionState != null) {
      mTransitionsExtension.clearLastMountedTreeId(mTransitionsExtensionState);
    }
  }

  @Nullable
  EndToEndTestingExtension getEndToEndTestingExtension() {
    return mEndToEndTestingExtension;
  }

  void enableTransitions() {
    if (mTransitionsExtension != null) {
      throw new IllegalStateException("Transitions have already been enabled on this coordinator.");
    }

    mTransitionsExtension =
        TransitionsExtension.getInstance(
            true, (AnimationsDebug.ENABLED ? AnimationsDebug.TAG : null));
    mTransitionsExtensionState = mMountDelegateTarget.registerMountExtension(mTransitionsExtension);
  }

  void collectAllTransitions(LayoutState layoutState) {
    if (mTransitionsExtension == null) {
      return;
    }

    if (mTransitionsExtension != null && mTransitionsExtensionState != null) {
      mTransitionsExtension.collectAllTransitions(mTransitionsExtensionState, layoutState);
    }
  }

  @VisibleForTesting
  void useVisibilityExtension(VisibilityMountExtension extension) {
    mVisibilityExtension = extension;
    mVisibilityExtensionState = mMountDelegateTarget.registerMountExtension(mVisibilityExtension);
  }

  public void enableDynamicProps() {
    if (mDynamicPropsExtension != null) {
      return;
    }

    mDynamicPropsExtension = DynamicPropsExtension.getInstance();
    mDynamicPropsExtensionState =
        mMountDelegateTarget.registerMountExtension(mDynamicPropsExtension);
  }

  @Nullable
  @VisibleForTesting
  public DynamicPropsManager getDynamicPropsManager() {
    if (mDynamicPropsExtension == null || mDynamicPropsExtensionState == null) {
      return null;
    }

    return mDynamicPropsExtensionState.getState().getDynamicPropsManager();
  }

  public void clearVisibilityItems() {
    if (mVisibilityExtension != null && mVisibilityExtensionState != null) {
      VisibilityMountExtension.clearVisibilityItems(mVisibilityExtensionState);
    }
  }
}
