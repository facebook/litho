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
 * objects that represent the same item also have the same content.
 *
 * todo(t16485443): The generic type declaration(OnCheckIsSameContentEvent<TEdgeModel>)
 * is temporarily removed until the bug in the attached task is fixed.
 */
@Event(returnType = Boolean.class)
public class OnCheckIsSameContentEvent {
  public Object previousItem;
  public Object nextItem;
}
