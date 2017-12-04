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
 * Allows a top-down communication with a component and its immediate parent. The component must be
 * able to handle {@link com.facebook.litho.annotations.OnTrigger} events in order to accept an
 * EventTrigger.
 */
public class EventTrigger<E> {

  public EventTriggerTarget mTriggerTarget;
  public final int mId;
  public final String mKey;

  public EventTrigger(String parentKey, int id, String childKey) {
    mId = id;
    mKey = parentKey + id + childKey;
  }

  public Object dispatchOnTrigger(E event) {
    return dispatchOnTrigger(event, new Object[] {});
  }

  public Object dispatchOnTrigger(E event, Object[] params) {
    return mTriggerTarget.acceptTriggerEvent(this, event, params);
  }
}
