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

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Temporary internal settings augmenting {@link com.facebook.litho.config.ComponentsConfiguration}.
 * This only exists to circumvent some circular dependency issues that arise by placing this in the
 * <code>.config</code> namespace. Please be aware that this will go away soon. (T30053822)
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class ErrorBoundariesConfiguration {
  private ErrorBoundariesConfiguration() {}

  /**
   * Optional top-level component that sits at the root of every {@link ComponentTree}. This can be
   * used to wrap all components in error boundaries if desired. Bear in mind that this is not the
   * final API for using this feature and will change in the future.
   */
  @Nullable public static RootWrapperComponentFactory rootWrapperComponentFactory = null;
}
