/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.common;

import com.facebook.litho.annotations.Event;
/**
 * This event is triggered by {@link HideableDataDiffSectionSpec} when it needs to
 * get a unique identifier for the edge model provided.
 */
@Event(returnType = Object.class)
public class GetUniqueIdentifierEvent {
  public Object model;
}
