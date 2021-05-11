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

package com.facebook.litho;

import static com.facebook.litho.config.ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY;

import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.LayoutThreadPoolConfiguration;

/** Configures a thread pool used for layout calculations. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutThreadPoolConfigurationImpl implements LayoutThreadPoolConfiguration {

  private int mCorePoolSize;
  private int mMaxPoolSize;
  private int mThreadPriority;

  public LayoutThreadPoolConfigurationImpl(int corePoolSize, int maxPoolSize) {
    this(corePoolSize, maxPoolSize, DEFAULT_BACKGROUND_THREAD_PRIORITY);
  }

  public LayoutThreadPoolConfigurationImpl(int corePoolSize, int maxPoolSize, int threadPriority) {
    mCorePoolSize = corePoolSize;
    mMaxPoolSize = maxPoolSize;
    mThreadPriority = threadPriority;
  }

  @Override
  public int getCorePoolSize() {
    return mCorePoolSize;
  }

  @Override
  public int getMaxPoolSize() {
    return mMaxPoolSize;
  }

  @Override
  public int getThreadPriority() {
    return mThreadPriority;
  }
}
