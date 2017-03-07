// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing.testrunner;

import java.lang.reflect.Method;

import android.app.Application;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

public class ComponentsTestRunner extends RobolectricTestRunner {
  /**
   * Creates a runner to run {@code testClass}. Looks in your working directory for your
   * AndroidManifest.xml file and res directory by default. Use the {@link Config} annotation to
   * configure.
   *
   * @param testClass the test class to be run
   * @throws InitializationError if junit says so
   */
  public ComponentsTestRunner(final Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  public Config getConfig(final Method method) {
    final Config config = super.getConfig(method);
    // We are hard-coding the path here instead of relying on BUCK internals
    // to allow for building with gradle in the Open Source version.
    return new Config.Implementation(config, new Config.Implementation(
        new int[]{},
        // TODO(16485772): The path should not include the `libraries` prefix as this will
        // break running tests from the GH repo.
        "libraries/components/src/test/java/com/facebook/components/AndroidManifest.xml",
        "",
        "",
        "res",
        "assets",
        new Class[]{},
        Application.class,
        new String[0],
        null));
  }
}
