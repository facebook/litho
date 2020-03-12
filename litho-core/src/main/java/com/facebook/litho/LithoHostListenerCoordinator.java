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

import java.util.ArrayList;
import java.util.List;

/** Helper for dispatching events to multiple MountListenerExtensions in Litho. */
public class LithoHostListenerCoordinator implements HostListenerExtension<Object> {

  private final List<HostListenerExtension> mMountExtensions;

  public LithoHostListenerCoordinator() {
    mMountExtensions = new ArrayList<>();
  }

  // TODO figure out how to better enforce the input type here.
  @Override
  public void beforeMount(Object input) {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).beforeMount(input);
    }
  }

  @Override
  public void onViewOffset() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).onViewOffset();
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

  @Override
  public void onHostVisibilityChanged(boolean isVisible) {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).onHostVisibilityChanged(isVisible);
    }
  }

  private void registerListener(HostListenerExtension mountListenerExtension) {
    mMountExtensions.add(mountListenerExtension);
  }
}
