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

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface ShouldUpdate {

  /**
   * <b>Note:</b> This should only be set in the context of {@link MountSpec}. Will be ignored for
   * {@link LayoutSpec} types.
   *
   * @return If this is true and this MountSpec is pureRender the mount process will check
   *     shouldComponentUpdate before unmounting/mounting in place and only update the content if
   *     necessary. If this is false instead, the mount process will only rely on the information
   *     provided by the layout process. As a rule of thumb this should only be set to true when for
   *     a Component the cost of calling Mount/Unmount greatly exceeds the cost of calling
   *     ShouldUpdate.
   * @deprecated this param is ignored. MountSpecs with pureRender will now always check
   *     shouldUpdate on the main thread if the information from layout isn't able to be used.
   */
  boolean onMount() default false;
}
