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
import com.facebook.litho.drawable.ComparableColorDrawable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class EquivalenceUtils {

  /** Return true if two objects are equivalent. */
  @SuppressWarnings("unchecked")
  public static boolean areObjectsEquivalent(@Nullable Object val1, @Nullable Object val2) {
    if (val1 == val2) {
      return true;
    }
    if (val1 == null || val2 == null || val1.getClass() != val2.getClass()) {
      return false;
    }

    if (val1 instanceof Float) {
      return Float.compare((Float) val1, (Float) val2) == 0;
    } else if (val1 instanceof Double) {
      return Double.compare((Double) val1, (Double) val2) == 0;
    } else if (val1 instanceof Equivalence) {
      return ((Equivalence) val1).isEquivalentTo(val2);
    } else if (val1.getClass().isArray()) {
      // TODO(T69494307): replace this with areArraysEquivalent(Object, Object).
      return ComponentUtils.areArraysEquals(val1.getClass(), val1, val2);
    } else if (val1 instanceof Collection) {
      // TODO(T69494307):
      // replace this with areCollectionsEquivalent(Collection, Collection).
      return ComponentUtils.areCollectionsEquals(
          val1.getClass().getGenericSuperclass(), (Collection) val1, (Collection) val2);
    } else if (val1 instanceof ComparableColorDrawable) {
      return ((ComparableColorDrawable) val1).isEquivalentTo((ComparableColorDrawable) val2);
    } else if (val1 instanceof EventHandler) {
      return ((EventHandler) val1).isEquivalentTo((EventHandler) val2);
    }
    return val1.equals(val2);
  }

  /**
   * TODO(T69494307): Don't delete this method, it's going to replace {@code
   * ComponentUtils.areArraysEquals(Class<?>, Object, Object)}
   */
  private static boolean areArraysEquivalent(Object val1, Object val2) {
    if (val1 instanceof byte[]) {
      return Arrays.equals((byte[]) val1, (byte[]) val2);
    } else if (val1 instanceof short[]) {
      return Arrays.equals((short[]) val1, (short[]) val2);
    } else if (val1 instanceof char[]) {
      return Arrays.equals((char[]) val1, (char[]) val2);
    } else if (val1 instanceof int[]) {
      return Arrays.equals((int[]) val1, (int[]) val2);
    } else if (val1 instanceof long[]) {
      return Arrays.equals((long[]) val1, (long[]) val2);
    } else if (val1 instanceof float[]) {
      return Arrays.equals((float[]) val1, (float[]) val2);
    } else if (val1 instanceof double[]) {
      return Arrays.equals((double[]) val1, (double[]) val2);
    } else if (val1 instanceof boolean[]) {
      return Arrays.equals((boolean[]) val1, (boolean[]) val2);
    } else {
      final Object[] array1 = (Object[]) val1;
      final Object[] array2 = (Object[]) val2;

      if (array1.length != array2.length) {
        return false;
      }

      for (int i = 0, size = array1.length; i < size; i++) {
        if (!areObjectsEquivalent(array1[i], array2[i])) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * TODO(T69494307): Don't delete this method, it's going to replace {@code
   * ComponentUtils.areCollectionsEquals(Type, Collection, Collection)}
   */
  private static boolean areCollectionsEquivalent(Collection collection1, Collection collection2) {
    if (collection1.size() != collection2.size()) {
      return false;
    }

    final Iterator iterator1 = collection1.iterator();
    final Iterator iterator2 = collection2.iterator();

    while (iterator1.hasNext()) {
      final Object elem1 = iterator1.next();
      final Object elem2 = iterator2.next();
      if (!areObjectsEquivalent(elem1, elem2)) {
        return false;
      }
    }
    return true;
  }
}
