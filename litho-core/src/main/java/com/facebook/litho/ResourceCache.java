/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import javax.annotation.Nullable;

import android.content.res.Configuration;

abstract class ResourceCache {
  private static ResourceCache latest;

  static synchronized ResourceCache getLatest(Configuration configuration) {
    if (latest == null || !latest.mConfiguration.equals(configuration)) {
      latest = new LruResourceCache(configuration);
    }
    return latest;
  }

  private final Configuration mConfiguration;

  protected ResourceCache(Configuration configuration) {
    mConfiguration = configuration;
  }

  @Nullable
  abstract <T> T get(int key);

  abstract void put(int key, Object object);
}
