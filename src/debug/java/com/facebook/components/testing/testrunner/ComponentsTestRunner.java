/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.testrunner;

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

  enum ProjectEnvironment {
    INTERNAL, OSS;

    static ProjectEnvironment detectFromSystemProperties() {
      final String property = System.getProperty("com.facebook.litho.is_oss");
      // If this isn't set, you're probably not running Buck, ergo this isn't an internal build.
      if (property == null) {
        return OSS;
      }

      return property.equals("true") ? OSS : INTERNAL;
    }
  }

  private static String getAndroidManifestPath() {
    String prefix = "";
    switch (ProjectEnvironment.detectFromSystemProperties()) {
      case OSS:
        break;
      case INTERNAL:
        prefix = "libraries/components/";
        break;
    }

    return prefix +
        "src/test/java/com/facebook/components/AndroidManifest.xml";
  }

  @Override
  public Config getConfig(final Method method) {
    final Config config = super.getConfig(method);
    // We are hard-coding the path here instead of relying on BUCK internals
    // to allow for building with gradle in the Open Source version.
    return new Config.Implementation(config, new Config.Implementation(
        new int[]{},
        getAndroidManifestPath(),
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
