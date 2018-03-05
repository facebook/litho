/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.annotations.Event;

/**
 * An event used internally to propagate exceptions up the hierarchy. Don't use this directly. Use
 * {@link com.facebook.litho.annotations.OnError} instead.
 */
@Event
public class ErrorEvent {
  /** The exception that caused the error event to be raised. */
  public Exception exception;
}
