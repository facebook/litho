/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

/**
 * Exception thrown when a view isn't found in a test.
 */
public class ViewForTestKeyNotFoundException extends RuntimeException {

  public ViewForTestKeyNotFoundException(String testKey) {
    super("The view with the given test key was not found: " + testKey);
  }
}
