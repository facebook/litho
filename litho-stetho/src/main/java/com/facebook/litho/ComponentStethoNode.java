/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * Represents a component in the stetho node hierarchy. We need the backing InternalNode and the
 * reference to which component in the components list this object represents.
 */
class ComponentStethoNode {
  String key;
  InternalNode node;
  int componentIndex;
}
