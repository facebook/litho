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

package com.facebook.litho.internal;

import androidx.collection.SimpleArrayMap;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A simple Set implementation backed by an array. Currently implemented as a wrapper around
 * ArrayMap because Google's ArraySet is API23+ and not yet available from a support library.
 * Google's ArraySet should be a drop-in replacement for this class, and will be a little more
 * efficient and complete.
 *
 * @deprecated This collection uses SimpleArrayMap as backing array which static cache can be
 *     corrupted when used in multi-threaded environment. When static cache gets corrupted, next
 *     time instance is used it can crash with ClassCastException even if that instance is thread
 *     confined.
 */
@Deprecated
public class ArraySet<E> implements Set<E> {

  // This could be any value other than null.
  private static final Integer SENTINEL_MAP_VALUE = Integer.valueOf(0);

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  private final SimpleArrayMap<E, Integer> mMap;

  public ArraySet() {
    mMap = new SimpleArrayMap<>();
  }

  public ArraySet(int capacity) {
    mMap = new SimpleArrayMap<>(capacity);
  }

  public ArraySet(@Nullable Collection<? extends E> set) {
    mMap = new SimpleArrayMap<>();
    if (set != null) {
      addAll(set);
    }
  }

  @Override
  public boolean add(E value) {
    Object oldValue = mMap.put(value, SENTINEL_MAP_VALUE);
    return oldValue == null;
  }

  @Override
  public boolean addAll(Collection<? extends E> collection) {
    ensureCapacity(size() + collection.size());
    boolean added = false;
    if (collection instanceof ArraySet) {
      // Use a code path in SimpleArrayMap which avoids an iterator allocation. It is also optimized
      // to run in O(N) time when this.size() == 0.
      ArraySet<? extends E> arraySet = (ArraySet) collection;
      int oldSize = size();
      mMap.putAll(arraySet.mMap);
      added = size() != oldSize;
    } else if (collection instanceof List && collection instanceof RandomAccess) {
      List<? extends E> list = (List) collection;
      for (int i = 0, size = list.size(); i < size; ++i) {
        added |= add(list.get(i));
      }
    } else {
      for (E value : collection) {
        added |= add(value);
      }
    }
    return added;
  }

  @Override
  public void clear() {
    mMap.clear();
  }

  /**
   * Equivalent to calling {@link #clear()} followed by {@link #addAll(Collection)}, but this should
   * make it more apparent that this is an optimized code path. Instead of needing lots of O(log2 N)
   * operations, this special case is optimized to perform in O(N) time, where N is the size of the
   * incoming collection.
   */
  public void clearAndAddAll(ArraySet<? extends E> collection) {
    clear();
    addAll(collection);
  }

  @Override
  public boolean contains(Object value) {
    return mMap.containsKey(value);
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    if (collection instanceof List && collection instanceof RandomAccess) {
      List<?> list = (List) collection;
      for (int i = 0, size = list.size(); i < size; ++i) {
        if (!contains(list.get(i))) {
          return false;
        }
      }
    } else {
      Iterator<?> it = collection.iterator();
      while (it.hasNext()) {
        if (!contains(it.next())) {
          return false;
        }
      }
    }
    return true;
  }

  public void ensureCapacity(int minimumCapacity) {
    mMap.ensureCapacity(minimumCapacity);
  }

  @Override
  public boolean equals(Object object) {
    // This implementation is borrowed from the real ArraySet<T>

    if (object == this) {
      return true;
    }

    if (object instanceof Set) {
      Set<?> set = (Set<?>) object;
      if (size() != set.size()) {
        return false;
      }

      try {
        for (int i = 0, size = size(); i < size; ++i) {
          E mine = valueAt(i);
          if (!set.contains(mine)) {
            return false;
          }
        }
      } catch (NullPointerException ignored) {
        return false;
      } catch (ClassCastException ignored) {
        return false;
      }
      return true;
    }

    return false;
  }

  @Override
  public int hashCode() {
    // This algorithm is borrowed from the real ArraySet<T>

    int result = 0;
    for (int i = 0, size = size(); i < size; ++i) {
      E value = valueAt(i);
      if (value != null) {
        result += value.hashCode();
      }
    }
    return result;
  }

  public int indexOf(E value) {
    return mMap.indexOfKey(value);
  }

  @Override
  public boolean isEmpty() {
    return mMap.isEmpty();
  }

  @Override
  public Iterator<E> iterator() {
    return new ArraySetIterator();
  }

  @Override
  public boolean remove(Object value) {
    int index = indexOf((E) value);
    if (index >= 0) {
      removeAt(index);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    boolean removed = false;
    if (collection instanceof List && collection instanceof RandomAccess) {
      // If possible, avoid the iterator allocation inherent in for-each
      final List<?> list = (List) collection;
      for (int i = 0, size = list.size(); i < size; ++i) {
        removed |= remove(list.get(i));
      }
    } else {
      for (Object value : collection) {
        removed |= remove(value);
      }
    }
    return removed;
  }

  public E removeAt(int index) {
    E value = mMap.keyAt(index);
    mMap.removeAt(index);
    return value;
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    boolean removed = false;
    for (int i = size() - 1; i >= 0; --i) {
      if (!collection.contains(valueAt(i))) {
        removeAt(i);
        removed = true;
      }
    }
    return removed;
  }

  @Override
  public int size() {
    return mMap.size();
  }

  @Override
  public Object[] toArray() {
    int size = mMap.size();
    if (size == 0) {
      // This ensures that ImmutableSet.copyOf() can be used without an extra Object[0] allocation
      return EMPTY_OBJECT_ARRAY;
    }
    Object[] array = new Object[size];
    for (int i = 0; i < size; ++i) {
      array[i] = mMap.keyAt(i);
    }
    return array;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] array) {
    int size = size();
    if (array.length < size) {
      T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), size);
      array = newArray;
    }
    for (int i = 0; i < size; ++i) {
      array[i] = (T) valueAt(i);
    }
    if (array.length > size) {
      array[size] = null;
    }
    return array;
  }

  @Override
  public String toString() {
    // This implementation is borrowed from the real ArraySet<T>

    if (isEmpty()) {
      return "{}";
    }

    int size = size();
    StringBuilder buffer = new StringBuilder(size * 14);
    buffer.append('{');
    for (int i = 0; i < size; ++i) {
      if (i > 0) {
        buffer.append(", ");
      }
      Object value = valueAt(i);
      if (value != this) {
        buffer.append(value);
      } else {
        buffer.append("(this Set)");
      }
    }
    buffer.append('}');
    return buffer.toString();
  }

  public E valueAt(int index) {
    return mMap.keyAt(index);
  }

  private final class ArraySetIterator implements Iterator<E> {

    private int mIndex;
    private boolean mRemoved;

    public ArraySetIterator() {
      mIndex = -1;
    }

    @Override
    public boolean hasNext() {
      return (mIndex + 1) < size();
    }

    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      mRemoved = false;
      ++mIndex;
      return valueAt(mIndex);
    }

    @Override
    public void remove() {
      if (mRemoved) {
        throw new IllegalStateException();
      }
      removeAt(mIndex);
      mRemoved = true;
      --mIndex;
    }
  }
}
