/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

public class EventHandler<E> {

  public final HasEventDispatcher mHasEventDispatcher;
  public final int id;
  public final Object[] params;

  protected EventHandler(HasEventDispatcher hasEventDispatcher, int id) {
    this(hasEventDispatcher, id, null);
  }

  public EventHandler(HasEventDispatcher hasEventDispatcher, int id, Object[] params) {
    this.mHasEventDispatcher = hasEventDispatcher;
    this.id = id;
    this.params = params;
