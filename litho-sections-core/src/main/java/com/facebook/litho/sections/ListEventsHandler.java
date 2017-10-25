/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

/**
 * An interface used by sections to interact with their data-fetching services.
 *
 * @param <QueryParams> POJO of data to be passed to the data-fetching service.
 */
public interface ListEventsHandler<QueryParams> {
  void onPTR(QueryParams ptrFetchParams);

  void onScrollNearBottom(QueryParams tailFetchParams);

  void onScrollNearTop(QueryParams headFetchParams);
}
