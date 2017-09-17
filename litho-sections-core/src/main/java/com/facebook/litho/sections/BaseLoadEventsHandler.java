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
 * An empty implementation of {@link LoadEventsHandler}
 */
public class BaseLoadEventsHandler implements LoadEventsHandler {

  @Override
  public void onInitialLoad() {
  }

  @Override
  public void onLoadStarted(boolean empty) {}

  @Override
  public void onLoadSucceded(boolean empty) {
  }

  @Override
  public void onLoadFailed(boolean empty) {
  }
}
