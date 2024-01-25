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

package com.facebook.litho.config

import com.facebook.rendercore.RunnableHandler

/**
 * This interface defines how ComponentTree should handle pre-allocation specs. It triggers an
 * extra-step post layout, which verifies what mount content is associated with the calculated
 * LayoutState and pushes it into the corresponding MountItemPool if it can.
 */
sealed interface PreAllocationHandler {

  /**
   * Litho will run the default pre-allocation in the same Thread that is used to run the Render
   * pipeline, which is a background Thread.
   */
  object LayoutThread : PreAllocationHandler

  /** Litho will run the pre-allocation in the Handler that is explicitly given. */
  class Custom(val handler: RunnableHandler) : PreAllocationHandler
}
