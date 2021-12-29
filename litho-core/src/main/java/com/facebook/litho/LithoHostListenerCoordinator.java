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
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension;
import com.facebook.rendercore.visibility.VisibilityMountExtension;
import java.util.ArrayList;
import java.util.List;

/** Helper for dispatching events to multiple MountListenerExtensions in Litho. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoHostListenerCoordinator {

  private final List<MountExtension> mMountExtensions;
  private final MountDelegateTarget mMountDelegateTarget;
  @Nullable private IncrementalMountExtension mIncrementalMountExtension;
  @Nullable private VisibilityMountExtension mVisibilityExtension;
  @Nullable private TransitionsExtension mTransitionsExtension;
  @Nullable private EndToEndTestingExtension mEndToEndTestingExtension;
  @Nullable private DynamicPropsExtension mDynamicPropsExtension;
  @Nullable private LithoViewAttributesExtension mViewAttributesExtension;
  @Nullable private NestedLithoViewsExtension mNestedLithoViewsExtension;

  public LithoHostListenerCoordinator(MountDelegateTarget mountDelegateTarget) {
    mMountExtensions = new ArrayList<>();
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

  // TODO figure out how to better enforce the input type here.
  public void beforeMount(Object input, Rect localVisibleRect) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      MountExtension hostListenerExtension = mMountExtensions.get(i);
      ExtensionState state = mMountDelegateTarget.getExtensionState(hostListenerExtension);
      if (state != null) {
        hostListenerExtension.beforeMount(state, input, localVisibleRect);
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void afterMount() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      ExtensionState state = mMountDelegateTarget.getExtensionState(mountExtension);
      if (state != null) {
        mountExtension.afterMount(state);
      }
    }

    endNotifyVisibleBoundsChangedSection();

    LithoStats.incrementComponentMountCount();
  }

  public void processVisibilityOutputs(Rect localVisibleRect, boolean isDirty) {
    startNotifyVisibleBoundsChangedSection();

    if (mVisibilityExtension != null) {
      final ExtensionState state = mMountDelegateTarget.getExtensionState(mVisibilityExtension);
      if (state != null) {
        if (isDirty) {
          mVisibilityExtension.afterMount(state);
        } else {
          mVisibilityExtension.onVisibleBoundsChanged(state, localVisibleRect);
        }
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onVisibleBoundsChanged(Rect localVisibleRect) {
    startNotifyVisibleBoundsChangedSection();

    // We first mount and then we process visibility outputs.
    if (mIncrementalMountExtension != null) {
      ExtensionState state = mMountDelegateTarget.getExtensionState(mIncrementalMountExtension);
      if (state != null) {
        mIncrementalMountExtension.onVisibleBoundsChanged(state, localVisibleRect);
        LithoStats.incrementComponentMountCount();
      }
    }

    if (mTransitionsExtension != null) {
      ExtensionState state = mMountDelegateTarget.getExtensionState(mTransitionsExtension);
      if (state != null) {
        mTransitionsExtension.onVisibleBoundsChanged(state, localVisibleRect);
      }
    }

    if (mVisibilityExtension != null) {
      ExtensionState state = mMountDelegateTarget.getExtensionState(mVisibilityExtension);
      if (state != null) {
        mVisibilityExtension.onVisibleBoundsChanged(state, localVisibleRect);
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onUnmount() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      ExtensionState state = mMountDelegateTarget.getExtensionState(mountExtension);
      if (state != null) {
        mountExtension.onUnmount(state);
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onUnbind() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      ExtensionState state = mMountDelegateTarget.getExtensionState(mountExtension);
      if (state != null) {
        mountExtension.onUnbind(state);
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  void enableIncrementalMount() {
    if (mIncrementalMountExtension != null) {
      return;
    }

    mIncrementalMountExtension = IncrementalMountExtension.getInstance();

    mMountDelegateTarget.registerMountDelegateExtension(mIncrementalMountExtension);
    registerListener(mIncrementalMountExtension);
  }

  void disableIncrementalMount() {
    if (mIncrementalMountExtension == null) {
      return;
    }
    mMountDelegateTarget.unregisterMountDelegateExtension(mIncrementalMountExtension);
    removeListener(mIncrementalMountExtension);
    mIncrementalMountExtension = null;
  }

  void enableVisibilityProcessing(LithoView lithoView) {
    if (mVisibilityExtension != null) {
      return;
    }

    mVisibilityExtension = VisibilityMountExtension.getInstance();
    mMountDelegateTarget.registerMountDelegateExtension(mVisibilityExtension);
    ExtensionState state = mMountDelegateTarget.getExtensionState(mVisibilityExtension);
    if (state != null) {
      VisibilityMountExtension.setRootHost(state, lithoView);
    }
    registerListener(mVisibilityExtension);
  }

  void disableVisibilityProcessing() {
    if (mVisibilityExtension == null) {
      return;
    }

    mMountDelegateTarget.unregisterMountDelegateExtension(mVisibilityExtension);
    removeListener(mVisibilityExtension);
    mVisibilityExtension = null;
  }

  void enableEndToEndTestProcessing() {
    if (mEndToEndTestingExtension != null) {
      throw new IllegalStateException(
          "End to end test processing has already been enabled on this coordinator");
    }

    mEndToEndTestingExtension = new EndToEndTestingExtension(mMountDelegateTarget);
    mMountDelegateTarget.registerMountDelegateExtension(mEndToEndTestingExtension);
    registerListener(mEndToEndTestingExtension);
  }

  void enableViewAttributes() {
    if (mViewAttributesExtension != null) {
      throw new IllegalStateException(
          "View attributes extension has already been enabled on this coordinator");
    }

    mViewAttributesExtension = LithoViewAttributesExtension.getInstance();
    mMountDelegateTarget.registerMountDelegateExtension(mViewAttributesExtension);
    registerListener(mViewAttributesExtension);
  }

  void enableNestedLithoViewsExtension() {
    if (mNestedLithoViewsExtension != null) {
      throw new IllegalStateException(
          "Nested LithoView extension has already been enabled on this coordinator");
    }

    mNestedLithoViewsExtension = new NestedLithoViewsExtension();
    mMountDelegateTarget.registerMountDelegateExtension(mNestedLithoViewsExtension);
    registerListener(mNestedLithoViewsExtension);
  }

  @Nullable
  VisibilityMountExtension getVisibilityExtension() {
    return mVisibilityExtension;
  }

  void clearLastMountedTreeId() {
    if (mTransitionsExtension != null) {
      ExtensionState state = mMountDelegateTarget.getExtensionState(mTransitionsExtension);
      if (state != null) {
        mTransitionsExtension.clearLastMountedTreeId(state);
      }
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
    mMountDelegateTarget.registerMountDelegateExtension(mTransitionsExtension);

    registerListener(mTransitionsExtension);
  }

  void collectAllTransitions(LayoutState layoutState) {
    if (mTransitionsExtension == null) {
      return;
    }

    startNotifyVisibleBoundsChangedSection();

    ExtensionState state = mMountDelegateTarget.getExtensionState(mTransitionsExtension);
    if (state != null) {
      mTransitionsExtension.collectAllTransitions(state, layoutState);
    }

    endNotifyVisibleBoundsChangedSection();
  }

  private void registerListener(MountExtension mountListenerExtension) {
    mMountExtensions.add(mountListenerExtension);
  }

  private void removeListener(MountExtension mountExtension) {
    mMountExtensions.remove(mountExtension);
  }

  @VisibleForTesting
  void useVisibilityExtension(VisibilityMountExtension extension) {
    mVisibilityExtension = extension;
    mMountDelegateTarget.registerMountDelegateExtension(mVisibilityExtension);
    registerListener(mVisibilityExtension);
  }

  public void enableDynamicProps() {
    if (mDynamicPropsExtension != null) {
      return;
    }

    mDynamicPropsExtension = DynamicPropsExtension.getInstance();
    mMountDelegateTarget.registerMountDelegateExtension(mDynamicPropsExtension);
    registerListener(mDynamicPropsExtension);
  }

  @Nullable
  @VisibleForTesting
  public DynamicPropsManager getDynamicPropsManager() {
    if (mDynamicPropsExtension == null) {
      return null;
    }

    final ExtensionState<DynamicPropsExtension.DynamicPropsExtensionState> state =
        mMountDelegateTarget.getExtensionState(mDynamicPropsExtension);

    return state.getState().getDynamicPropsManager();
  }
}
