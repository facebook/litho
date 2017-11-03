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
 * An {@link Event} that gets triggered by the
 * {@link com.facebook.litho.sections.fb.datasources.BaseGraphQLConnectionSectionServiceListener} in
 * {@link com.facebook.litho.sections.fb.datasources.BaseGraphQLConnectionSection}
 * as the connection state of
 * {@link com.facebook.litho.sections.fb.datasources.GraphQLConnectionService} changes.
 */
@Event
public class ConnectionStateEvent {
  public RenderSectionEvent.FetchState fetchState;
  public Object connectionData;
  public boolean isEmpty;
  public Throwable fetchError;
  public RenderSectionEvent.DataSource dataSource;
}
