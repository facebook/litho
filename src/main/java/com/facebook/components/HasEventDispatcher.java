// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

/**
 * A class implementing this interface will expose a method to retrieve an {@link EventDispatcher}.
 */
public interface HasEventDispatcher {
  EventDispatcher getEventDispatcher();
}
