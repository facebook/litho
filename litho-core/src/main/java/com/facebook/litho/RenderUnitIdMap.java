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

import androidx.annotation.GuardedBy;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class RenderUnitIdMap {

  private final AtomicInteger mNextId = new AtomicInteger(1);

  @GuardedBy("this")
  private final HashMap<String, Integer> mKeyToId = new HashMap<>();

  public synchronized int getId(String key) {
    final Integer currentId = mKeyToId.get(key);
    if (currentId != null) {
      return currentId;
    }

    int nextId = mNextId.getAndIncrement();
    mKeyToId.put(key, nextId);
    return nextId;
  }
}
