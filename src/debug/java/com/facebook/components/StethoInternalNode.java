// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

/**
 * This class is only needed until support for stable ids is implemented in stetho. Currently
 * stetho uses object instances as ids so we expose a wrapper to stetho as our stable "id". We keep
 * a mapping of component keys to wrappers stored in the ComponentsStethoManager.
 */
class StethoInternalNode {
  String key;
  InternalNode node;
}
