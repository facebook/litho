// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.content.res.Configuration;
import android.support.v4.util.LruCache;

class ResourceCache {
  private static ResourceCache latest;

  static synchronized ResourceCache getLatest(Configuration configuration) {
    if (latest == null || !latest.mConfiguration.equals(configuration)) {
      latest = new ResourceCache(configuration);
    }
    return latest;
  }

  private Configuration mConfiguration;
  private final LruCache<Integer, Object> mCache = new LruCache<Integer, Object>(500) {
    @Override
    protected int sizeOf(Integer key, Object value) {
      if (value instanceof String) {
        return ((String) value).length();
      }
      return 1;
    }
  };

  private ResourceCache(Configuration configuration) {
    mConfiguration = configuration;
  }

  void put(int key, Object object) {
    mCache.put(key, object);
  }

  <T> T get(int key) {
    return (T) mCache.get(key);
  }
}
