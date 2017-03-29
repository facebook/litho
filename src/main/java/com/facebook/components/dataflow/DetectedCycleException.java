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
 * Exception thrown when a runtime cycle is detected.
 */
public class DetectedCycleException extends BadGraphSetupException {

  public DetectedCycleException(String detailMessage) {
    super(detailMessage);
  }
}
