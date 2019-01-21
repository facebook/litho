/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread pool executor implementation used to calculate layout on multiple background threads. */
public class LayoutThreadPoolExecutor extends ThreadPoolExecutor {

  public LayoutThreadPoolExecutor(int corePoolSize, int maxPoolSize, int priority) {
    super(
        corePoolSize,
        maxPoolSize,
        1,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(),
        new LayoutThreadFactory(priority));
  }
}
