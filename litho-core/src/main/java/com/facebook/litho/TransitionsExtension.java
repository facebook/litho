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

import com.facebook.rendercore.MountDelegate.MountDelegateInput;
import com.facebook.rendercore.MountDelegateExtension;

/** Extension for performing transitions. */
public class TransitionsExtension extends MountDelegateExtension
    implements HostListenerExtension<TransitionsExtension.TransitionsExtensionInput> {

  private final Host mLithoView;
  private TransitionsExtensionInput mInput;

  public interface TransitionsExtensionInput extends MountDelegateInput {}

  public TransitionsExtension(Host lithoView) {
    mLithoView = lithoView;
  }

  @Override
  public void beforeMount(TransitionsExtensionInput input) {
    mInput = input;
  }

  @Override
  public void onViewOffset() {}

  @Override
  public void onUnmount() {}

  @Override
  public void onUnbind() {}

  @Override
  public void onHostVisibilityChanged(boolean isVisible) {}
}
