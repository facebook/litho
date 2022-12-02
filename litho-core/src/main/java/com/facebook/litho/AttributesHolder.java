/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import java.util.HashMap;
import java.util.Map;

/** Simple holder which allows to store and retrieve attributes. */
public class AttributesHolder implements AttributesAcceptor {

  private final Map<AttributeKey<?>, Object> mAttributes = new HashMap<>();

  public <T> T get(AttributeKey<T> key) {
    if (!mAttributes.containsKey(key)) {
      throw new IllegalStateException("There is no attribute for key " + key);
    }

    return (T) mAttributes.get(key);
  }

  @Override
  public <T> void setAttributeKey(AttributeKey<T> attributeKey, T value) {
    mAttributes.put(attributeKey, value);
  }
}
