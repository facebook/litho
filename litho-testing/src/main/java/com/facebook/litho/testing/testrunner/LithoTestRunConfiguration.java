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

import org.junit.runners.model.FrameworkMethod;

/**
 * A run configuration is a mode that a test method can be run in. In beforeTest, configuration
 * should be updated as needed (e.g. ComponentsConfiguration properties set) and in afterTest the
 * configuration should be restored back to default.
 */
public interface LithoTestRunConfiguration {

  void beforeTest(FrameworkMethod method);

  void afterTest(FrameworkMethod method);
}
