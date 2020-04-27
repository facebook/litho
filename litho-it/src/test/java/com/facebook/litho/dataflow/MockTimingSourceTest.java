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

package com.facebook.litho.dataflow;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.facebook.litho.choreographercompat.ChoreographerCompat.FrameCallback;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(ComponentsTestRunner.class)
public class MockTimingSourceTest {

  private MockTimingSource mTimingSource;

  @Before
  public void setUp() {
    mTimingSource = new MockTimingSource();
    DataFlowGraph.create(mTimingSource);
    mTimingSource.start();
  }

  @Test
  public void testPostFrameCallback() {
    ArrayList<FrameCallback> callbacks = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      FrameCallback callback = mock(FrameCallback.class);
      callbacks.add(callback);
      mTimingSource.postFrameCallback(callback);
    }

    mTimingSource.step(1);

    for (int i = 0; i < callbacks.size(); i++) {
      Mockito.verify(callbacks.get(i)).doFrame(anyLong());
    }
  }

  @Test
  public void testPostFrameCallbackDelayed() {
    FrameCallback callback1 = mock(FrameCallback.class);
    FrameCallback callback2 = mock(FrameCallback.class);
    FrameCallback delayedCallback = mock(FrameCallback.class);

    mTimingSource.postFrameCallback(callback1);
    mTimingSource.postFrameCallbackDelayed(delayedCallback, 20);
    mTimingSource.postFrameCallback(callback2);

    mTimingSource.step(1);

    verify(callback1).doFrame(anyLong());
    verify(callback2).doFrame(anyLong());
    verify(delayedCallback, never()).doFrame(anyLong());

    mTimingSource.step(1);

    verify(delayedCallback).doFrame(anyLong());
  }

  @Test
  public void testNestedFrameCallbacks() {
    FrameCallback callback =
        new FrameCallback() {
          @Override
          public void doFrame(long frameTimeNanos) {
            mTimingSource.postFrameCallback(
                new FrameCallback() {
                  @Override
                  public void doFrame(long frameTimeNanos) {
                    fail("Nested FrameCallback should not be called in the same step");
                  }
                });
          }
        };

    mTimingSource.postFrameCallback(callback);

    mTimingSource.step(1);
  }
}
