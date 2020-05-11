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

public class NoOpComponentsSystrace implements ComponentsSystrace.Systrace {

  public static final NoOpComponentsSystrace sInstance = new NoOpComponentsSystrace();

  @Override
  public void beginSection(String name) {}

  @Override
  public void beginSectionAsync(String name) {}

  @Override
  public void beginSectionAsync(String name, int cookie) {}

  @Override
  public ComponentsSystrace.ArgsBuilder beginSectionWithArgs(String name) {
    return ComponentsSystrace.NO_OP_ARGS_BUILDER;
  }

  @Override
  public void endSection() {}

  @Override
  public void endSectionAsync(String name) {}

  @Override
  public void endSectionAsync(String name, int cookie) {}

  @Override
  public boolean isTracing() {
    return true;
  }
}
