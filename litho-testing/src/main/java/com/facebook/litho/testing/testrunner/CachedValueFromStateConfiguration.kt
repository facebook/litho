// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho.testing.testrunner

import com.facebook.litho.config.ComponentsConfiguration
import org.junit.runners.model.FrameworkMethod

class CachedValueFromStateConfiguration : LithoTestRunConfiguration {

  override fun beforeTest(method: FrameworkMethod) {
    ComponentsConfiguration.defaultInstance =
        ComponentsConfiguration.defaultInstance.copy(useStateForCachedValues = true)
  }

  override fun afterTest(method: FrameworkMethod) {
    ComponentsConfiguration.defaultInstance =
        ComponentsConfiguration.defaultInstance.copy(useStateForCachedValues = false)
  }
}
