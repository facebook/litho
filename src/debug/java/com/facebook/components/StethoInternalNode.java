/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * This class is only needed until support for stable ids is implemented in stetho. Currently
 * stetho uses object instances as ids so we expose a wrapper to stetho as our stable "id". We keep
 * a mapping of component keys to wrappers stored in the ComponentsStethoManager.
 */
class StethoInternalNode {
  String key;
  InternalNode node;
}
