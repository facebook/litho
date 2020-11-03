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

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;

/**
 * DebugMetadata is a special class that allows custom key-value pairs to be appended to {@link
 * LithoMetadataExceptionWrapper} when it wraps an exception. To use it, create a DebugMetadata tree
 * prop via @OnCreateTreeProp and add custom metadata you want to appear in crashes. Any crash
 * coming from that subtree will contain this metadata in the LithoMetadataExceptionWrapper. Make
 * sure to pass in the existing DebugMetadata tree prop if there is one.
 *
 * <p>Example:
 *
 * <pre>
 *   @OnCreateTreeProp
 *   static DebugMetadata createDebugMetadata(
 *       ComponentContext c,
 *       @Prop String metadataValue,
 *       @TreeProp DebugMetadata existingMetadata) {
 *     return DebugMetadata.createWithMetadata(existingMetadata, "metadata_key", metadataValue);
 *   }
 * </pre>
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DebugMetadata {

  private final HashMap<String, String> mMetadata;

  /**
   * Creates a new DebugMetadata object based on an existing DebugMetadata object (or null). Keys
   * and values will override existing properties with the same key. See the example in the class
   * javadocs for how to use it.
   */
  public static DebugMetadata createWithMetadata(
      @Nullable DebugMetadata existingMetadata,
      String key,
      String value,
      String... moreKeysAndValues) {
    if (moreKeysAndValues.length % 2 != 0) {
      throw new RuntimeException("Keys and values must come in pairs");
    }

    final HashMap<String, String> newMap;
    if (existingMetadata == null) {
      newMap = new HashMap<>();
    } else {
      newMap = new HashMap<>(existingMetadata.mMetadata);
    }

    newMap.put(key, value);
    for (int i = 0; i < moreKeysAndValues.length; i += 2) {
      newMap.put(moreKeysAndValues[i], moreKeysAndValues[i + 1]);
    }

    return new DebugMetadata(newMap);
  }

  private DebugMetadata(HashMap<String, String> metadata) {
    mMetadata = metadata;
  }

  HashMap<String, String> getMetadataMap() {
    return mMetadata;
  }
}
