/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

/** Describing a class which allows the transfer of props onto an {@link InternalNode}. */
interface CommonPropsCopyable {
  void copyInto(ComponentContext c, InternalNode node);
}
