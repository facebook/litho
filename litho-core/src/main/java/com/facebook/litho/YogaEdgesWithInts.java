/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import com.facebook.yoga.YogaEdge;

public interface YogaEdgesWithInts {

  void add(YogaEdge yogaEdge, int value);

  int size();

  YogaEdge getEdge(int index);

  int getValue(int index);
}
