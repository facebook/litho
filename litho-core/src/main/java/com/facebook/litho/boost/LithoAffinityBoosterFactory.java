/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.boost;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating a {@link LithoAffinityBooster}. One LithoAffinityBooster per thread is
 * created.
 */
public abstract class LithoAffinityBoosterFactory {

  Map<String, LithoAffinityBooster> mBoosters = new HashMap<>();

  public LithoAffinityBooster acquireInstance(String tag, int threadId) {
    LithoAffinityBooster booster = mBoosters.get(tag);

    if (booster != null) {
      return booster;
    }

    booster = create(tag, threadId);
    mBoosters.put(tag, booster);

    return booster;
  }

  public LithoAffinityBooster acquireInstance(String tag) {
    LithoAffinityBooster booster = mBoosters.get(tag);

    if (booster != null) {
      return booster;
    }

    booster = create(tag);
    mBoosters.put(tag, booster);

    return booster;
  }

  protected abstract LithoAffinityBooster create(String tag);

  protected abstract LithoAffinityBooster create(String tag, int threadId);
}
