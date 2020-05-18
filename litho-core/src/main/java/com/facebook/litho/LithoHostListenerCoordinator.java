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
import com.facebook.rendercore.MountDelegate.MountDelegateTarget;
import java.util.ArrayList;
import java.util.List;

/** Helper for dispatching events to multiple MountListenerExtensions in Litho. */
public class LithoHostListenerCoordinator implements HostListenerExtension<Object> {

  private final List<HostListenerExtension> mMountExtensions;
  private IncrementalMountExtension mIncrementalMountExtension;
  private VisibilityOutputsExtension mVisibilityOutputsExtension;
  private TransitionsExtension mTransitionsExtension;

  public LithoHostListenerCoordinator() {
    mMountExtensions = new ArrayList<>();
  }

  // TODO figure out how to better enforce the input type here.
  @Override
  public void beforeMount(Object input, Rect localVisibleRect) {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      HostListenerExtension hostListenerExtension = mMountExtensions.get(i);
      hostListenerExtension.beforeMount(input, localVisibleRect);
    }
  }

  @Override
  public void afterMount() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).afterMount();
    }
  }

  @Override
  public void onVisibleBoundsChanged(Rect localVisibleRect) {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).onVisibleBoundsChanged(localVisibleRect);
    }
  }

  @Override
  public void onUnmount() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).onUnmount();
    }
  }

  @Override
  public void onUnbind() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).onUnbind();
    }
  }

  void enableIncrementalMount(LithoView lithoView, MountDelegateTarget mountDelegateTarget) {
    if (mIncrementalMountExtension != null) {
      throw new IllegalStateException(
          "Incremental mount has already been enabled on this coordinator.");
    }

    mIncrementalMountExtension = new IncrementalMountExtension(lithoView);
    mountDelegateTarget.registerMountDelegateExtension(mIncrementalMountExtension);
    registerListener(mIncrementalMountExtension);
  }

  void enableVisibilityProcessing(LithoView lithoView) {
    if (mVisibilityOutputsExtension != null) {
      throw new IllegalStateException(
          "Visibility processing has already been enabled on this coordinator");
    }

    mVisibilityOutputsExtension = new VisibilityOutputsExtension(lithoView);
    registerListener(mVisibilityOutputsExtension);
  }

  @Nullable
  VisibilityOutputsExtension getVisibilityOutputsExtension() {
    return mVisibilityOutputsExtension;
  }

  void enableTransitions(LithoView lithoView, MountDelegateTarget mountDelegateTarget) {
    if (mTransitionsExtension != null) {
      throw new IllegalStateException("Transitions have already been enabled on this coordinator.");
    }

    mTransitionsExtension = new TransitionsExtension(lithoView);
    mountDelegateTarget.registerMountDelegateExtension(mTransitionsExtension);
    registerListener(mTransitionsExtension);
  }

  void collectAllTransitions(LayoutState layoutState, ComponentTree componentTree) {
    if (mTransitionsExtension == null) {
      return;
    }

    mTransitionsExtension.collectAllTransitions(layoutState, componentTree);
  }

  private void registerListener(HostListenerExtension mountListenerExtension) {
    mMountExtensions.add(mountListenerExtension);
  }
}
