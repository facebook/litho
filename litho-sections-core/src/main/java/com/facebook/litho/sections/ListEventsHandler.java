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
 * An interface used by GraphQL connection sections to interact with their data-fetching services.
 * @param <QueryParams> this is the same type as QueryParams in
 * {@link com.facebook.controller.connectioncontroller.common.ConnectionConfiguration}
 */
public interface ListEventsHandler<QueryParams> {
  void onPTR(QueryParams ptrFetchParams);

  void onScrollNearBottom(QueryParams tailFetchParams);

  void onScrollNearTop(QueryParams headFetchParams);
}
