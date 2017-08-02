/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.List;

/**
 * An interface that a mountable view can extend which informs that this mountable content has other
 * LithoView children. This is used to make sure to unmount this view's children when unmounting
 * this view itself.
 */
public interface HasLithoViewChildren {
  void obtainLithoViewChildren(List<LithoView> lithoViews);
}
