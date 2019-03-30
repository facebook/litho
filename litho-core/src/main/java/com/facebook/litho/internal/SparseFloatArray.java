/*
 * Copyright 2006-present Facebook, Inc.
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
package com.facebook.litho.internal;

import androidx.annotation.Nullable;
import java.util.Arrays;

/** SparseFloatArray map integers to floats similar to {@link android.util.SparseIntArray} */
public class SparseFloatArray implements Cloneable {
  private static final int[] EMPTY_INT_ARRAY = new int[0];
  private static final float[] EMPTY_FLOAT_ARRAY = new float[0];

  private int[] mKeys;
  private float[] mValues;
  private int mSize;

  /** Creates a new SparseFloatArray containing no mappings. */
  public SparseFloatArray() {
    this(2);
  }

  /**
   * Creates a new SparseFloatArray containing no mappings that will not require any additional
   * memory allocation to store the specified number of mappings. If you supply an initial capacity
   * of 0, the sparse array will be initialized with a light-weight representation not requiring any
   * additional array allocations.
   */
  public SparseFloatArray(int initialCapacity) {
    if (initialCapacity == 0) {
      mKeys = EMPTY_INT_ARRAY;
      mValues = EMPTY_FLOAT_ARRAY;
    } else {
      mKeys = new int[initialCapacity];
      mValues = new float[mKeys.length];
    }
    mSize = 0;
  }

  @Override
  public @Nullable SparseFloatArray clone() {
    try {
      SparseFloatArray clone = (SparseFloatArray) super.clone();
      clone.mKeys = mKeys.clone();
      clone.mValues = mValues.clone();
      return clone;
    } catch (CloneNotSupportedException cnse) {
      // head in sand
      throw new RuntimeException(cnse);
    }
  }

  /**
   * Gets the float mapped from the specified key, or <code>0f</code> if no such mapping has been
   * made.
   */
  public float get(int key) {
    return get(key, 0);
  }

  /**
   * Gets the float mapped from the specified key, or the specified value if no such mapping has
   * been made.
   */
  public float get(int key, float valueIfKeyNotFound) {
    int i = binarySearch(mKeys, mSize, key);

    if (i < 0) {
      return valueIfKeyNotFound;
    } else {
      return mValues[i];
    }
  }

  /** Removes the mapping from the specified key, if there was any. */
  public void delete(int key) {
    int i = binarySearch(mKeys, mSize, key);

    if (i >= 0) {
      removeAt(i);
    }
  }

  /** Removes the mapping at the given index. */
  public void removeAt(int index) {
    System.arraycopy(mKeys, index + 1, mKeys, index, mSize - (index + 1));
    System.arraycopy(mValues, index + 1, mValues, index, mSize - (index + 1));
    mSize--;
  }

  /**
   * Adds a mapping from the specified key to the specified value, replacing the previous mapping
   * from the specified key if there was one.
   */
  public void put(int key, float value) {
    int i = binarySearch(mKeys, mSize, key);

    if (i >= 0) {
      mValues[i] = value;
    } else {
      i = ~i;

      mKeys = insert(mKeys, mSize, i, key);
      mValues = insert(mValues, mSize, i, value);
      mSize++;
    }
  }

  /** Returns the number of key-value mappings that this SparseFloatArray currently stores. */
  public int size() {
    return mSize;
  }

  /**
   * Given an index in the range <code>0...size()-1</code>, returns the key from the <code>index
   * </code>th key-value mapping that this SparseFloatArray stores.
   *
   * <p>The keys corresponding to indices in ascending order are guaranteed to be in ascending
   * order, e.g., <code>keyAt(0)</code> will return the smallest key and <code>keyAt(size()-1)
   * </code> will return the largest key.
   */
  public int keyAt(int index) {
    return mKeys[index];
  }

  /**
   * Given an index in the range <code>0...size()-1</code>, returns the value from the <code>index
   * </code>th key-value mapping that this SparseFloatArray stores.
   *
   * <p>The values corresponding to indices in ascending order are guaranteed to be associated with
   * keys in ascending order, e.g., <code>valueAt(0)</code> will return the value associated with
   * the smallest key and <code>valueAt(size()-1)</code> will return the value associated with the
   * largest key.
   */
  public float valueAt(int index) {
    return mValues[index];
  }

  /** Directly set the value at a particular index. */
  public void setValueAt(int index, float value) {
    mValues[index] = value;
  }

