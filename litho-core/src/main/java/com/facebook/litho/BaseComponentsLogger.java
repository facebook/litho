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

import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base implementation of {@link ComponentsLogger} which handles pooling event objects.
 */
public abstract class BaseComponentsLogger implements ComponentsLogger {

  /** Filenames that match these keywords will be added to the stack trace. */
  private static final Set<String> sStackTraceKeywords = new HashSet<>();

  /** Filenames that match these blacklisted items will be excluded from the stack trace. */
  private static final Set<String> sStackTraceBlacklist = new HashSet<>();

  static {
    sStackTraceKeywords.add("Spec.java");
    sStackTraceKeywords.add("Activity.java");
  }

  @Override
  public Set<String> getKeyCollisionStackTraceKeywords() {
    return Collections.unmodifiableSet(sStackTraceKeywords);
  }

  @Override
  public Set<String> getKeyCollisionStackTraceBlacklist() {
    return Collections.unmodifiableSet(sStackTraceBlacklist);
  }

  @Nullable
  @Override
  public Map<String, String> getExtraAnnotations(TreeProps treeProps) {
    return null;
  }
}
