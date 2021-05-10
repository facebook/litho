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

package com.facebook.litho;

import android.content.res.Configuration;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class ResourceCache {
  private static ResourceCache latest;

  static synchronized ResourceCache getLatest(Configuration configuration) {
    if (latest == null || !latest.mConfiguration.equals(configuration)) {
      latest = new LruResourceCache(new Configuration(configuration));
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
