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
 * A class implementing this interface wll expose a method annotated with
 * {@link com.facebook.litho.annotations.OnTrigger} to accept an
 * {@link com.facebook.litho.annotations.Event} given an {@link EventTrigger}
 */
public interface EventTriggerTarget {
  Object acceptTriggerEvent(EventTrigger eventTrigger, Object eventState, Object[] params);
}
