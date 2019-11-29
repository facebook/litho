// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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

import androidx.annotation.VisibleForTesting;
import javax.annotation.Nullable;

public class NoOpEventHandler<E> extends EventHandler<E> {

  private static final int ID = -1;
  @VisibleForTesting static final NoOpEventHandler sNoOpEventHandler = new NoOpEventHandler();

  private static final class NoOpHasEventDispatcher implements HasEventDispatcher {
    @Override
    public EventDispatcher getEventDispatcher() {
      return new EventDispatcher() {
        @Nullable
        @Override
        public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
          // DO NOTHING HERE
          // The exception should be thrown when the handler is create, much before any events
          // get dispatched.
          return null;
        }
      };
    }
  }

  private NoOpEventHandler() {
    super(new NoOpHasEventDispatcher(), ID);
  }

  private NoOpEventHandler(HasEventDispatcher hasEventDispatcher, int id) {
    super(hasEventDispatcher, id);
  }

  private NoOpEventHandler(
      HasEventDispatcher hasEventDispatcher, int id, @Nullable Object[] params) {
    super(hasEventDispatcher, id, params);
  }

  public static <E> NoOpEventHandler<E> getNoOpEventHandler() {
    //noinspection unchecked
    return sNoOpEventHandler;
  }
}
