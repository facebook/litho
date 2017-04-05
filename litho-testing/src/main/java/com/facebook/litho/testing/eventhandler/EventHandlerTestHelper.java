/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.eventhandler;

import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.HasEventDispatcher;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

/**
 * Provides utility functions for mocking {@link EventHandler} in a unit test.
 */
public class EventHandlerTestHelper {

  /**
   * A mock handler that is used to handle events in a unit test
   * @param <E> The type of the event to handle
   * @param <R> The type of the return value of the event
   */
  public interface MockEventHandler<E, R> {

    /**
     * Called when the event is triggered during the unit test
     * @param event The event that was triggered
     * @return The return value of the event handler.
     */
    R handleEvent(E event);
  }

  /**
   * Creates a mock {@link EventHandler}
   * @param eventClass The class of the event that is being handled
   * @param handler The mock handler that gets called when the event is triggered
   * @param <E> The type of the event being handled
   * @param <R> The type of the return value of the event
   * @return A mock event handler
   */
  @SuppressWarnings("unchecked")
  public static <E, R> EventHandler<E> createMockEventHandler(
      Class<E> eventClass,
      final MockEventHandler<E, R> handler) {
    final EventDispatcher dispatcher = mock(EventDispatcher.class);
    when(dispatcher.dispatchOnEvent(
        any(EventHandler.class),
        any(eventClass)))
        .then(new Answer<R>() {
          @Override
          public R answer(InvocationOnMock invocation) throws Throwable {
            final E event = (E) invocation.getArguments()[1];
            if (event != null) {
              return handler.handleEvent(event);
            } else {
              return null;
            }
          }
        });

    return new EventHandler<>(
        new HasEventDispatcher() {
          @Override
          public EventDispatcher getEventDispatcher() {
            return dispatcher;
          }
        },
        0,
        null);
  }
}
