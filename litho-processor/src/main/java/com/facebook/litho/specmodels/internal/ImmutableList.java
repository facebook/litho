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

package com.facebook.litho.specmodels.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A dummy representation of an immutable set. This can be used temporarily as a type until we have
 * an actual non-guava implementation.
 */
public class ImmutableList<E> extends ArrayList<E> {

  private ImmutableList(final int capacity) {
    super(capacity);
  }

  private ImmutableList(List<E> list) {
    super(list);
  }

  public static <E> ImmutableList<E> copyOf(List<E> list) {
    return new ImmutableList<>(list);
  }

  public static <E> ImmutableList<E> of(E... elements) {
    final ImmutableList<E> list = new ImmutableList<>(elements.length);
    Collections.addAll(list, elements);
    return list;
  }
}
