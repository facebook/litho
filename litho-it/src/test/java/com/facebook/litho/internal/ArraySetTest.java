/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.internal;

import java.util.Iterator;

import com.facebook.litho.internal.ArraySet;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArraySetTest {

  @Test
  public void testArraySet() {
    ArraySet<String> set = new ArraySet<>();

    String testElt1 = "test1";
    String testElt2 = "test2";
    String testElt3 = "test3";
    String testElt4 = "test4";
    String testElt5 = null;

    assertTrue(set.isEmpty());
    assertEquals(0, set.size());

    set.add(testElt1);
    assertTrue(set.contains(testElt1));
    // Can't add more than once
    assertFalse(set.add(testElt1));
    assertEquals(1, set.size());
    set.remove(testElt1);
    assertFalse(set.contains(testElt1));
    assertEquals(0, set.size());
    assertTrue(set.isEmpty());
    assertTrue(checkIterator(set));

    assertTrue(set.add(testElt1));
    assertTrue(set.add(testElt2));
    assertTrue(set.add(testElt3));
    assertTrue(set.contains(testElt1));
    assertTrue(set.contains(testElt2));
    assertTrue(set.contains(testElt3));
    assertFalse(set.contains(testElt4));
    assertFalse(set.contains(testElt5));
    assertEquals(3, set.size());
    assertFalse(set.isEmpty());
    assertTrue(checkIterator(set));

    assertTrue(set.remove(testElt1));
    assertFalse(set.contains(testElt1));
    assertTrue(set.contains(testElt2));
    assertTrue(set.contains(testElt3));
    assertFalse(set.contains(testElt4));
    assertEquals(2, set.size());
    assertTrue(checkIterator(set));

    assertTrue(set.add(testElt4));
    assertFalse(set.contains(testElt1));
    assertTrue(set.contains(testElt2));
    assertTrue(set.contains(testElt3));
    assertTrue(set.contains(testElt4));
    assertEquals(3, set.size());
    assertTrue(checkIterator(set));

    assertTrue(set.add(testElt5));
    assertTrue(set.contains(testElt5));
    assertEquals(4, set.size());
    assertTrue(checkIterator(set));
    assertTrue(set.remove(testElt5));

    assertFalse(set.remove(testElt1));
    assertFalse(set.contains(testElt1));
    assertTrue(set.contains(testElt2));
    assertTrue(set.contains(testElt3));
    assertTrue(set.contains(testElt4));
    assertEquals(3, set.size());
    assertTrue(checkIterator(set));

    assertTrue(set.remove(testElt4));
    assertFalse(set.contains(testElt1));
    assertTrue(set.contains(testElt2));
    assertTrue(set.contains(testElt3));
    assertFalse(set.contains(testElt4));
    assertEquals(2, set.size());
    assertTrue(checkIterator(set));

    assertTrue(set.remove(testElt2));
    assertFalse(set.contains(testElt1));
    assertFalse(set.contains(testElt2));
    assertTrue(set.contains(testElt3));
    assertFalse(set.contains(testElt4));
    assertEquals(1, set.size());
    assertTrue(checkIterator(set));

    assertTrue(set.remove(testElt3));
    assertFalse(set.contains(testElt1));
    assertFalse(set.contains(testElt2));
    assertFalse(set.contains(testElt3));
    assertFalse(set.contains(testElt4));
    assertEquals(0, set.size());
    assertTrue(set.isEmpty());
    assertTrue(checkIterator(set));

    assertTrue(set.add(testElt1));
    assertTrue(set.add(testElt2));
    assertTrue(set.add(testElt3));
    assertTrue(checkIterator(set));
    set.clear();
    assertFalse(set.contains(testElt1));
    assertFalse(set.contains(testElt2));
    assertFalse(set.contains(testElt3));
    assertEquals(0, set.size());
    assertTrue(set.isEmpty());
    assertTrue(checkIterator(set));

    set.add(testElt1);
    set.add(testElt2);
    set.add(testElt3);
    Iterator<String> it = set.iterator();
    assertTrue(it.hasNext());
    while (it.hasNext()) {
      String value = it.next();
      assertNotNull(value);
      if (value == testElt2) {
        it.remove();
      }
    }
    assertTrue(set.contains(testElt1));
    assertFalse(set.contains(testElt2));
    assertTrue(set.contains(testElt3));
  }

  private <T> boolean checkIterator(ArraySet<T> set) {
    int index = 0;
    for (T value : set) {
      if (value != set.valueAt(index)) {
        return false;
      }
      if (set.indexOf(value) != index) {
        return false;
      }
      ++index;
    }
    return true;
  }
}
