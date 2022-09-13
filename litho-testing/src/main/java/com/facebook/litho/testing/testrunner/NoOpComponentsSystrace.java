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

package com.facebook.litho.testing.testrunner;

import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.Systracer;

public class NoOpComponentsSystrace implements Systracer {

  public static final NoOpComponentsSystrace sInstance = new NoOpComponentsSystrace();

  @Override
  public void beginSection(String name) {}

  @Override
  public void beginAsyncSection(String name) {}

  @Override
  public void beginAsyncSection(String name, int cookie) {}

  @Override
  public ArgsBuilder beginSectionWithArgs(String name) {
    return RenderCoreSystrace.NO_OP_ARGS_BUILDER;
  }

  @Override
  public void endSection() {}

  @Override
  public void endAsyncSection(String name) {}

  @Override
  public void endAsyncSection(String name, int cookie) {}

  @Override
  public boolean isTracing() {
    return true;
  }
}
