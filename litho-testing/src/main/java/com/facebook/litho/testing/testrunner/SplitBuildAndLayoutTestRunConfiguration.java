// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho.testing.testrunner;

import com.facebook.litho.config.ComponentsConfiguration;
import org.junit.runners.model.FrameworkMethod;

public class SplitBuildAndLayoutTestRunConfiguration implements LithoTestRunConfiguration {

  private final boolean defaultIsBuildAndLayoutSplitEnabled =
      ComponentsConfiguration.isBuildAndLayoutSplitEnabled;

  @Override
  public void beforeTest(FrameworkMethod method) {
    ComponentsConfiguration.isBuildAndLayoutSplitEnabled = true;
  }

  @Override
  public void afterTest(FrameworkMethod method) {
    ComponentsConfiguration.isBuildAndLayoutSplitEnabled = defaultIsBuildAndLayoutSplitEnabled;
  }
}
