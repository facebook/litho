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

import android.os.HandlerThread;
import android.os.Process;
import com.facebook.rendercore.RunnableHandler;

/**
 * LithoHandler implementation backed by a HandlerThread, which allows changing the priority of a
 * HandlerThread.
 */
public class DefaultLithoHandlerDynamicPriority implements RunnableHandler {

  private final HandlerThread mHandlerThread;
  private final DefaultHandler mDelegate;

  /**
   * Creates a {@link RunnableHandler} instance backed by a {@link HandlerThread}. Starts the
   * HandlerThread if it's not started yet.
   */
  public DefaultLithoHandlerDynamicPriority(HandlerThread handlerThread) {
    if (!handlerThread.isAlive()) {
      handlerThread.start();
    }

    mHandlerThread = handlerThread;
    mDelegate = new DefaultHandler(handlerThread.getLooper());
  }

  @Override
  public boolean isTracing() {
    return mDelegate.isTracing();
  }

  @Override
  public void post(Runnable runnable, String tag) {
    mDelegate.post(runnable, tag);
  }

  @Override
  public void postAtFront(Runnable runnable, String tag) {
    mDelegate.postAtFront(runnable, tag);
  }

  @Override
  public void remove(Runnable runnable) {
    mDelegate.remove(runnable);
  }

  public void setThreadPriority(int priority) {
    Process.setThreadPriority(mHandlerThread.getThreadId(), priority);
  }
}
