/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.dataflow;

/**
 * Exception thrown when a node tries to calculate and propagate an illegal value (e.g. NaN).
 */
public class IllegalValueException extends RuntimeException {

  public IllegalValueException(String detailMessage) {
    super(detailMessage);
  }
}
