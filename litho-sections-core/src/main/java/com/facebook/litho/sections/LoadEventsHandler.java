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
 * An interface used by a {@link Section} to notify its users of the status
 * of the connection while loading new items.
 */
public interface LoadEventsHandler {

  /**
   * Invoked when a {@link Section} performs it's initial fetch.
   */
  void onInitialLoad();

  /**
   * Invoked every time the {@link Section} starts loading new elements.
   *
   * @param empty true on load starts
   */
  void onLoadStarted(boolean empty);

  /**
   * Invoked every time the {@link Section} succeeds in loading new elements.
   * @param  empty true if after this fetch the dataset is empty.
   */
  void onLoadSucceded(boolean empty);

  /**
   * Invoked every time the {@link Section} fails loading new elements.
   * @param  empty true if after this failure the dataset is empty.
   */
  void onLoadFailed(boolean empty);
}
