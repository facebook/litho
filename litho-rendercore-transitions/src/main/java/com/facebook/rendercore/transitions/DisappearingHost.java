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

package com.facebook.rendercore.transitions;

import com.facebook.rendercore.MountItem;

/**
 * This interface is needed to add specific methods to a {@link com.facebook.rendercore.Host} that
 * would allow for the extension to handle disappearing animations.
 */
public interface DisappearingHost {

  /**
   * This method should remove any reference to the mounted item but keep it added to the host to be
   * drawn.
   *
   * @param mountItem
   */
  void startDisappearingMountItem(MountItem mountItem);

  /**
   * In this method we finally remove the mountItem from the drawing pass.
   *
   * @param mountItem
   */
  void finaliseDisappearingItem(MountItem mountItem);
}
