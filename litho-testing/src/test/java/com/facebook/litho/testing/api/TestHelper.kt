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

package com.facebook.litho.testing.api

import com.facebook.litho.ComponentContext
import com.facebook.litho.widget.ExperimentalRecycler
import com.facebook.litho.widget.Recycler

/**
 * Returns the name of the recycler component that is currently in use. This will depend on the
 * [com.facebook.litho.ComponentsConfiguration#primitiveRecyclerEnabled]
 */
internal fun recyclerComponentName(componentContext: ComponentContext): String {
  val recyclerInUse =
      if (componentContext.lithoConfiguration.componentsConfig.primitiveRecyclerBinderStrategy !=
          null) {
        ExperimentalRecycler::class.java
      } else {
        Recycler::class.java
      }
  return recyclerInUse.simpleName
}
