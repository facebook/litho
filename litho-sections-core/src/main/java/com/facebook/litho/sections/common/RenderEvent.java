/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.common;

import android.os.Bundle;
import android.support.annotation.Nullable;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.widget.RenderInfo;

/**
 * An {@link Event} that gets triggered by a
 * {@link com.facebook.litho.sections.annotations.DiffSectionSpec} to render the edges of the
 * connection.
 *
 * {@link RenderEvent} is triggered when the
 * {@link com.facebook.litho.sections.annotations.DiffSectionSpec}
 * is generating the {@link com.facebook.litho.sections.ChangeSet}.
 *
 * This means that when {@link RenderEvent} is handled, the components are not inserted into
 * the adapter yet.
 *
 * @param index the index of the item in the collection.
 * @param model the edge model object.
 * @param loggingExtras a bundle of logging extras we want to expose to the rendered component.
 */
@Event(returnType = RenderInfo.class)
public class RenderEvent {
  public int index;
  public Object model;
  public @Nullable Bundle loggingExtras;
}
