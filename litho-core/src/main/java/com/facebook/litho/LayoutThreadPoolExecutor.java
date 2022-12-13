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

package com.facebook.litho;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.ComponentsConfiguration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Thread pool executor implementation used to calculate layout on multiple background threads. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutThreadPoolExecutor extends ThreadPoolExecutor {

  public LayoutThreadPoolExecutor(int corePoolSize, int maxPoolSize, int priority) {
    this(corePoolSize, maxPoolSize, priority, null);
  }

  public LayoutThreadPoolExecutor(
      int corePoolSize, int maxPoolSize, int priority, @Nullable Runnable layoutThreadInitializer) {
    super(
        corePoolSize,
        maxPoolSize,
        ComponentsConfiguration.layoutThreadKeepAliveTimeMs,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(),
        new LayoutThreadFactory(priority, layoutThreadInitializer));
    this.allowCoreThreadTimeOut(ComponentsConfiguration.shouldAllowCoreThreadTimeout);
  }
}
