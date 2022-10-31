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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to be used as a prop in {@link
 * com.facebook.litho.widget.RenderAndLayoutCountingTesterSpec}
 *
 * <p>This class will help count the total amount of renders and measures on a
 * RenderAndLayoutCountingTesterSpec
 */
public class RenderAndMeasureCounter {
  private final AtomicInteger mRenderCount = new AtomicInteger();
  private final AtomicInteger mMeasureCount = new AtomicInteger();

  public int getRenderCount() {
    return mRenderCount.intValue();
  }

  public int getMeasureCount() {
    return mMeasureCount.intValue();
  }

  public void incrementRenderCount() {
    mRenderCount.incrementAndGet();
  }

  public void incrementMeasureCount() {
    mMeasureCount.incrementAndGet();
  }

  public void reset() {
    mRenderCount.set(0);
    mMeasureCount.set(0);
  }
}
