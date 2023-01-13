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

package com.facebook.litho.testing.api

import com.facebook.litho.config.ComponentsConfiguration
import org.junit.After
import org.junit.Before

/**
 * This is a not desirable approach to force the test class that extends this one to run with
 * [ComponentsConfiguration.isEndToEndTestRun] always set to `true`.
 *
 * This is needed at the moment since this flag allows to keep extra information about the mounted
 * components, which allows us to build the test nodes tree even with lazy lists/sections. When this
 * flag is disabled this information is discarded and we can't inspect the components inside the
 * collection.
 */
abstract class RunWithDebugInfoTest {

  private val defaultIsEndToEndTest = ComponentsConfiguration.isEndToEndTestRun

  @Before
  fun setup() {
    ComponentsConfiguration.isEndToEndTestRun = true
  }

  @After
  fun teardown() {
    ComponentsConfiguration.isEndToEndTestRun = defaultIsEndToEndTest
  }
}
