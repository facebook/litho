/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class RecyclePoolTest {

  @Test
  public void testClear() {
    final RecyclePool<Object> pool = new RecyclePool<>("test", 10, false);
    final int ELEMENT_NUM = 7;

    for (int i = 0; i < ELEMENT_NUM; i++) {
      pool.release(new Object());
    }

    assertEquals(ELEMENT_NUM, pool.getCurrentSize());

    pool.clear();

    assertEquals(0, pool.getCurrentSize());
  }
}
