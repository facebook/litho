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

import static org.junit.Assert.assertEquals;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
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
