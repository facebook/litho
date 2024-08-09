/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.common;

import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.annotations.Event;

/**
 * An {@link Event} that gets triggered by the {@link
 * com.facebook.litho.sections.fb.datasources.BaseGraphQLConnectionSectionServiceListener} in {@link
 * com.facebook.litho.sections.fb.datasources.BaseGraphQLConnectionSection} as the connection state
 * of {@link com.facebook.litho.sections.fb.datasources.GraphQLConnectionService} changes.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@Event
public class ConnectionStateEvent {
  // NULLSAFE_FIXME[Field Not Initialized]
  public RenderSectionEvent.FetchState fetchState;
  // NULLSAFE_FIXME[Field Not Initialized]
  public Object connectionData;
  public boolean isEmpty;
  // NULLSAFE_FIXME[Field Not Initialized]
  public Throwable fetchError;
  // NULLSAFE_FIXME[Field Not Initialized]
  public RenderSectionEvent.DataSource dataSource;
  // NULLSAFE_FIXME[Field Not Initialized]
  public RenderSectionEvent.FetchType fetchType;
}
