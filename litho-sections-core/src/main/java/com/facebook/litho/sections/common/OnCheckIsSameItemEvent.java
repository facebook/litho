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
 * This event is triggered by {@link DataDiffSectionSpec} when it needs to verify whether two model
 * objects represent the same item in the collection.
 *
 * todo(t16485443): The generic type declaration(OnCheckIsSameItemEvent<TEdgeModel>)
 * is temporarily removed until the bug in the attached task is fixed.
 */
@Event(returnType = Boolean.class)
public class OnCheckIsSameItemEvent {
  public Object previousItem;
  public Object nextItem;
}
