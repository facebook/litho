// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho.testing.testrunner

import com.facebook.litho.config.ComponentsConfiguration
import org.junit.runners.model.FrameworkMethod

class ComponentTreePropContainerConfiguration : LithoTestRunConfiguration {

  override fun beforeTest(method: FrameworkMethod) {
    ComponentsConfiguration.defaultInstance =
        ComponentsConfiguration.defaultInstance.copy(
            useComponentTreePropContainerAsSourceOfTruth = true)
  }

  override fun afterTest(method: FrameworkMethod) {
    ComponentsConfiguration.defaultInstance =
        ComponentsConfiguration.defaultInstance.copy(
            useComponentTreePropContainerAsSourceOfTruth = true)
  }
}
