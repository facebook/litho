/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.content.res.Configuration;
import androidx.collection.LruCache;
import javax.annotation.Nullable;

class LruResourceCache extends ResourceCache {
  private final LruCache<Integer, Object> mCache =
      new LruCache<Integer, Object>(500) {
        @Override
        protected int sizeOf(Integer key, Object value) {
          if (value instanceof String) {
            return ((String) value).length();
          }
          return 1;
        }
      };

  LruResourceCache(Configuration configuration) {
    super(configuration);
  }

  @Override
  @Nullable
  <T> T get(int key) {
    return (T) mCache.get(key);
  }

  @Override
  void put(int key, Object object) {
    mCache.put(key, object);
  }
}
