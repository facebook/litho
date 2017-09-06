/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * Allows the parent to communicate with its child component. The child must
 * be able to handle {@link com.facebook.litho.annotations.OnTrigger} events
 * in order to accept an EventTrigger from the parent.
 */
public class EventTrigger<E> {

  public final HasEventTrigger mTriggerTarget;
  public int mId;

  public EventTrigger(HasEventTrigger triggerTarget) {
    mTriggerTarget = triggerTarget;
  }

  public Object dispatchOnTrigger(E event, Object[] params) {
    return mTriggerTarget.getEventTriggerTarget().acceptTriggerEvent(this, event, params);
  }
}
