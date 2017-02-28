// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

public class EventHandler<E> {

  public final HasEventDispatcher mHasEventDispatcher;
  public final int id;
  public final Object[] params;

  protected EventHandler(HasEventDispatcher hasEventDispatcher, int id) {
    this(hasEventDispatcher, id, null);
  }

  public EventHandler(HasEventDispatcher hasEventDispatcher, int id, Object[] params) {
    this.mHasEventDispatcher = hasEventDispatcher;
    this.id = id;
    this.params = params;
  }

  public void dispatchEvent(E event) {
    mHasEventDispatcher.getEventDispatcher().dispatchOnEvent(this, event);
  }
}
