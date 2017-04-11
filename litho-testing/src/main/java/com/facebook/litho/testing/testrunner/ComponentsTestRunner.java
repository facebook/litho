/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.testrunner;

import android.app.Application;
import com.facebook.litho.testing.shadows.ColorDrawableShadow;
import com.facebook.litho.testing.shadows.LayoutDirectionViewGroupShadow;
import com.facebook.litho.testing.shadows.LayoutDirectionViewShadow;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

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

  private static Class<?>[] getDefaultShadows() {
    return new Class[]{
            ColorDrawableShadow.class,
            LayoutDirectionViewShadow.class,
            LayoutDirectionViewGroupShadow.class,
    };
  }

  private static String getResPrefix() {
    String prefix = "";
    switch (ProjectEnvironment.detectFromSystemProperties()) {
      case OSS:
        break;
      case INTERNAL:
        prefix = "libraries/components/";
        break;
    }

    // If we're running with gradle, the test runner will start running from within
    // the given sub-project.
    if (System.getProperty("org.gradle.test.worker") != null) {
      return "../litho-it/src/main/";
    } else {
      return prefix + "litho-it/src/main/";
    }
  }

  private static String getAndroidManifestPath() {
    return getResPrefix() + "AndroidManifest.xml";
  }

  @Override
  public Config getConfig(final Method method) {
    final Config config = super.getConfig(method);
    // We are hard-coding the path here instead of relying on BUCK internals
    // to allow for building with gradle in the Open Source version.
    return new Config.Implementation(config, new Config.Implementation(
        new int[]{},
        getResPrefix() + "AndroidManifest.xml",
        "",
        "",
        "res",
        "assets",
        getDefaultShadows(),
        Application.class,
        new String[0],
        null));
  }
}
