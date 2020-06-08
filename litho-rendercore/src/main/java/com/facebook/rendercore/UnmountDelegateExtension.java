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

package com.facebook.rendercore;

/** This delegate allows to overtake the unmount responsibility of a {@link MountItem}. */
public interface UnmountDelegateExtension {

  /**
   * This method is called to check if this item unmount needs to be delegated.
   *
   * @param mountItem
   * @return
   */
  boolean shouldDelegateUnmount(MountItem mountItem);

  /**
   * This method is responsable of unmounting the item from the {@link Host} and unbinding the item
   * from the {@link MountDelegate.MountDelegateTarget}.
   *
   * @param index
   * @param mountItem
   * @param host
   */
  void unmount(int index, MountItem mountItem, Host host);
}
