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
 * An {@link Event} that is handled by a
 * {@link HideableDataDiffSectionSpec} to remove an edge from the {@link DataDiffSection}
 *
 * @param model the edge model object.
 */
@Event
public class HideItemEvent {
  public Object model;
}
