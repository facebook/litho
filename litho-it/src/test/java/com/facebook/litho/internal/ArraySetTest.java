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

import static org.assertj.core.api.Java6Assertions.assertThat;
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

    assertThat(set).isEmpty();
    assertThat(set).isEmpty();

    set.add(testElt1);
    assertThat(set).contains(testElt1);
    // Can't add more than once
    assertThat(set.add(testElt1)).isFalse();
    assertThat(set).hasSize(1);
    set.remove(testElt1);
    assertThat(set)
        .doesNotContain(testElt1)
        .isEmpty();
    assertThat(checkIterator(set)).isTrue();

    assertThat(set.add(testElt1)).isTrue();
    assertThat(set.add(testElt2)).isTrue();
    assertThat(set.add(testElt3)).isTrue();
    assertThat(set)
        .contains(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .doesNotContain(testElt4)
        .doesNotContain(testElt5)
        .hasSize(3)
        .isNotEmpty();
    assertThat(checkIterator(set)).isTrue();

    assertThat(set.remove(testElt1)).isTrue();
    assertThat(set)
        .doesNotContain(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .doesNotContain(testElt4)
        .hasSize(2);
    assertThat(checkIterator(set)).isTrue();

    assertThat(set.add(testElt4)).isTrue();
    assertThat(set)
        .doesNotContain(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .contains(testElt4)
        .hasSize(3);
    assertThat(checkIterator(set)).isTrue();

    assertThat(set.add(testElt5)).isTrue();
    assertThat(set)
        .contains(testElt5)
        .hasSize(4);
    assertThat(checkIterator(set)).isTrue();
    assertThat(set.remove(testElt5)).isTrue();

    assertThat(set.remove(testElt1)).isFalse();
    assertThat(set)
        .doesNotContain(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .contains(testElt4)
        .hasSize(3);
    assertThat(checkIterator(set)).isTrue();

    assertThat(set.remove(testElt4)).isTrue();
    assertThat(set)
        .doesNotContain(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .doesNotContain(testElt4)
        .hasSize(2);
    assertThat(checkIterator(set)).isTrue();

    assertThat(set.remove(testElt2)).isTrue();
    assertThat(set).doesNotContain(testElt1)
        .doesNotContain(testElt2)
        .contains(testElt3)
        .doesNotContain(testElt4)
        .hasSize(1);
    assertThat(checkIterator(set)).isTrue();

    assertThat(set.remove(testElt3)).isTrue();
    assertThat(set)
        .doesNotContain(testElt1)
        .doesNotContain(testElt2)
        .doesNotContain(testElt3)
        .doesNotContain(testElt4)
        .isEmpty();
    assertThat(checkIterator(set)).isTrue();

    assertThat(set.add(testElt1)).isTrue();
    assertThat(set.add(testElt2)).isTrue();
    assertThat(set.add(testElt3)).isTrue();
    assertThat(checkIterator(set)).isTrue();
    set.clear();
    assertThat(set).doesNotContain(testElt1);
    assertThat(set).doesNotContain(testElt2);
    assertThat(set).doesNotContain(testElt3);
    assertThat(set).isEmpty();
    assertThat(set).isEmpty();
    assertThat(checkIterator(set)).isTrue();

    set.add(testElt1);
    set.add(testElt2);
    set.add(testElt3);
    Iterator<String> it = set.iterator();
    assertThat(it.hasNext()).isTrue();
    while (it.hasNext()) {
      String value = it.next();
      assertThat(value).isNotNull();
      if (value == testElt2) {
        it.remove();
      }
    }
    assertThat(set).contains(testElt1)
        .doesNotContain(testElt2)
        .contains(testElt3);
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
