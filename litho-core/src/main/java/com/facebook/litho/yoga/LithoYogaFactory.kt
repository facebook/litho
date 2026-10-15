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

package com.facebook.litho.yoga

import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.yoga.YogaConfig
import com.facebook.yoga.YogaConfigFactory
import com.facebook.yoga.YogaErrata
import com.facebook.yoga.YogaExperimentalFeature
import com.facebook.yoga.YogaNode
import com.facebook.yoga.YogaNodeFactory

object LithoYogaFactory {

  @JvmStatic
  fun createYogaConfig(): YogaConfig {
    val yogaConfig = YogaConfigFactory.create()
    yogaConfig.setUseWebDefaults(true)
    yogaConfig.setExperimentalFeatureEnabled(YogaExperimentalFeature.WEB_FLEX_BASIS, true)
    // CLASSIC sets every legacy errata bit, including `MinSizeUndefinedInsteadOfAuto`. Clear
    // that bit when `useAutoMinSize` is enabled so the CSS Flexbox §4.5 automatic minimum sizing
    // probe fires for every Litho tree. Uses the raw-int `setErrata` overload because the
    // YogaErrata enum cannot represent arbitrary bitmask combinations.
    if (ComponentsConfiguration.useAutoMinSize) {
      yogaConfig.setErrata(
          YogaErrata.CLASSIC.intValue() and
              YogaErrata.MIN_SIZE_UNDEFINED_INSTEAD_OF_AUTO.intValue().inv(),
      )
    } else {
      yogaConfig.setErrata(YogaErrata.CLASSIC)
    }
    return yogaConfig
  }

  @JvmStatic fun createYogaNode(config: YogaConfig): YogaNode = YogaNodeFactory.create(config)
}