  /**
   * Returns the index for which {@link #keyAt} would return the specified key, or a negative number
   * if the specified key is not mapped.
   */
  public int indexOfKey(int key) {
    return binarySearch(mKeys, mSize, key);
  }

  /**
   * Returns an index for which {@link #valueAt} would return the specified key, or a negative
   * number if no keys map to the specified value. Beware that this is a linear search, unlike
   * lookups by key, and that multiple keys can map to the same value and this will find only one of
   * them.
   */
  public int indexOfValue(float value) {
    for (int i = 0; i < mSize; i++) if (mValues[i] == value) return i;

    return -1;
  }

  /** Removes all key-value mappings from this SparseFloatArray. */
  public void clear() {
    mSize = 0;
  }

  /**
   * Puts a key/value pair into the array, optimizing for the case where the key is greater than all
   * existing keys in the array.
   */
  public void append(int key, float value) {
    if (mSize != 0 && key <= mKeys[mSize - 1]) {
      put(key, value);
      return;
    }

    mKeys = append(mKeys, mSize, key);
    mValues = append(mValues, mSize, value);
    mSize++;
  }

  /** Provides a copy of keys. */
  public @Nullable int[] copyKeys() {
    if (size() == 0) {
      return null;
    }
    return Arrays.copyOf(mKeys, size());
  }

  /** This implementation composes a string by iterating over its mappings. */
  @Override
  public String toString() {
    if (size() <= 0) {
      return "{}";
    }

    StringBuilder buffer = new StringBuilder(mSize * 28);
    buffer.append('{');
    for (int i = 0; i < mSize; i++) {
      if (i > 0) {
        buffer.append(", ");
      }
      int key = keyAt(i);
      buffer.append(key);
      buffer.append('=');
      float value = valueAt(i);
      buffer.append(value);
    }
    buffer.append('}');
    return buffer.toString();
  }

  private static int binarySearch(int[] array, int size, int value) {
    int lo = 0;
    int hi = size - 1;
    while (lo <= hi) {
      final int mid = (lo + hi) >>> 1;
      final int midVal = array[mid];
      if (midVal < value) {
        lo = mid + 1;
      } else if (midVal > value) {
        hi = mid - 1;
      } else {
        return mid; // value found
      }
    }
    return ~lo; // value not present
  }

  /** Primitive int version of append. */
  private static int[] append(int[] array, int currentSize, int element) {
    if (currentSize + 1 > array.length) {
      int[] newArray = new int[growSize(currentSize)];
      System.arraycopy(array, 0, newArray, 0, currentSize);
      array = newArray;
    }
    array[currentSize] = element;
    return array;
  }

  /** Primitive int version of insert. */
  private static int[] insert(int[] array, int currentSize, int index, int element) {
    if (currentSize + 1 <= array.length) {
      System.arraycopy(array, index, array, index + 1, currentSize - index);
      array[index] = element;
      return array;
    }
    int[] newArray = new int[growSize(currentSize)];
    System.arraycopy(array, 0, newArray, 0, index);
    newArray[index] = element;
    System.arraycopy(array, index, newArray, index + 1, array.length - index);
    return newArray;
  }

  /** Primitive float version of for append. */
  private static float[] append(float[] array, int currentSize, float element) {
    if (currentSize + 1 > array.length) {
      float[] newArray = new float[growSize(currentSize)];
      System.arraycopy(array, 0, newArray, 0, currentSize);
      array = newArray;
    }
    array[currentSize] = element;
    return array;
  }

  /** Primitive float version of insert. */
  private static float[] insert(float[] array, int currentSize, int index, float element) {
    if (currentSize + 1 <= array.length) {
      System.arraycopy(array, index, array, index + 1, currentSize - index);
      array[index] = element;
      return array;
    }
    float[] newArray = new float[growSize(currentSize)];
    System.arraycopy(array, 0, newArray, 0, index);
    newArray[index] = element;
    System.arraycopy(array, index, newArray, index + 1, array.length - index);
    return newArray;
  }

  /**
   * Given the current size of an array, returns an ideal size to which the array should grow. This
   * is the double of {@param currentSize} but but should not be relied upon to do so in the future.
   *
   * @param currentSize The current size of the array.
   */
  private static int growSize(int currentSize) {
    return currentSize <= 2 ? 4 : currentSize * 2;
  }
}
