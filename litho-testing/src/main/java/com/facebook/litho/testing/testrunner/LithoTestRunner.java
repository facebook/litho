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

package com.facebook.litho.testing.testrunner;

import com.facebook.litho.ComponentsSystrace;
import com.facebook.litho.config.ComponentsConfiguration;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.junit.runners.model.InitializationError;
import org.robolectric.DefaultTestLifecycle;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.TestLifecycle;
import org.robolectric.annotation.Config;

public class LithoTestRunner extends RobolectricTestRunner {
  /**
   * Creates a runner to run {@code testClass}. Looks in your working directory for your
   * AndroidManifest.xml file and res directory by default. Use the {@link Config} annotation to
   * configure.
   *
   * @param testClass the test class to be run
   * @throws InitializationError if junit says so
   */
  public LithoTestRunner(final Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  protected Config buildGlobalConfig() {
    // If we're running with gradle, the test runner will start running from within
    // the given sub-project.
    if (System.getProperty("org.gradle.test.worker") != null) {
      return new Config.Builder().setManifest("../litho-it/src/main/AndroidManifest.xml").build();
    }

    // BUCK will set up the manifest correctly, so nothing to do here.
    return super.buildGlobalConfig();
  }

  @Override
  protected Class<? extends TestLifecycle> getTestLifecycleClass() {
    return LithoTestLifecycle.class;
  }

  public static class LithoTestLifecycle extends DefaultTestLifecycle {

    private final Map<Field, Object> mSavedComponentsConfiguration;

    public LithoTestLifecycle() {
      ComponentsSystrace.provide(NoOpComponentsSystrace.sInstance);
      mSavedComponentsConfiguration = recordComponentsConfiguration();
    }

    @Override
    public void afterTest(Method method) {
      super.afterTest(method);
      restoreComponentsConfiguration(mSavedComponentsConfiguration);
    }

    private static Map<Field, Object> recordComponentsConfiguration() {
      final HashMap<Field, Object> record = new HashMap<>();
      for (Field f : ComponentsConfiguration.class.getDeclaredFields()) {
        final int modifiers = f.getModifiers();
        if (Modifier.isStatic(modifiers)
            && Modifier.isPublic(modifiers)
            && !Modifier.isFinal(modifiers)) {
          try {
            record.put(f, f.get(null));
          } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't record ComponentsConfiguration state", e);
          }
        }
      }
      return record;
    }

    private static void restoreComponentsConfiguration(Map<Field, Object> fieldsToValues) {
      for (Map.Entry<Field, Object> entry : fieldsToValues.entrySet()) {
        try {
          entry.getKey().set(null, entry.getValue());
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Couldn't restore ComponentsConfiguration state", e);
        }
      }
    }
  }
}
