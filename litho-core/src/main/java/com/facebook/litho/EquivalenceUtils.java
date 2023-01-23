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

import android.util.SparseArray;
import androidx.annotation.Nullable;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.rendercore.primitives.Equivalence;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class EquivalenceUtils {

  /** Checks if objects are equal. */
  public static boolean equals(@Nullable Object a, @Nullable Object b) {
    if (a == b) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    return a.equals(b);
  }

  /** Checks if {@link SparseArray} objects are equal. */
  public static boolean equals(@Nullable SparseArray<?> a, @Nullable SparseArray<?> b) {
    if (a == b) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    if (a.size() != b.size()) {
      return false;
    }

    int size = a.size();

    for (int i = 0; i < size; i++) {
      if (a.keyAt(i) != b.keyAt(i)) {
        return false;
      }

      if (!equals(a.valueAt(i), b.valueAt(i))) {
        return false;
      }
    }

    return true;
  }

  /** Checks if {@link Equivalence} objects are equivalent. */
  public static <T extends Equivalence<T>> boolean isEquivalentTo(@Nullable T a, @Nullable T b) {
    if (a == b) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    return a.isEquivalentTo(b);
  }

  /**
   * Checks if to objects are equivalent if they implement {@link Equivalence} or compares the
   * objects field by field.
   */
  public static <T> boolean isEqualOrEquivalentTo(@Nullable T a, @Nullable T b) {
    if (a instanceof Equivalence && b instanceof Equivalence) {
      return isEquivalentTo((Equivalence) a, (Equivalence) b);
    } else {
      return hasEquivalentFields(a, b, false);
    }
  }

  /** Compare all private final fields in an object */
  @SuppressWarnings("unchecked")
  public static boolean hasEquivalentFields(Object a, Object b) {
    return hasEquivalentFields(a, b, true);
  }

  /** Compare all private final fields in an object */
  @SuppressWarnings("unchecked")
  public static boolean hasEquivalentFields(Object a, Object b, boolean shouldCompareCommonProps) {
    if (a == b) {
      return true;
    }
    if (a.getClass() != b.getClass()) {
      return false;
    }

    for (Field field : a.getClass().getDeclaredFields()) {
      final Object val1;
      final Object val2;
      try {
        final boolean wasAccessible = field.isAccessible();
        if (!wasAccessible) {
          field.setAccessible(true);
        }
        val1 = field.get(a);
        val2 = field.get(b);
        if (!wasAccessible) {
          field.setAccessible(false);
        }
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Unable to get fields by reflection.", e);
      }

      if (!EquivalenceUtils.areObjectsEquivalent(val1, val2, shouldCompareCommonProps)) {
        return false;
      }
    }

    return true;
  }

  /** Return true if two objects are equivalent. */
  @SuppressWarnings("unchecked")
  public static boolean areObjectsEquivalent(
      @Nullable Object val1, @Nullable Object val2, boolean shouldCompareCommonProps) {
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
    } else if (val1 instanceof Component) {
      return ((Component) val1).isEquivalentTo((Component) val2, shouldCompareCommonProps);
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
  private static boolean areArraysEquivalent(
      Object val1, Object val2, boolean shouldCompareCommonProps) {
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
        if (!areObjectsEquivalent(array1[i], array2[i], shouldCompareCommonProps)) {
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
  private static boolean areCollectionsEquivalent(
      Collection collection1, Collection collection2, boolean shouldCompareCommonProps) {
    if (collection1.size() != collection2.size()) {
      return false;
    }

    final Iterator iterator1 = collection1.iterator();
    final Iterator iterator2 = collection2.iterator();

    while (iterator1.hasNext()) {
      final Object elem1 = iterator1.next();
      final Object elem2 = iterator2.next();
      if (!areObjectsEquivalent(elem1, elem2, shouldCompareCommonProps)) {
        return false;
      }
    }
    return true;
  }
}
