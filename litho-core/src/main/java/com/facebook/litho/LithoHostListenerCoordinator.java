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

import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.MountDelegateTarget;
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

  public LithoHostListenerCoordinator(MountDelegateTarget mountDelegateTarget) {
    mMountExtensions = new ArrayList<>();
    mMountDelegateTarget = mountDelegateTarget;
  }

  // TODO figure out how to better enforce the input type here.
  public void beforeMount(Object input, Rect localVisibleRect) {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      MountExtension hostListenerExtension = mMountExtensions.get(i);
      hostListenerExtension.beforeMount(
          mMountDelegateTarget.getExtensionState(hostListenerExtension), input, localVisibleRect);
    }
  }

  public void afterMount() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      mountExtension.afterMount(mMountDelegateTarget.getExtensionState(mountExtension));
    }
  }

  public void processVisibilityOutputs(Rect localVisibleRect) {
    if (mVisibilityExtension != null) {
      mVisibilityExtension.onVisibleBoundsChanged(
          mMountDelegateTarget.getExtensionState(mVisibilityExtension), localVisibleRect);
    }
  }

  public void onVisibleBoundsChanged(Rect localVisibleRect) {
    // We first mount and then we process visibility outputs.
    if (mIncrementalMountExtension != null) {
      mIncrementalMountExtension.onVisibleBoundsChanged(
          mMountDelegateTarget.getExtensionState(mIncrementalMountExtension), localVisibleRect);
      LithoStats.incrementComponentMountCount();
    }

    if (mTransitionsExtension != null) {
      mTransitionsExtension.onVisibleBoundsChanged(
          mMountDelegateTarget.getExtensionState(mTransitionsExtension), localVisibleRect);
    }

    if (mVisibilityExtension != null) {
      mVisibilityExtension.onVisibleBoundsChanged(
          mMountDelegateTarget.getExtensionState(mVisibilityExtension), localVisibleRect);
    }
  }

  public void onUnmount() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      mountExtension.onUnmount(mMountDelegateTarget.getExtensionState(mountExtension));
    }
  }

  public void onUnbind() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      mountExtension.onUnbind(mMountDelegateTarget.getExtensionState(mountExtension));
    }
  }

  void enableIncrementalMount(LithoView lithoView, MountDelegateTarget mountDelegateTarget) {
    if (mIncrementalMountExtension != null) {
      throw new IllegalStateException(
          "Incremental mount has already been enabled on this coordinator.");
    }

    mIncrementalMountExtension = IncrementalMountExtension.getInstance();

    mountDelegateTarget.registerMountDelegateExtension(mIncrementalMountExtension);
    registerListener(mIncrementalMountExtension);
  }

  void enableVisibilityProcessing(LithoView lithoView, MountDelegateTarget mountDelegateTarget) {
    if (mVisibilityExtension != null) {
      throw new IllegalStateException(
          "Visibility processing has already been enabled on this coordinator");
    }

    mVisibilityExtension = VisibilityMountExtension.getInstance();
    mountDelegateTarget.registerMountDelegateExtension(mVisibilityExtension);
    VisibilityMountExtension.setRootHost(
        mMountDelegateTarget.getExtensionState(mVisibilityExtension), lithoView);
    registerListener(mVisibilityExtension);
  }

  void enableEndToEndTestProcessing(MountDelegateTarget mountDelegateTarget) {
    if (mEndToEndTestingExtension != null) {
      throw new IllegalStateException(
          "End to end test processing has already been enabled on this coordinator");
    }

    mEndToEndTestingExtension = new EndToEndTestingExtension(mountDelegateTarget);
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

  @Nullable
  VisibilityMountExtension getVisibilityExtension() {
    return mVisibilityExtension;
  }

  @Nullable
  TransitionsExtension getTransitionsExtension() {
    return mTransitionsExtension;
  }

  void clearLastMountedTreeId() {
    if (mTransitionsExtension != null) {
      mTransitionsExtension.clearLastMountedTreeId(
          mMountDelegateTarget.getExtensionState(mTransitionsExtension));
    }
  }

  @Nullable
  EndToEndTestingExtension getEndToEndTestingExtension() {
    return mEndToEndTestingExtension;
  }

  void enableTransitions(LithoView lithoView, MountDelegateTarget mountDelegateTarget) {
    if (mTransitionsExtension != null) {
      throw new IllegalStateException("Transitions have already been enabled on this coordinator.");
    }

    mTransitionsExtension = TransitionsExtension.getInstance();
    mountDelegateTarget.registerMountDelegateExtension(mTransitionsExtension);

    registerListener(mTransitionsExtension);
  }

  void collectAllTransitions(LayoutState layoutState) {
    if (mTransitionsExtension == null) {
      return;
    }

    mTransitionsExtension.collectAllTransitions(
        mMountDelegateTarget.getExtensionState(mTransitionsExtension), layoutState);
  }

  private void registerListener(MountExtension mountListenerExtension) {
    mMountExtensions.add(mountListenerExtension);
  }

  @VisibleForTesting
  void useVisibilityExtension(VisibilityMountExtension extension, LithoView lithoView) {
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
}
