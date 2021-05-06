/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import androidx.annotation.Nullable;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.sections.Children;

/**
 * An {@link Event} that gets triggered by a {@link
 * com.facebook.litho.sections.annotations.GroupSectionSpec} to render the section of the {@link
 * com.facebook.litho.sections.fb.datasources.GraphQLRootQuerySectionSpec} result.
 */
@Event(returnType = Children.class)
public class RenderSectionEvent {

  public enum FetchState {
    /**
     * GraphQLConnectionService is performing an initial fetch. This will only happen once for the
     * entire lifecycle of the GraphQLConnectionService.
     */
    INITIAL_STATE,

    /** Data fetching on GraphQLConnectionService has started. */
    DOWNLOADING_STATE,

    /**
     * Data fetching has completed and the result is returned from GraphQLConnectionService. Result
     * can be empty.
     *
     * <p>There could be an error with the latest fetch but has cached data available, so the state
     * will not be DOWNLOAD_ERROR.
     */
    IDLE_STATE,

    /**
     * Data fetching has failed to complete and there is an error.
     *
     * <p>Data is always empty at this state.
     */
    DOWNLOAD_ERROR,
  }

  public enum DataSource {
    /** Returned on Initial, Downloading, or Error states */
    UNSET,
    /** the data for the model came from the network */
    FROM_NETWORK,
    /** the data for the model came from the device */
    FROM_LOCAL_CACHE,
    /** the stale data for the model came from the device */
    FROM_LOCAL_STALE_CACHE;

    public static boolean fromCache(DataSource dataSource) {
      return dataSource == DataSource.FROM_LOCAL_CACHE
          || dataSource == DataSource.FROM_LOCAL_STALE_CACHE;
    }
  }

  public enum FetchType {
    /** Data refresh fetch */
    REFRESH_FETCH,
    /** Data head fetch for more data */
    HEAD_FETCH,
    /** Data tail fetch for more data */
    TAIL_FETCH,
  }

  public Object model;
  public @Nullable Object lastNonNullModel;
  public FetchState state;
  public Throwable error;
  public DataSource dataSource;
  public @Nullable FetchType fetchType;
}
