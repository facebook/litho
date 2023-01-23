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

import com.facebook.infer.annotation.ThreadConfined;
import java.util.concurrent.atomic.AtomicReference;

/** Represents a pointer to the Tree that a ComponentContext is attached to */
public class LithoTree {

  @ThreadConfined(ThreadConfined.ANY)
  private final StateUpdater mStateUpdater;

  @ThreadConfined(ThreadConfined.UI)
  private final MountedViewReference mMountedViewReference;

  private final ErrorComponentReceiver mErrorComponentReceiver;
  private final LithoTreeLifecycleProvider mLithoTreeLifecycleProvider;

  // Used to lazily store a CoroutineScope, if coroutine helper methods are used.
  final AtomicReference<Object> mInternalScopeRef = new AtomicReference<>();

  public LithoTree(
      StateUpdater stateUpdater,
      MountedViewReference mountedViewReference,
      ErrorComponentReceiver errorComponentReceiver,
      LithoTreeLifecycleProvider lifecycleProvider) {
    mStateUpdater = stateUpdater;
    mMountedViewReference = mountedViewReference;
    mErrorComponentReceiver = errorComponentReceiver;
    mLithoTreeLifecycleProvider = lifecycleProvider;
  }

  public StateUpdater getStateUpdater() {
    return mStateUpdater;
  }

  public MountedViewReference getMountedViewReference() {
    return mMountedViewReference;
  }

  public ErrorComponentReceiver getErrorComponentReceiver() {
    return mErrorComponentReceiver;
  }

  public LithoTreeLifecycleProvider getLithoTreeLifecycleProvider() {
    return mLithoTreeLifecycleProvider;
  }
}
