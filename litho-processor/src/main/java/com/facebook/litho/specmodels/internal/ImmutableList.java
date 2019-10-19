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

package com.facebook.litho.specmodels.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/** A sad, standard Java implementation of an immutable List. */
public final class ImmutableList<E> implements List<E> {

  private final List<E> mBackingList;

  private ImmutableList(List<E> list) {
    mBackingList = Collections.unmodifiableList(list);
  }

  public static <E> ImmutableList<E> copyOf(List<E> list) {
    return new ImmutableList<>(list);
  }

  public static <E> ImmutableList<E> copyOf(Collection<E> collection) {
    return new ImmutableList<>(new ArrayList<>(collection));
  }

  public static <E> ImmutableList<E> of(E... elements) {
    return new ImmutableList<>(Arrays.asList(elements));
  }

  @Override
  public int size() {
    return mBackingList.size();
  }

  @Override
  public boolean isEmpty() {
    return mBackingList.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return mBackingList.contains(o);
  }

  @Override
  public Iterator<E> iterator() {
    return mBackingList.iterator();
  }

  @Override
  public Object[] toArray() {
    return mBackingList.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return mBackingList.toArray(a);
  }

  @Deprecated
  @Override
  public boolean add(E e) {
    return mBackingList.add(e);
  }

  @Deprecated
  @Override
  public boolean remove(Object o) {
    return mBackingList.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return mBackingList.containsAll(c);
  }

  @Deprecated
  @Override
  public boolean addAll(Collection<? extends E> c) {
    return mBackingList.addAll(c);
  }

  @Deprecated
  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    return mBackingList.addAll(index, c);
  }

  @Deprecated
  @Override
  public boolean removeAll(Collection<?> c) {
    return mBackingList.removeAll(c);
  }

  @Deprecated
  @Override
  public boolean retainAll(Collection<?> c) {
    return mBackingList.retainAll(c);
  }

  @Deprecated
  @Override
  public void clear() {
    mBackingList.clear();
  }

  @Override
  public E get(int index) {
    return mBackingList.get(index);
  }

  @Deprecated
  @Override
  public E set(int index, E element) {
    return mBackingList.set(index, element);
  }

  @Deprecated
  @Override
  public void add(int index, E element) {
    mBackingList.add(index, element);
  }

  @Deprecated
  @Override
  public E remove(int index) {
    return mBackingList.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return mBackingList.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return mBackingList.lastIndexOf(o);
  }

  @Override
  public ListIterator<E> listIterator() {
    return mBackingList.listIterator();
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    return mBackingList.listIterator(index);
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    return mBackingList.subList(fromIndex, toIndex);
  }

  @Override
  public int hashCode() {
    return mBackingList.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return mBackingList.equals(obj);
  }

  @Override
  public String toString() {
    return mBackingList.toString();
  }
}
