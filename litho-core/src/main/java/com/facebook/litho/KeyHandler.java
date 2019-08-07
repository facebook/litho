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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class manages the {@link Component}s global keys for a {@link ComponentTree}. It provides
 * methods for detecting duplicate keys and logging duplicate key occurences.
 *
 * <p>TODO: (T38237241) remove the usage of the key handler post the nested tree experiment
 */
public class KeyHandler {

  private static final String STACK_TRACE_NO_SPEC_MESSAGE =
      "Unable to determine root of duplicate key in a *Spec.java file.";
  private static final String STACK_TRACE_SPEC_MESSAGE =
      "Please look at the following spec hierarchy and make sure "
          + "all sibling children components of the same type have unique keys:\n";

  private final @Nullable ComponentsLogger mLogger;
  private final Set<String> mKnownGlobalKeys;

  public KeyHandler(@Nullable ComponentsLogger logger) {
    mKnownGlobalKeys = new HashSet<>();
    mLogger = logger;
  }

  public void registerKey(Component component) {
    /**
     * We still need to check whether the component's global key is unique, in case a duplicate key
     * has been manually set on sibling components.
     */
    checkIsDuplicateKey(component);
    mKnownGlobalKeys.add(component.getGlobalKey());
  }

  /** Returns true if this KeyHandler has already recorded a component with the given key. */
  public boolean hasKey(String key) {
    return mKnownGlobalKeys.contains(key);
  }

  private void checkIsDuplicateKey(Component component) {
    if (mKnownGlobalKeys.contains(component.getGlobalKey())) {
      final String message =
          "Found another "
              + component.getSimpleName()
              + " Component with the same key: "
              + component.getKey();
      final String errorMessage = mLogger == null ? message : getDuplicateKeyMessage();

      if (component.hasState()) {
        throw new RuntimeException(message + "\n" + errorMessage);
      }

      if (mLogger != null) {
        mLogger.emitMessage(ComponentsLogger.LogLevel.ERROR, message + "\n" + errorMessage);
      }
    }
  }

  /**
   * Builds a list of file names that could be useful to trace the duplicate key source. Adds spec
   * and part definition classes and excludes blacklisted file names. Doesn't add the same file name
   * in a row since that's not useful.
   */
  private String getDuplicateKeyMessage() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    List<String> specHierarchy = new ArrayList<>();

    for (int i = 0, len = stackTrace.length; i < len; i++) {
      final StackTraceElement stackElement = stackTrace[i];
      final String fileName = stackElement.getFileName();
      if (fileName == null) {
        continue;
      }

      final boolean hasJustBeenAdded =
          !specHierarchy.isEmpty() && specHierarchy.get(specHierarchy.size() - 1).equals(fileName);

      if (hasMatch(fileName)
          && !mLogger.getKeyCollisionStackTraceBlacklist().contains(fileName)
          && !hasJustBeenAdded) {
        specHierarchy.add(fileName);
      }
    }

    if (specHierarchy.isEmpty()) {
      return STACK_TRACE_NO_SPEC_MESSAGE;
    }

    return format(specHierarchy);
  }

  private static String format(List<String> specHierarchy) {
    Collections.reverse(specHierarchy);

    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(STACK_TRACE_SPEC_MESSAGE);
    int tabLevel = 1;

    for (String spec : specHierarchy) {
      for (int i = 0; i < tabLevel; i++) {
        messageBuilder.append("\t");
      }
      tabLevel++;
      messageBuilder.append(spec);
      messageBuilder.append("\n");
    }

    return messageBuilder.toString();
  }

  private boolean hasMatch(String filename) {
    for (String query : mLogger.getKeyCollisionStackTraceKeywords()) {
      if (filename.contains(query)) {
        return true;
      }
    }

    return false;
  }
}
