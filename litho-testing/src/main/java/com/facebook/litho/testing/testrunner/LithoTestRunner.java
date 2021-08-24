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

import androidx.annotation.Nullable;
import com.facebook.litho.ComponentsSystrace;
import com.facebook.litho.config.ComponentsConfiguration;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.robolectric.DefaultTestLifecycle;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.TestLifecycle;
import org.robolectric.annotation.Config;
import org.robolectric.internal.bytecode.Sandbox;
import org.robolectric.util.ReflectionHelpers;

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

  /**
   * @return a list of LithoTestRunConfiguration that the test suite should also be run with. The
   *     test suite will always be run without any extra configurations in addition to these run
   *     configurations.
   */
  private List<? extends Class<? extends LithoTestRunConfiguration>> getExtraRunConfigurations() {
    return Arrays.asList(StatelessTestRunConfiguration.class);
  }

  @Override
  protected List<FrameworkMethod> getChildren() {
    final List<FrameworkMethod> children = super.getChildren();
    final List<? extends Class<? extends LithoTestRunConfiguration>> extraRunConfigurations =
        getExtraRunConfigurations();
    if (extraRunConfigurations.isEmpty()) {
      return children;
    }

    // Add instances of the method to run for each extra run configuration we have
    final ArrayList<FrameworkMethod> res = new ArrayList<>();
    for (FrameworkMethod method : children) {
      res.add(method);
      for (Class<? extends LithoTestRunConfiguration> configuration : extraRunConfigurations) {
        res.add(
            new LithoRobolectricFrameworkMethod(
                (RobolectricFrameworkMethod) method, configuration));
      }
    }

    return res;
  }

  @SuppressWarnings("CatchGeneralException")
  @Override
  protected void beforeTest(Sandbox sandbox, FrameworkMethod method, Method bootstrappedMethod)
      throws Throwable {
    super.beforeTest(sandbox, method, bootstrappedMethod);

    // The reason we accept LithoTestRunConfigurations as classes is because we need to load them in
    // the Sandbox class loader - otherwise they will only be able to modify the
    // ComponentsConfiguration loaded in the 'app' classloader, meaning changes to it won't be seen
    // within tests.
    if (method instanceof LithoRobolectricFrameworkMethod) {
      LithoRobolectricFrameworkMethod lithoMethod = (LithoRobolectricFrameworkMethod) method;
      lithoMethod.sandbox = sandbox;
      lithoMethod.configurationInstance =
          ReflectionHelpers.newInstance(
              sandbox.<LithoTestRunConfiguration>bootstrappedClass(lithoMethod.configurationClass));

      // This hack is because LithoTestRunConfiguration is loaded in both the main 'app' classloader
      // and in the sandbox classloader the test runs with. Normally, we would exclude
      // LithoTestRunConfiguration by overriding createClassLoaderConfig and in this class using
      // doNotAcquireClass, however doing leads to the dreaded "Cannot load NodeConfig" error due
      // to the change in classloader config, thus we hack around it with reflection instead.
      Class<LithoTestRunner> testRunnerClass =
          lithoMethod.sandbox.bootstrappedClass(LithoTestRunConfiguration.class);
      try {
        testRunnerClass
            .getMethod("beforeTest", FrameworkMethod.class)
            .invoke(lithoMethod.configurationInstance, method);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @SuppressWarnings("CatchGeneralException")
  @Override
  protected void afterTest(FrameworkMethod method, Method bootstrappedMethod) {
    if (method instanceof LithoRobolectricFrameworkMethod) {
      LithoRobolectricFrameworkMethod lithoMethod = (LithoRobolectricFrameworkMethod) method;
      Class<LithoTestRunner> testRunnerClass =
          lithoMethod.sandbox.bootstrappedClass(LithoTestRunConfiguration.class);

      // See comment in beforeTest above
      try {
        testRunnerClass
            .getMethod("afterTest", FrameworkMethod.class)
            .invoke(lithoMethod.configurationInstance, method);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      lithoMethod.sandbox = null;
      lithoMethod.configurationInstance = null;
    }
    super.afterTest(method, bootstrappedMethod);
  }

  @Override
  protected Class<? extends TestLifecycle> getTestLifecycleClass() {
    return LithoTestLifecycle.class;
  }

  private static class LithoRobolectricFrameworkMethod extends RobolectricFrameworkMethod {

    public final Class<? extends LithoTestRunConfiguration> configurationClass;

    // This is meant to be a LithoTestRunConfiguration instance - it isn't because the
    // LithoTestRunConfiguration that will be the parent class of this instance is from a different
    // class loader than LithoRobolectricFrameworkMethod will be loaded in.
    @Nullable Object configurationInstance;
    @Nullable Sandbox sandbox;

    protected LithoRobolectricFrameworkMethod(
        RobolectricFrameworkMethod other,
        Class<? extends LithoTestRunConfiguration> configurationClass) {
      super(other);
      this.configurationClass = configurationClass;
    }

    @SuppressWarnings("ReflectionMethodUse")
    @Override
    public String getName() {
      return super.getName() + "[" + configurationClass.getSimpleName() + "]";
    }
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
