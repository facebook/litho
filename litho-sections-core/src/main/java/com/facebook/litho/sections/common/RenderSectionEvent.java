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
import com.facebook.litho.sections.Children;

/**
 * An {@link Event} that gets triggered by a
 * {@link com.facebook.litho.sections.annotations.GroupSectionSpec} to render the section of the
 * {@link com.facebook.litho.sections.fb.datasources.GraphQLRootQuerySectionSpec} result.
 */
@Event(returnType = Children.class)
public class RenderSectionEvent {

  public enum FetchState {
    /**
     * GraphQLConnectionService is performing an initial fetch.
     * This will only happen once for the entire lifecycle of the GraphQLConnectionService.
     */
    INITIAL_STATE,

    /**
     * Data fetching on GraphQLConnectionService has started.
     */
    DOWNLOADING_STATE,

    /**
     * Data fetching has completed and the result is returned from GraphQLConnectionService.
     * Result can be empty.
     *
     * There could be an error with the latest fetch but has cached data available,
     * so the state will not be DOWNLOAD_ERROR.
     */
    IDLE_STATE,

    /**
     * Data fetching has failed to complete and there is an error.
     *
     * Data is always empty at this state.
     */
    DOWNLOAD_ERROR,
  }

  public enum DataSource {
    /** Returned on Initial, Downloading, or Error states */
    UNSET,
    /** the data for the model came from the network */
    FROM_NETWORK,
    /** the data for the model came from the device */
    FROM_LOCAL_CACHE
  }

  public Object model;
  public Object lastNonNullModel;
  public FetchState state;
  public Throwable error;
  public DataSource dataSource;
}
