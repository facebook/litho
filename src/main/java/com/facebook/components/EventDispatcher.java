// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

/**
 * A class implementing this interface will expose a method to dispatch an {@link Event} given
 * an {@link EventHandler}.
 */
public interface EventDispatcher {
  public Object dispatchOnEvent(EventHandler eventHandler, Object eventState);
}
